package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.RecipeEntity
import com.example.ui.RecipeViewModel
import com.example.ui.components.LanguageSwitchButton
import com.example.util.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryRecipesScreen(
    categoryName: String,
    viewModel: RecipeViewModel,
    onBack: () -> Unit,
    onRecipeClick: (Long) -> Unit,
    onAddRecipeInThisCategory: (String) -> Unit
) {
    val allRecipes by viewModel.allRecipes.collectAsState()
    val strings by viewModel.strings.collectAsState()
    val currentLanguage by viewModel.appLanguage.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(allRecipes, categoryName, searchQuery) {
        allRecipes.filter { recipe ->
            val matchesCategory = recipe.category.equals(categoryName, ignoreCase = true) ||
                    recipe.category.contains(categoryName, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    recipe.title.contains(searchQuery, ignoreCase = true) ||
                    recipe.ingredientsJson.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val emptyMsg = if (currentLanguage == AppLanguage.ENGLISH) {
        "No recipes in \"$categoryName\" yet"
    } else {
        "\"$categoryName\" အမျိုးအစားတွင် ဟင်းချက်နည်း မရှိသေးပါ"
    }

    val emptyHint = if (currentLanguage == AppLanguage.ENGLISH) {
        "Be the first to record a recipe note under this category!"
    } else {
        "ဤအမျိုးအစားထဲသို့ သင့်စိတ်ကြိုက် ချက်နည်းအသစ်ကို ပထမဆုံး ထည့်သွင်းမှတ်သားနိုင်ပါသည်"
    }

    val addBtnText = if (currentLanguage == AppLanguage.ENGLISH) {
        "+ Add $categoryName Recipe"
    } else {
        "+ $categoryName အသစ်ထည့်မည်"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(${filteredList.size}) ${if (currentLanguage == AppLanguage.ENGLISH) "recipes" else "ခု"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddRecipeInThisCategory(categoryName) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(addBtnText) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_category_recipe")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("${strings.searchPlaceholder} ($categoryName)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(70.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) strings.emptyTitle else emptyMsg,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = emptyHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { onAddRecipeInThisCategory(categoryName) },
                            modifier = Modifier.testTag("btn_add_in_category")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.btnAddNew)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { recipe ->
                        RecipeCardItem(
                            recipe = recipe,
                            minsUnit = strings.minsUnit,
                            servingsUnit = strings.servingsUnit,
                            stepsUnit = strings.stepsUnit,
                            videoBadge = strings.videoBadge,
                            onClick = { onRecipeClick(recipe.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(recipe) }
                        )
                    }
                }
            }
        }
    }
}
