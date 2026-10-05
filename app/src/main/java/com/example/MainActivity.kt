package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.RecipeViewModel
import com.example.ui.screens.AddEditRecipeScreen
import com.example.ui.screens.AiChefScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CategoryRecipesScreen
import com.example.ui.screens.RecipeDetailScreen
import com.example.ui.screens.RecipeListScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CookNoteApp()
            }
        }
    }
}

sealed class Screen(val route: String) {
    object RecipeList : Screen("recipe_list")
    object Categories : Screen("categories")
    object AddRecipe : Screen("add_recipe?category={category}") {
        fun createRoute(category: String? = null) =
            if (category != null) "add_recipe?category=$category" else "add_recipe"
    }
    object CategoryRecipes : Screen("category_recipes/{categoryName}") {
        fun createRoute(categoryName: String) = "category_recipes/$categoryName"
    }
    object AiChef : Screen("ai_chef")
    object RecipeDetail : Screen("recipe_detail/{recipeId}") {
        fun createRoute(recipeId: Long) = "recipe_detail/$recipeId"
    }
    object EditRecipe : Screen("recipe_edit/{recipeId}") {
        fun createRoute(recipeId: Long) = "recipe_edit/$recipeId"
    }
}

@Composable
fun CookNoteApp(viewModel: RecipeViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val strings by viewModel.strings.collectAsState()

    val bottomNavItems = listOf(
        Triple(Screen.RecipeList.route, strings.tabRecipes, Icons.AutoMirrored.Filled.MenuBook),
        Triple(Screen.Categories.route, strings.tabCategories, Icons.Default.Category),
        Triple("add_recipe", strings.tabAddRecipe, Icons.Default.AddCircle),
        Triple(Screen.AiChef.route, strings.tabAiChef, Icons.Default.AutoAwesome)
    )

    val showBottomBar = currentRoute in listOf(
        Screen.RecipeList.route,
        Screen.Categories.route,
        "add_recipe",
        Screen.AddRecipe.route,
        Screen.AiChef.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { (route, label, icon) ->
                        val selected = when (route) {
                            "add_recipe" -> currentRoute?.startsWith("add_recipe") == true
                            else -> currentRoute == route
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_item_$route")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.RecipeList.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.RecipeList.route) {
                RecipeListScreen(
                    viewModel = viewModel,
                    onRecipeClick = { recipeId ->
                        navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                    },
                    onAddRecipeClick = {
                        navController.navigate("add_recipe")
                    },
                    onAiChefClick = {
                        navController.navigate(Screen.AiChef.route)
                    }
                )
            }

            composable(Screen.Categories.route) {
                CategoriesScreen(
                    viewModel = viewModel,
                    onCategoryClick = { categoryName ->
                        navController.navigate(Screen.CategoryRecipes.createRoute(categoryName))
                    },
                    onAddRecipeWithCategory = { categoryName ->
                        navController.navigate(Screen.AddRecipe.createRoute(categoryName))
                    }
                )
            }

            composable(
                route = Screen.CategoryRecipes.route,
                arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                CategoryRecipesScreen(
                    categoryName = categoryName,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onRecipeClick = { recipeId ->
                        navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                    },
                    onAddRecipeInThisCategory = { cat ->
                        navController.navigate(Screen.AddRecipe.createRoute(cat))
                    }
                )
            }

            composable(
                route = "add_recipe?category={category}",
                arguments = listOf(
                    navArgument("category") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val categoryArg = backStackEntry.arguments?.getString("category")
                AddEditRecipeScreen(
                    recipeId = null,
                    viewModel = viewModel,
                    initialCategory = categoryArg,
                    onBack = {
                        navController.popBackStack()
                    },
                    onSaved = { savedId ->
                        navController.navigate(Screen.RecipeDetail.createRoute(savedId)) {
                            popUpTo(Screen.RecipeList.route)
                        }
                    }
                )
            }

            composable("add_recipe") {
                AddEditRecipeScreen(
                    recipeId = null,
                    viewModel = viewModel,
                    initialCategory = null,
                    onBack = {
                        navController.popBackStack()
                    },
                    onSaved = { savedId ->
                        navController.navigate(Screen.RecipeDetail.createRoute(savedId)) {
                            popUpTo(Screen.RecipeList.route)
                        }
                    }
                )
            }

            composable(
                route = Screen.EditRecipe.route,
                arguments = listOf(navArgument("recipeId") { type = NavType.LongType })
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: 0L
                AddEditRecipeScreen(
                    recipeId = recipeId,
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    onSaved = { _ ->
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.RecipeDetail.route,
                arguments = listOf(navArgument("recipeId") { type = NavType.LongType })
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: 0L
                RecipeDetailScreen(
                    recipeId = recipeId,
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    onEditRecipe = { id ->
                        navController.navigate(Screen.EditRecipe.createRoute(id))
                    }
                )
            }

            composable(Screen.AiChef.route) {
                AiChefScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    onRecipeSaved = { newId ->
                        navController.navigate(Screen.RecipeDetail.createRoute(newId)) {
                            popUpTo(Screen.RecipeList.route)
                        }
                    }
                )
            }
        }
    }
}
