package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RecipeViewModel
import com.example.ui.components.LanguageSwitchButton
import com.example.util.AppLanguage

data class CategoryInfo(
    val name: String,
    val iconEmoji: String,
    val description: String,
    val containerColor: Color,
    val contentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: RecipeViewModel,
    onCategoryClick: (String) -> Unit,
    onAddRecipeWithCategory: (String) -> Unit
) {
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val allRecipes by viewModel.allRecipes.collectAsState()
    val strings by viewModel.strings.collectAsState()
    val currentLanguage by viewModel.appLanguage.collectAsState()

    val defaultCategories = remember(currentLanguage) {
        if (currentLanguage == AppLanguage.ENGLISH) {
            listOf(
                CategoryInfo("Curry / Main", "🍛", "Curries, braised & main dishes", Color(0xFFFFE0B2), Color(0xFFE65100)),
                CategoryInfo("Soup", "🍲", "Sour soup, broth, clear soups", Color(0xFFE1F5FE), Color(0xFF0288D1)),
                CategoryInfo("Salad", "🥗", "Tea leaf salad, ginger salad, mixed salads", Color(0xFFE8F5E9), Color(0xFF2E7D32)),
                CategoryInfo("Fried", "🍳", "Fried fritters, stir-fries, crispy bites", Color(0xFFFFF3E0), Color(0xFFF57C00)),
                CategoryInfo("Dessert", "🍰", "Traditional sweets & desserts", Color(0xFFFCE4EC), Color(0xFFC2185B)),
                CategoryInfo("Breakfast", "☕", "Mohinga, noodles, sticky rice", Color(0xFFFFF8E1), Color(0xFFFFA000)),
                CategoryInfo("Beverage", "🍹", "Fresh juices, tea & drinks", Color(0xFFE0F2F1), Color(0xFF00796B)),
                CategoryInfo("Snacks", "🥟", "Appetizers & afternoon snacks", Color(0xFFEDE7F6), Color(0xFF512DA8))
            )
        } else {
            listOf(
                CategoryInfo("ဟင်းလျာ", "🍛", "ဆီပြန်၊ နှပ်၊ ချက် အဓိကဟင်းလျာများ", Color(0xFFFFE0B2), Color(0xFFE65100)),
                CategoryInfo("ဟင်းချို", "🍲", "ချဉ်ရည်ဟင်း၊ ဟင်းခါး၊ ရည်သောက်များ", Color(0xFFE1F5FE), Color(0xFF0288D1)),
                CategoryInfo("အသုပ်", "🥗", "လက်ဖက်သုပ်၊ ဂျင်းသုပ်၊ အသုပ်စုံများ", Color(0xFFE8F5E9), Color(0xFF2E7D32)),
                CategoryInfo("အကြော်", "🍳", "ပဲကြော်၊ ငါးကြော်၊ အကြော်စုံများ", Color(0xFFFFF3E0), Color(0xFFF57C00)),
                CategoryInfo("အချိုပွဲ", "🍰", "ရိုးရာမုန့်၊ အချိုပွဲနှင့် သရေစာများ", Color(0xFFFCE4EC), Color(0xFFC2185B)),
                CategoryInfo("မနက်စာ", "☕", "မုန့်ဟင်းခါး၊ နန်းကြီးသုပ်၊ ကောက်ညှင်း", Color(0xFFFFF8E1), Color(0xFFFFA000)),
                CategoryInfo("အဖျော်ယမကာ", "🍹", "ဖျော်ရည်၊ လက်ဖက်ရည်၊ သဘာဝဖျော်ရည်", Color(0xFFE0F2F1), Color(0xFF00796B)),
                CategoryInfo("အဆာပြေ", "🥟", "နေ့လယ်စာအဆာပြေ၊ မုန့်တီ၊ မုန့်လက်ဆောင်း", Color(0xFFEDE7F6), Color(0xFF512DA8))
            )
        }
    }

    // Extra user-created categories
    val extraCategories = remember(categoryCounts, defaultCategories) {
        val defaultNames = defaultCategories.map { it.name }.toSet()
        categoryCounts.keys.filter { it !in defaultNames && it.isNotBlank() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.categoriesTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.totalRecipesPrefix} (${allRecipes.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(defaultCategories, key = { it.name }) { cat ->
                // Look up count by exact or partial match
                val count = categoryCounts[cat.name]
                    ?: categoryCounts.entries.firstOrNull { it.key.contains(cat.name) || cat.name.contains(it.key) }?.value
                    ?: 0

                CategoryGridItem(
                    category = cat,
                    count = count,
                    viewLabel = if (currentLanguage == AppLanguage.ENGLISH) "View" else "ကြည့်မည်",
                    onClick = { onCategoryClick(cat.name) }
                )
            }

            if (extraCategories.isNotEmpty()) {
                items(extraCategories, key = { it }) { catName ->
                    val count = categoryCounts[catName] ?: 0
                    val customCat = CategoryInfo(
                        name = catName,
                        iconEmoji = "🍱",
                        description = if (currentLanguage == AppLanguage.ENGLISH) "Custom category" else "စိတ်ကြိုက်အမျိုးအစား",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CategoryGridItem(
                        category = customCat,
                        count = count,
                        viewLabel = if (currentLanguage == AppLanguage.ENGLISH) "View" else "ကြည့်မည်",
                        onClick = { onCategoryClick(catName) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryGridItem(
    category: CategoryInfo,
    count: Int,
    viewLabel: String = "ကြည့်မည်",
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clickable(onClick = onClick)
            .testTag("category_card_${category.name}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = category.containerColor,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = category.iconEmoji,
                            fontSize = 22.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (count > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (count > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
