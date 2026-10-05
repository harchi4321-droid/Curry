package com.example.data

import kotlinx.coroutines.flow.Flow

class RecipeRepository(
    private val recipeDao: RecipeDao,
    private val settingsDao: SettingsDao
) {

    val allRecipes: Flow<List<RecipeEntity>> = recipeDao.getAllRecipes()

    val favoriteRecipes: Flow<List<RecipeEntity>> = recipeDao.getFavoriteRecipes()

    fun getRecipesByCategory(category: String): Flow<List<RecipeEntity>> =
        recipeDao.getRecipesByCategory(category)

    fun getRecipeById(id: Long): Flow<RecipeEntity?> = recipeDao.getRecipeById(id)

    suspend fun getRecipeByIdOnce(id: Long): RecipeEntity? = recipeDao.getRecipeByIdOnce(id)

    fun searchRecipes(query: String): Flow<List<RecipeEntity>> = recipeDao.searchRecipes(query)

    suspend fun insertRecipe(recipe: RecipeEntity): Long = recipeDao.insertRecipe(recipe)

    suspend fun updateRecipe(recipe: RecipeEntity) = recipeDao.updateRecipe(recipe)

    suspend fun deleteRecipe(recipe: RecipeEntity) = recipeDao.deleteRecipe(recipe)

    suspend fun deleteAllRecipes() = recipeDao.deleteAllRecipes()

    suspend fun toggleFavorite(id: Long, isFav: Boolean) = recipeDao.updateFavorite(id, isFav)

    // Language setting persisted in Room DB
    fun getLanguageSetting(): Flow<String?> = settingsDao.getSetting("app_language")

    suspend fun setLanguageSetting(langCode: String) {
        settingsDao.setSetting(AppSettingEntity("app_language", langCode))
    }
}
