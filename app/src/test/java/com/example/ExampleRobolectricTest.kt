package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppSettingEntity
import com.example.data.CookingStep
import com.example.data.IngredientItem
import com.example.data.RecipeEntity
import com.example.util.AppLanguage
import com.example.util.Localization
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Myo Min Khant", appName)
    }

    @Test
    fun `test recipe serialization and parsing`() {
        val ingredients = listOf(
            IngredientItem("ကြက်သား", "၅၀", "ကျပ်သား"),
            IngredientItem("ကြက်သွန်နီ", "၃", "လုံး")
        )
        val steps = listOf(
            CookingStep(1, "ကြက်သားကို သန့်စင်အောင် ဆေးကြောပါ", 5),
            CookingStep(2, "ဆီသတ်ပြီး လုံးချက်ပါ", 15)
        )

        val entity = RecipeEntity(
            title = "ကြက်သားဆီပြန်ဟင်း",
            category = "ဟင်းလျာ",
            ingredientsJson = RecipeEntity.ingredientsToJson(ingredients),
            stepsJson = RecipeEntity.stepsToJson(steps)
        )

        val parsedIngredients = entity.parseIngredients()
        val parsedSteps = entity.parseSteps()

        assertEquals(2, parsedIngredients.size)
        assertEquals("ကြက်သား", parsedIngredients[0].name)
        assertEquals("၅၀", parsedIngredients[0].amount)

        assertEquals(2, parsedSteps.size)
        assertEquals(1, parsedSteps[0].stepNumber)
        assertEquals(5, parsedSteps[0].timerMinutes)
    }

    @Test
    fun `test room database settings and language persistence`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val settingsDao = db.settingsDao()

        settingsDao.setSetting(AppSettingEntity("app_language", "en"))
        val savedLang = settingsDao.getSettingOnce("app_language")
        assertEquals("en", savedLang)

        val enStrings = Localization.get(AppLanguage.ENGLISH)
        assertEquals("Recipes", enStrings.tabRecipes)
        assertEquals("Categories", enStrings.tabCategories)

        val myStrings = Localization.get(AppLanguage.MYANMAR)
        assertEquals("မှတ်စုများ", myStrings.tabRecipes)
        assertEquals("အမျိုးအစား", myStrings.tabCategories)
    }
}
