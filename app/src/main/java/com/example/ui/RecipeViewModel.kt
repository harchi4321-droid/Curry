package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiChefService
import com.example.ai.GeneratedRecipe
import com.example.data.AppDatabase
import com.example.data.RecipeEntity
import com.example.data.RecipeRepository
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.Localization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface AiGenerationUiState {
    object Idle : AiGenerationUiState
    object Loading : AiGenerationUiState
    data class Success(val recipe: GeneratedRecipe) : AiGenerationUiState
    data class Error(val message: String) : AiGenerationUiState
}

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecipeRepository

    // Language setting persisted in Room Database
    private val _appLanguage = MutableStateFlow(AppLanguage.MYANMAR)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    val strings: StateFlow<AppStrings> = _appLanguage
        .map { Localization.get(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Localization.Myanmar
        )

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("အားလုံး")

    private val _aiGenState = MutableStateFlow<AiGenerationUiState>(AiGenerationUiState.Idle)
    val aiGenState: StateFlow<AiGenerationUiState> = _aiGenState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = RecipeRepository(db.recipeDao(), db.settingsDao())

        // Collect language from Room DB
        viewModelScope.launch {
            repository.getLanguageSetting().collect { savedCode ->
                val lang = when (savedCode) {
                    "en" -> AppLanguage.ENGLISH
                    else -> AppLanguage.MYANMAR
                }
                _appLanguage.value = lang
                if (_chatMessages.value.isEmpty()) {
                    _chatMessages.value = listOf(
                        ChatMessage(
                            sender = "ai",
                            message = Localization.get(lang).defaultChefGreeting
                        )
                    )
                }
            }
        }
    }

    fun toggleLanguage() {
        val next = if (_appLanguage.value == AppLanguage.MYANMAR) AppLanguage.ENGLISH else AppLanguage.MYANMAR
        setLanguage(next)
    }

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
        viewModelScope.launch {
            repository.setLanguageSetting(language.code)
        }
        // If query/category is "အားလုံး" or "All", update
        if (selectedCategory.value == "အားလုံး" || selectedCategory.value == "All") {
            selectedCategory.value = if (language == AppLanguage.ENGLISH) "All" else "အားလုံး"
        }
    }

    val allRecipes: StateFlow<List<RecipeEntity>> = repository.allRecipes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteRecipes: StateFlow<List<RecipeEntity>> = repository.favoriteRecipes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Category recipe counts
    val categoryCounts: StateFlow<Map<String, Int>> = allRecipes
        .map { list ->
            val counts = mutableMapOf<String, Int>()
            list.forEach { recipe ->
                val cat = recipe.category.trim()
                counts[cat] = (counts[cat] ?: 0) + 1
            }
            counts
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val filteredRecipes: StateFlow<List<RecipeEntity>> = combine(
        allRecipes,
        searchQuery,
        selectedCategory
    ) { list, query, category ->
        list.filter { recipe ->
            val matchesQuery = query.isBlank() ||
                    recipe.title.contains(query, ignoreCase = true) ||
                    recipe.category.contains(query, ignoreCase = true) ||
                    recipe.ingredientsJson.contains(query, ignoreCase = true) ||
                    recipe.description.contains(query, ignoreCase = true)

            val isAll = category == "အားလုံး" || category == "All"
            val matchesCategory = isAll || recipe.category.equals(category, ignoreCase = true) || recipe.category.contains(category, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getRecipeFlow(id: Long) = repository.getRecipeById(id)

    fun saveRecipe(recipe: RecipeEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            if (recipe.id == 0L) {
                val newId = repository.insertRecipe(recipe)
                onSaved(newId)
            } else {
                repository.updateRecipe(recipe)
                onSaved(recipe.id)
            }
        }
    }

    fun deleteRecipe(recipe: RecipeEntity, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteRecipe(recipe)
            onDeleted()
        }
    }

    fun clearAllData(onCleared: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteAllRecipes()
            onCleared()
        }
    }

    fun toggleFavorite(recipe: RecipeEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(recipe.id, !recipe.isFavorite)
        }
    }

    fun generateRecipeFromAi(userPrompt: String) {
        if (userPrompt.isBlank()) return
        val isEng = _appLanguage.value == AppLanguage.ENGLISH
        viewModelScope.launch {
            _aiGenState.value = AiGenerationUiState.Loading
            val result = GeminiChefService.generateRecipe(userPrompt, isEng)
            result.fold(
                onSuccess = { generated ->
                    _aiGenState.value = AiGenerationUiState.Success(generated)
                },
                onFailure = { err ->
                    _aiGenState.value = AiGenerationUiState.Error(
                        err.localizedMessage ?: if (isEng) "Error generating recipe with AI" else "AI Recipe ဖန်တီးရာတွင် အမှားတစ်ခုရှိပါသည်"
                    )
                }
            )
        }
    }

    fun resetAiGenState() {
        _aiGenState.value = AiGenerationUiState.Idle
    }

    fun sendChatMessage(question: String, currentRecipe: RecipeEntity? = null) {
        if (question.isBlank() || _isChatLoading.value) return
        val isEng = _appLanguage.value == AppLanguage.ENGLISH
        val userMsg = ChatMessage(sender = "user", message = question)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        val contextStr = currentRecipe?.let {
            "Recipe: ${it.title}\nCategory: ${it.category}\nIngredients: ${it.ingredientsJson}\nSteps: ${it.stepsJson}"
        }

        viewModelScope.launch {
            val result = GeminiChefService.askChef(question, contextStr, isEng)
            _isChatLoading.value = false
            result.fold(
                onSuccess = { reply ->
                    _chatMessages.value = _chatMessages.value + ChatMessage(sender = "ai", message = reply)
                },
                onFailure = { err ->
                    val errorReply = if (isEng) {
                        "Sorry, could not retrieve response: ${err.localizedMessage ?: "Network error"}"
                    } else {
                        "တောင်းပန်ပါတယ်ခင်ဗျာ။ အဖြေပြန်လည်ရယူရာတွင် အခက်အခဲရှိနေပါသည်: ${err.localizedMessage ?: "Network error"}"
                    }
                    _chatMessages.value = _chatMessages.value + ChatMessage(
                        sender = "ai",
                        message = errorReply
                    )
                }
            )
        }
    }
}
