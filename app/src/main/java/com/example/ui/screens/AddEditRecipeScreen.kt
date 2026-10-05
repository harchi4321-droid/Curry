package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.GeminiChefService
import com.example.data.CookingStep
import com.example.data.IngredientItem
import com.example.data.RecipeEntity
import com.example.ui.RecipeViewModel
import com.example.ui.components.LanguageSwitchButton
import com.example.ui.components.VideoPlayerDialog
import com.example.util.AppLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecipeScreen(
    recipeId: Long?,
    viewModel: RecipeViewModel,
    initialCategory: String? = null,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoadingExisting by remember { mutableStateOf(recipeId != null && recipeId > 0) }
    val strings by viewModel.strings.collectAsState()
    val currentLanguage by viewModel.appLanguage.collectAsState()

    // Form states
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(initialCategory ?: if (currentLanguage == AppLanguage.ENGLISH) "Curry / Main" else "ဟင်းလျာ") }
    var description by remember { mutableStateOf("") }
    var prepTimeMinutes by remember { mutableIntStateOf(15) }
    var cookTimeMinutes by remember { mutableIntStateOf(30) }
    var servings by remember { mutableIntStateOf(4) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var videoUrl by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val ingredients = remember { mutableStateListOf<IngredientItem>() }
    val steps = remember { mutableStateListOf<CookingStep>() }

    var isAiGenerating by remember { mutableStateOf(false) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }
    var showAiPromptDialog by remember { mutableStateOf(false) }
    var showVideoPreview by remember { mutableStateOf(false) }

    // Load existing recipe if editing
    LaunchedEffect(recipeId) {
        if (recipeId != null && recipeId > 0) {
            val db = com.example.data.AppDatabase.getDatabase(viewModel.getApplication())
            val existing = db.recipeDao().getRecipeByIdOnce(recipeId)
            if (existing != null) {
                title = existing.title
                category = existing.category
                description = existing.description
                prepTimeMinutes = existing.prepTimeMinutes
                cookTimeMinutes = existing.cookTimeMinutes
                servings = existing.servings
                imageUrl = existing.imageUrl
                videoUrl = existing.videoUrl ?: ""
                notes = existing.notes ?: ""

                ingredients.clear()
                ingredients.addAll(existing.parseIngredients())

                steps.clear()
                steps.addAll(existing.parseSteps())
            }
            isLoadingExisting = false
        } else {
            if (ingredients.isEmpty()) {
                ingredients.add(IngredientItem(name = "", amount = "", unit = ""))
            }
            if (steps.isEmpty()) {
                steps.add(CookingStep(stepNumber = 1, instruction = "", timerMinutes = 0))
            }
        }
    }

    // Photo picker (Zero permissions)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUrl = uri.toString()
        }
    }

    val predefinedCategories = remember(currentLanguage) {
        if (currentLanguage == AppLanguage.ENGLISH) {
            listOf("Curry / Main", "Soup", "Salad", "Fried", "Dessert", "Breakfast", "Beverage", "Snacks")
        } else {
            listOf("ဟင်းလျာ", "ဟင်းချို", "အသုပ်", "အကြော်", "အချိုပွဲ", "မနက်စာ", "အဖျော်ယမကာ", "အဆာပြေ")
        }
    }

    if (showVideoPreview && videoUrl.isNotBlank()) {
        VideoPlayerDialog(
            videoUrl = videoUrl,
            recipeTitle = if (title.isNotBlank()) title else "Video Preview",
            onDismiss = { showVideoPreview = false }
        )
    }

    // AI Auto-Draft Dialog
    if (showAiPromptDialog) {
        var aiDishPrompt by remember { mutableStateOf(title) }
        val draftTitle = if (currentLanguage == AppLanguage.ENGLISH) "Draft Recipe with AI" else "AI ဖြင့် ချက်နည်းအပြည့်အစုံ ရေးသားခိုင်းမည်"
        val draftDesc = if (currentLanguage == AppLanguage.ENGLISH) {
            "Enter dish name or available ingredients. AI Chef will populate ingredients and cooking steps automatically."
        } else {
            "ဟင်းအမည် သို့မဟုတ် သင့်လက်ထဲရှိ ပါဝင်ပစ္စည်းများကို ရေးပေးပါ။ AI မှ ပါဝင်ပစ္စည်းများနှင့် အဆင့်ဆင့်ချက်ပြုတ်ပုံကို အလိုအလျောက် ဖြည့်စွက်ပေးပါမည်။"
        }
        val draftPlaceholder = if (currentLanguage == AppLanguage.ENGLISH) "e.g. Chicken Biryani, Beef Stew..." else "ဥပမာ - ကြက်သားဒန်ပေါက်၊ အမဲသားဟင်းလျာ..."
        val draftBtn = if (currentLanguage == AppLanguage.ENGLISH) "Generate Draft" else "ရေးပေးပါ"

        AlertDialog(
            onDismissRequest = { showAiPromptDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(draftTitle)
                }
            },
            text = {
                Column {
                    Text(
                        text = draftDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = aiDishPrompt,
                        onValueChange = { aiDishPrompt = it },
                        label = { Text(draftPlaceholder) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val promptToUse = aiDishPrompt.ifBlank { title }
                        if (promptToUse.isNotBlank()) {
                            showAiPromptDialog = false
                            isAiGenerating = true
                            aiErrorMessage = null
                            val isEng = currentLanguage == AppLanguage.ENGLISH
                            coroutineScope.launch {
                                val result = GeminiChefService.generateRecipe(promptToUse, isEng)
                                isAiGenerating = false
                                result.fold(
                                    onSuccess = { generated ->
                                        title = generated.title
                                        category = generated.category
                                        description = generated.description
                                        prepTimeMinutes = generated.prepTimeMinutes
                                        cookTimeMinutes = generated.cookTimeMinutes
                                        servings = generated.servings
                                        notes = generated.notes

                                        ingredients.clear()
                                        ingredients.addAll(generated.ingredients)

                                        steps.clear()
                                        steps.addAll(generated.steps)
                                    },
                                    onFailure = { err ->
                                        aiErrorMessage = err.localizedMessage ?: "AI error"
                                    }
                                )
                            }
                        }
                    }
                ) {
                    Text(draftBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiPromptDialog = false }) {
                    Text(strings.btnCancel)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (recipeId != null && recipeId > 0) strings.editRecipeTitle else strings.newRecipeTitle,
                        fontWeight = FontWeight.Bold
                    )
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

                    Spacer(modifier = Modifier.width(4.dp))

                    // AI Quick assist button
                    IconButton(
                        onClick = { showAiPromptDialog = true },
                        modifier = Modifier.testTag("btn_ai_auto_draft")
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI Draft",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Save Button
                    FilledTonalButton(
                        onClick = {
                            if (title.isBlank()) return@FilledTonalButton
                            val recipeToSave = RecipeEntity(
                                id = recipeId ?: 0L,
                                title = title.trim(),
                                category = category.trim(),
                                description = description.trim(),
                                prepTimeMinutes = prepTimeMinutes,
                                cookTimeMinutes = cookTimeMinutes,
                                servings = servings,
                                imageUrl = imageUrl,
                                videoUrl = videoUrl.trim().ifBlank { null },
                                notes = notes.trim().ifBlank { null },
                                ingredientsJson = RecipeEntity.ingredientsToJson(ingredients.filter { it.name.isNotBlank() }),
                                stepsJson = RecipeEntity.stepsToJson(steps.filter { it.instruction.isNotBlank() })
                            )
                            viewModel.saveRecipe(recipeToSave) { savedId ->
                                onSaved(savedId)
                            }
                        },
                        enabled = title.isNotBlank() && !isAiGenerating,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_save_recipe")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (currentLanguage == AppLanguage.ENGLISH) "Save" else "သိမ်းမည်")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoadingExisting) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // AI Generating Indicator / Error
            if (isAiGenerating) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "AI Chef is drafting your recipe..." else "AI စားဖိုမှူးက ဟင်းချက်နည်း အသေးစိတ် ရေးသားပေးနေပါသည်...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (aiErrorMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = aiErrorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Quick AI assistance banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.aiDraftHint,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = { showAiPromptDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(strings.btnUseAi)
                    }
                }
            }

            // Section 1: Basic Info
            Text(
                text = if (currentLanguage == AppLanguage.ENGLISH) "Basic Information" else "အခြေခံ အချက်အလက်များ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(strings.inputTitleLabel) },
                placeholder = { Text(strings.inputTitlePlaceholder) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_recipe_title"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Selection Chips
            Text(strings.categoryLabel, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                predefinedCategories.forEach { cat ->
                    val isSelected = category == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(strings.categoryInputLabel) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(strings.inputDescLabel) },
                placeholder = { Text(strings.inputDescPlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Time & Servings Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = prepTimeMinutes.toString(),
                    onValueChange = { prepTimeMinutes = it.toIntOrNull() ?: 0 },
                    label = { Text("${strings.prepTimeLabel} (${strings.minsUnit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = cookTimeMinutes.toString(),
                    onValueChange = { cookTimeMinutes = it.toIntOrNull() ?: 0 },
                    label = { Text("${strings.cookTimeLabel} (${strings.minsUnit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = servings.toString(),
                    onValueChange = { servings = it.toIntOrNull() ?: 1 },
                    label = { Text(strings.servingsLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Photos & Videos
            Text(strings.photosVideosTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.photoLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Row {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.btnPickPhoto)
                            }

                            if (imageUrl != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { imageUrl = null }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    if (imageUrl != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Selected photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Video URL input
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text(strings.videoUrlLabel) },
                        placeholder = { Text("https://www.youtube.com/watch?v=...") },
                        trailingIcon = {
                            if (videoUrl.isNotBlank()) {
                                IconButton(onClick = { showVideoPreview = true }) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = "Preview", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Ingredients List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.ingredientsTitle} (${ingredients.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        ingredients.add(IngredientItem(name = "", amount = "", unit = ""))
                    },
                    modifier = Modifier.testTag("btn_add_ingredient_row")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.btnAddIngredient)
                }
            }

            ingredients.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = { newName ->
                            ingredients[index] = item.copy(name = newName)
                        },
                        placeholder = { Text(strings.ingredientNamePlaceholder) },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = item.amount,
                        onValueChange = { newAmount ->
                            ingredients[index] = item.copy(amount = newAmount)
                        },
                        placeholder = { Text(strings.ingredientAmountPlaceholder) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = item.unit,
                        onValueChange = { newUnit ->
                            ingredients[index] = item.copy(unit = newUnit)
                        },
                        placeholder = { Text(strings.ingredientUnitPlaceholder) },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (ingredients.size > 1) {
                                ingredients.removeAt(index)
                            } else {
                                ingredients[index] = IngredientItem(name = "", amount = "", unit = "")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 4: Cooking Steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.stepsTitle} (${steps.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        steps.add(CookingStep(stepNumber = steps.size + 1, instruction = "", timerMinutes = 0))
                    },
                    modifier = Modifier.testTag("btn_add_step_row")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.btnAddStep)
                }
            }

            steps.forEachIndexed { index, step ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = strings.stepTimerLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                OutlinedTextField(
                                    value = if (step.timerMinutes == 0) "" else step.timerMinutes.toString(),
                                    onValueChange = {
                                        val min = it.toIntOrNull() ?: 0
                                        steps[index] = step.copy(timerMinutes = min)
                                    },
                                    placeholder = { Text("0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(65.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                IconButton(
                                    onClick = {
                                        if (steps.size > 1) {
                                            steps.removeAt(index)
                                            val renumbered = steps.mapIndexed { idx, s -> s.copy(stepNumber = idx + 1) }
                                            steps.clear()
                                            steps.addAll(renumbered)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = step.instruction,
                            onValueChange = { newInstruction ->
                                steps[index] = step.copy(instruction = newInstruction)
                            },
                            placeholder = { Text(strings.stepDescPlaceholder) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            minLines = 2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 5: Notes / Secrets
            Text(strings.notesTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text(strings.notesPlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Big Save Button
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val recipeToSave = RecipeEntity(
                        id = recipeId ?: 0L,
                        title = title.trim(),
                        category = category.trim(),
                        description = description.trim(),
                        prepTimeMinutes = prepTimeMinutes,
                        cookTimeMinutes = cookTimeMinutes,
                        servings = servings,
                        imageUrl = imageUrl,
                        videoUrl = videoUrl.trim().ifBlank { null },
                        notes = notes.trim().ifBlank { null },
                        ingredientsJson = RecipeEntity.ingredientsToJson(ingredients.filter { it.name.isNotBlank() }),
                        stepsJson = RecipeEntity.stepsToJson(steps.filter { it.instruction.isNotBlank() })
                    )
                    viewModel.saveRecipe(recipeToSave) { savedId ->
                        onSaved(savedId)
                    }
                },
                enabled = title.isNotBlank() && !isAiGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_bottom_save"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.btnSaveRecipe, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
