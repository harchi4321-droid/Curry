package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.CookingStep
import com.example.data.RecipeEntity
import com.example.ui.RecipeViewModel
import com.example.ui.components.CookingTimerDialog
import com.example.ui.components.LanguageSwitchButton
import com.example.ui.components.VideoPlayerDialog
import com.example.util.AppLanguage
import com.example.util.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: Long,
    viewModel: RecipeViewModel,
    onBack: () -> Unit,
    onEditRecipe: (Long) -> Unit
) {
    val recipeFlow = remember(recipeId) { viewModel.getRecipeFlow(recipeId) }
    val recipe by recipeFlow.collectAsState(initial = null)
    val strings by viewModel.strings.collectAsState()
    val currentLanguage by viewModel.appLanguage.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var activeTimerStep by remember { mutableStateOf<CookingStep?>(null) }
    var showVideoPlayer by remember { mutableStateOf(false) }
    var showAiTipsSheet by remember { mutableStateOf(false) }

    val checkedIngredients = remember { mutableStateListOf<Int>() }

    if (recipe == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentRecipe = recipe!!
    val ingredients = remember(currentRecipe.ingredientsJson) { currentRecipe.parseIngredients() }
    val steps = remember(currentRecipe.stepsJson) { currentRecipe.parseSteps() }

    if (activeTimerStep != null) {
        val stepLabel = if (currentLanguage == AppLanguage.ENGLISH) "Step" else "အဆင့်"
        CookingTimerDialog(
            initialMinutes = activeTimerStep!!.timerMinutes,
            stepTitle = "$stepLabel ${activeTimerStep!!.stepNumber}: ${activeTimerStep!!.instruction}",
            onDismiss = { activeTimerStep = null }
        )
    }

    if (showVideoPlayer && !currentRecipe.videoUrl.isNullOrBlank()) {
        VideoPlayerDialog(
            videoUrl = currentRecipe.videoUrl!!,
            recipeTitle = currentRecipe.title,
            onDismiss = { showVideoPlayer = false }
        )
    }

    if (showDeleteConfirm) {
        val delTitle = if (currentLanguage == AppLanguage.ENGLISH) "Delete this recipe?" else "မှတ်စုအား ဖျက်ပစ်ရန် သေချာပါသလား?"
        val delText = if (currentLanguage == AppLanguage.ENGLISH) "\"${currentRecipe.title}\" will be permanently deleted." else "\"${currentRecipe.title}\" ဟင်းချက်နည်းမှတ်စုကို ပြန်လည်ရယူနိုင်မည် မဟုတ်ပါ။"
        val delBtn = if (currentLanguage == AppLanguage.ENGLISH) "Delete" else "ဖျက်မည်"

        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(delTitle) },
            text = { Text(delText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteRecipe(currentRecipe) {
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(delBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.btnCancel)
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!currentRecipe.videoUrl.isNullOrBlank()) {
                        FilledTonalButton(
                            onClick = { showVideoPlayer = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_watch_video")
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.btnWatchVideo)
                        }
                    }

                    Button(
                        onClick = { showAiTipsSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_ai_tips"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.btnAiAdvice)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Image with Back & Action buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val imageModel = currentRecipe.imageUrl ?: R.drawable.food_hero_banner_1791167374704
                AsyncImage(
                    model = imageModel,
                    contentDescription = currentRecipe.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top shadow scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )

                // Top navigation bar overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LanguageSwitchButton(
                            currentLanguage = currentLanguage,
                            onToggle = { viewModel.toggleLanguage() }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { viewModel.toggleFavorite(currentRecipe) },
                            modifier = Modifier.testTag("btn_detail_fav")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (currentRecipe.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (currentRecipe.isFavorite) Color(0xFFE91E63) else Color.White
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { onEditRecipe(currentRecipe.id) },
                            modifier = Modifier.testTag("btn_edit_recipe")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("btn_delete_recipe")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main Content Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Category Chip & Video indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(currentRecipe.category) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    if (!currentRecipe.videoUrl.isNullOrBlank()) {
                        SuggestionChip(
                            onClick = { showVideoPlayer = true },
                            label = { Text("🎬 ${strings.videoBadge}") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = currentRecipe.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                if (currentRecipe.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentRecipe.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time & Servings Summary Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.prepTimeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currentRecipe.prepTimeMinutes} ${strings.minsUnit}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(30.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.cookTimeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currentRecipe.cookTimeMinutes} ${strings.minsUnit}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(30.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.servingsLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${currentRecipe.servings}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 1: Ingredients with interactive checklist
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.ingredientsTitle} (${ingredients.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (checkedIngredients.isNotEmpty()) {
                        TextButton(onClick = { checkedIngredients.clear() }) {
                            Text(strings.btnResetChecklist)
                        }
                    }
                }

                Text(
                    text = strings.checklistHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ingredients.forEachIndexed { index, item ->
                    val isChecked = checkedIngredients.contains(index)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (isChecked) checkedIngredients.remove(index) else checkedIngredients.add(index)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) checkedIngredients.add(index) else checkedIngredients.remove(index)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (item.amount.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "${item.amount} ${item.unit}".trim(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Section 2: Step-by-Step Cooking Instructions
                Text(
                    text = "${strings.stepsTitle} (${steps.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                steps.forEach { step ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${step.stepNumber}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                if (step.timerMinutes > 0) {
                                    OutlinedButton(
                                        onClick = { activeTimerStep = step },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Timer,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "⏱ ${step.timerMinutes} ${strings.minsUnit}",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = step.instruction,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                // Section 3: Personal Notes / Chef Secrets
                if (!currentRecipe.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(28.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFF57F17),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.notesTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5D4037)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentRecipe.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF3E2723),
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // AI Tips Bottom Sheet Modal
    if (showAiTipsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiTipsSheet = false }
        ) {
            AiRecipeTipsContent(
                recipe = currentRecipe,
                viewModel = viewModel,
                strings = strings,
                currentLanguage = currentLanguage,
                onClose = { showAiTipsSheet = false }
            )
        }
    }
}

@Composable
fun AiRecipeTipsContent(
    recipe: RecipeEntity,
    viewModel: RecipeViewModel,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    onClose: () -> Unit
) {
    var customQuestion by remember { mutableStateOf("") }
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()

    val quickQuestions = remember(currentLanguage) {
        if (currentLanguage == AppLanguage.ENGLISH) {
            listOf(
                "What side dishes pair best with this?",
                "What chef secret will make this extra flavorful?",
                "How can I make this healthier with less oil?"
            )
        } else {
            listOf(
                "ဒီဟင်းနဲ့ လိုက်ဖက်မယ့် အရံဟင်း အကြံပြုပေးပါ",
                "ပိုမိုမွှေးပျံ့စေမယ့် စားဖိုမှူး လျှို့ဝှက်ချက် ဘာရှိမလဲ?",
                "ဆီမများဘဲ ကျန်းမာရေးနဲ့ညီညွတ်အောင် ဘယ်လိုချက်ရမလဲ?"
            )
        }
    }

    val adviceHeader = if (currentLanguage == AppLanguage.ENGLISH) "AI Chef Advice" else "AI စားဖိုမှူး အကြံပြုချက်"
    val adviceSub = if (currentLanguage == AppLanguage.ENGLISH) "Ask anything about \"${recipe.title}\"" else "\"${recipe.title}\" အတွက် သိလိုသည်များကို မေးမြန်းပါ"
    val quickQHeader = if (currentLanguage == AppLanguage.ENGLISH) "Quick questions:" else "အမြန်မေးခွန်းများ:"
    val chefTipTitle = if (currentLanguage == AppLanguage.ENGLISH) "Chef Tip:" else "စားဖိုမှူး အကြံပေးချက်:"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = adviceHeader,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Text(
            text = adviceSub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Quick suggestions
        Text(
            text = quickQHeader,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickQuestions.take(2).forEach { q ->
                OutlinedButton(
                    onClick = {
                        viewModel.sendChatMessage(q, recipe)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(q, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }

        // Recent response
        val lastAiMessage = chatMessages.lastOrNull { it.sender == "ai" }
        if (lastAiMessage != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(chefTipTitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = lastAiMessage.message,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        if (isChatLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    if (currentLanguage == AppLanguage.ENGLISH) "AI Chef is thinking..." else "AI စားဖိုမှူး စဉ်းစားနေပါသည်...",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Custom question input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customQuestion,
                onValueChange = { customQuestion = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(strings.askChefPlaceholder) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (customQuestion.isNotBlank()) {
                        viewModel.sendChatMessage(customQuestion, recipe)
                        customQuestion = ""
                    }
                },
                enabled = customQuestion.isNotBlank() && !isChatLoading
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
