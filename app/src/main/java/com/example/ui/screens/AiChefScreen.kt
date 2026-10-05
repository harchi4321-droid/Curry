package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeneratedRecipe
import com.example.data.RecipeEntity
import com.example.ui.AiGenerationUiState
import com.example.ui.RecipeViewModel
import com.example.ui.components.LanguageSwitchButton
import com.example.util.AppLanguage
import com.example.util.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChefScreen(
    viewModel: RecipeViewModel,
    onBack: () -> Unit,
    onRecipeSaved: (Long) -> Unit
) {
    val strings by viewModel.strings.collectAsState()
    val currentLanguage by viewModel.appLanguage.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Recipe Creator, 1: Ask Chef Chat

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(strings.aiChefTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(strings.aiChefSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    LanguageSwitchButton(
                        currentLanguage = currentLanguage,
                        onToggle = { viewModel.toggleLanguage() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.aiTabCreate, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.aiTabChat, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                AiRecipeGeneratorTab(
                    viewModel = viewModel,
                    strings = strings,
                    currentLanguage = currentLanguage,
                    onRecipeSaved = onRecipeSaved
                )
            } else {
                AiChefChatTab(
                    viewModel = viewModel,
                    strings = strings,
                    currentLanguage = currentLanguage
                )
            }
        }
    }
}

@Composable
fun AiRecipeGeneratorTab(
    viewModel: RecipeViewModel,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    onRecipeSaved: (Long) -> Unit
) {
    var ingredientsInput by remember { mutableStateOf("") }
    val aiState by viewModel.aiGenState.collectAsState()

    val quickPantryItems = remember(currentLanguage) {
        if (currentLanguage == AppLanguage.ENGLISH) {
            listOf("Chicken", "Pork", "Fish", "Shrimp", "Egg", "Potato", "Tomato", "Green Beans", "Mushroom", "Onion", "Garlic")
        } else {
            listOf("ကြက်သား", "ဝက်သား", "ငါး", "ပုစွန်", "ကြက်ဥ", "အာလူး", "ခရမ်းချဉ်သီး", "ပဲသီး", "မျှစ်", "မှို", "ကြက်သွန်နီ")
        }
    }
    val selectedItems = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = strings.pantryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.pantrySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPantryItems.forEach { item ->
                        val isSelected = selectedItems.contains(item)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    selectedItems.remove(item)
                                } else {
                                    selectedItems.add(item)
                                }
                            },
                            label = { Text(item) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ingredientsInput,
                    onValueChange = { ingredientsInput = it },
                    label = { Text(strings.otherIngredientsLabel) },
                    placeholder = {
                        Text(if (currentLanguage == AppLanguage.ENGLISH) "e.g. Garlic, ginger, soy sauce, lime..." else "ဥပမာ - ကြက်သွန်ဖြူ၊ ဂျင်း၊ ငရုတ်သီး၊ ငံပြာရည်...")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val combined = buildString {
                            if (selectedItems.isNotEmpty()) {
                                val prefix = if (currentLanguage == AppLanguage.ENGLISH) "Ingredients I have: " else "ငါ့မှာရှိတဲ့ ပါဝင်ပစ္စည်းများ: "
                                append(prefix)
                                append(selectedItems.joinToString(", "))
                            }
                            if (ingredientsInput.isNotBlank()) {
                                if (isNotEmpty()) append(". ")
                                append(ingredientsInput)
                            }
                        }
                        if (combined.isNotBlank()) {
                            viewModel.generateRecipeFromAi(combined)
                        }
                    },
                    enabled = (selectedItems.isNotEmpty() || ingredientsInput.isNotBlank()) && aiState !is AiGenerationUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_ai_generate_recipe")
                ) {
                    if (aiState is AiGenerationUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (currentLanguage == AppLanguage.ENGLISH) "Generating Recipe..." else "ချက်နည်း စဉ်းစားပေးနေပါသည်...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.btnGenerateRecipe)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Generated Result State
        when (val state = aiState) {
            is AiGenerationUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "AI Chef is crafting the perfect recipe..." else "AI စားဖိုမှူးက အသင့်တော်ဆုံး ချက်နည်းကို ရေးစပ်နေပါသည်...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            is AiGenerationUiState.Error -> {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                if (currentLanguage == AppLanguage.ENGLISH) "Generation Failed" else "ဖန်တီးမှု မအောင်မြင်ပါ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
            is AiGenerationUiState.Success -> {
                GeneratedRecipeCard(
                    recipe = state.recipe,
                    strings = strings,
                    currentLanguage = currentLanguage,
                    onSaveToCookNote = {
                        val entity = RecipeEntity(
                            id = 0L,
                            title = state.recipe.title,
                            category = state.recipe.category,
                            description = state.recipe.description,
                            prepTimeMinutes = state.recipe.prepTimeMinutes,
                            cookTimeMinutes = state.recipe.cookTimeMinutes,
                            servings = state.recipe.servings,
                            notes = state.recipe.notes,
                            ingredientsJson = RecipeEntity.ingredientsToJson(state.recipe.ingredients),
                            stepsJson = RecipeEntity.stepsToJson(state.recipe.steps)
                        )
                        viewModel.saveRecipe(entity) { newId ->
                            viewModel.resetAiGenState()
                            onRecipeSaved(newId)
                        }
                    }
                )
            }
            AiGenerationUiState.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.ENGLISH) "💡 Select pantry items and tap \"Generate Recipe\"" else "💡 ပါဝင်ပစ္စည်းများကို ရွေးချယ်ပြီး \"ဟင်းချက်နည်း ဖန်တီးပေးပါ\" ကို နှိပ်ပါ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun GeneratedRecipeCard(
    recipe: GeneratedRecipe,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    onSaveToCookNote: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_generated_recipe_result")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(recipe.category) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                val totalTime = recipe.prepTimeMinutes + recipe.cookTimeMinutes
                Text(
                    text = "⏱ $totalTime ${strings.minsUnit} | 👥 ${recipe.servings} ${strings.servingsUnit}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = recipe.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (recipe.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            // Ingredients
            Text(
                text = "${strings.ingredientsTitle} (${recipe.ingredients.size}):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            recipe.ingredients.forEach { ing ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("• ${ing.name}", style = MaterialTheme.typography.bodyMedium)
                    if (ing.amount.isNotBlank()) {
                        Text("${ing.amount} ${ing.unit}".trim(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

            // Steps
            Text(
                text = "${strings.stepsTitle}:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            recipe.steps.forEach { step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${step.stepNumber}. ",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = step.instruction + if (step.timerMinutes > 0) " (⏱ ${step.timerMinutes} ${strings.minsUnit})" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }

            if (recipe.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            if (currentLanguage == AppLanguage.ENGLISH) "💡 Chef Secrets & Tips:" else "💡 စားဖိုမှူး လျှို့ဝှက်ချက်:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(recipe.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSaveToCookNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_ai_recipe_to_notebook")
            ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.btnSaveAiRecipe, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AiChefChatTab(
    viewModel: RecipeViewModel,
    strings: AppStrings,
    currentLanguage: AppLanguage
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestedQuestions = remember(currentLanguage) {
        if (currentLanguage == AppLanguage.ENGLISH) {
            listOf(
                "How to make curry aromatic?",
                "What can substitute for fresh chili?",
                "How to cook pork belly without greasy feel?"
            )
        } else {
            listOf(
                "ကြက်သားဟင်း အနံ့မွှေးအောင် ဘာထည့်ရမလဲ?",
                "ငရုတ်သီးစိမ်းအစား ဘာသုံးလို့ရမလဲ?",
                "ဝက်သားဆီပြန် မအီအောင် ဘယ်လိုချက်ရမလဲ?"
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Bottom)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = msg.message,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 40.dp, top = 4.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (currentLanguage == AppLanguage.ENGLISH) "AI Chef is replying..." else "AI စားဖိုမှူး ပြန်လည်ဖြေကြားနေပါသည်...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Quick suggestions chip row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestedQuestions.forEach { suggestion ->
                AssistChip(
                    onClick = {
                        viewModel.sendChatMessage(suggestion)
                    },
                    label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Input Bar
        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(strings.askChefPlaceholder) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text"),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendChatMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isLoading,
                    modifier = Modifier.testTag("chat_send_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
