package com.example.ai

import com.example.BuildConfig
import com.example.data.CookingStep
import com.example.data.IngredientItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeneratedRecipe(
    val title: String,
    val category: String,
    val description: String,
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val servings: Int,
    val ingredients: List<IngredientItem>,
    val steps: List<CookingStep>,
    val notes: String
)

object GeminiChefService {
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun generateRecipe(userPrompt: String, isEnglish: Boolean = false): Result<GeneratedRecipe> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException(
                    if (isEnglish) "Gemini API Key is required in AI Studio Secrets panel."
                    else "Gemini API Key ကို AI Studio Secrets တွင် ထည့်သွင်းပေးရန် လိုအပ်ပါသည်။"
                )
            )
        }

        val langInstruction = if (isEnglish) {
            "Respond in English language. Provide an authentic, flavorful recipe with precise instructions."
        } else {
            "When a user asks for a recipe or gives available ingredients, generate a complete, delicious recipe in Myanmar language (မြန်မာစာ)."
        }

        val systemPrompt = """
            You are an expert master chef and cooking assistant fluent in Burmese (မြန်မာဘာသာ) and English.
            $langInstruction
            
            You MUST return ONLY valid JSON matching this schema:
            {
              "title": "Recipe Title",
              "category": "Main / Soup / Salad / Fried / Dessert / Breakfast",
              "description": "Short summary of the dish",
              "prepTimeMinutes": 15,
              "cookTimeMinutes": 30,
              "servings": 4,
              "ingredients": [
                { "name": "Ingredient name", "amount": "quantity", "unit": "unit measurement" }
              ],
              "steps": [
                { "stepNumber": 1, "instruction": "Step instruction", "timerMinutes": 5 }
              ],
              "notes": "Chef tips and secrets"
            }
            Do not include Markdown backticks in the response. Return raw JSON.
        """.trimIndent()

        val fullPrompt = "$systemPrompt\n\nUser request: $userPrompt"

        try {
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", fullPrompt)
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val genConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.7)
                }
                put("generationConfig", genConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty()
                return@withContext Result.failure(Exception("API Error (${response.code}): $errBody"))
            }

            val responseText = response.body?.string().orEmpty()
            val parsedRecipe = parseRecipeJson(responseText)
            Result.success(parsedRecipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun askChef(
        question: String,
        recipeContext: String? = null,
        isEnglish: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException(
                    if (isEnglish) "Gemini API Key is required in AI Studio Secrets panel."
                    else "Gemini API Key ကို AI Studio Secrets တွင် ထည့်သွင်းပေးရန် လိုအပ်ပါသည်။"
                )
            )
        }

        val langInstruct = if (isEnglish) {
            "You are an expert chef assistant answering cooking questions warmly and helpfully in English."
        } else {
            "You are an expert chef assistant answering cooking questions warmly and helpfully in Burmese (မြန်မာဘာသာ)."
        }

        val prompt = buildString {
            append(langInstruct)
            if (!recipeContext.isNullOrBlank()) {
                append("\nCurrent Recipe Context:\n")
                append(recipeContext)
                append("\n")
            }
            append("\nUser Question: ")
            append(question)
            append("\nPlease give direct, practical cooking guidance, ingredient substitutions, or tips.")
        }

        try {
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty()
                return@withContext Result.failure(Exception("API Error: $errBody"))
            }

            val responseText = response.body?.string().orEmpty()
            val json = JSONObject(responseText)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRecipeJson(responseBody: String): GeneratedRecipe {
        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text", "{}") ?: "{}"

        val cleanJsonStr = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val r = JSONObject(cleanJsonStr)

        val ingredientsList = mutableListOf<IngredientItem>()
        val ingArray = r.optJSONArray("ingredients")
        if (ingArray != null) {
            for (i in 0 until ingArray.length()) {
                val item = ingArray.optJSONObject(i)
                if (item != null) {
                    ingredientsList.add(
                        IngredientItem(
                            name = item.optString("name", ""),
                            amount = item.optString("amount", ""),
                            unit = item.optString("unit", "")
                        )
                    )
                } else {
                    val str = ingArray.optString(i)
                    if (str.isNotBlank()) ingredientsList.add(IngredientItem(name = str))
                }
            }
        }

        val stepsList = mutableListOf<CookingStep>()
        val stepArray = r.optJSONArray("steps")
        if (stepArray != null) {
            for (i in 0 until stepArray.length()) {
                val item = stepArray.optJSONObject(i)
                if (item != null) {
                    stepsList.add(
                        CookingStep(
                            stepNumber = item.optInt("stepNumber", i + 1),
                            instruction = item.optString("instruction", ""),
                            timerMinutes = item.optInt("timerMinutes", 0)
                        )
                    )
                } else {
                    val str = stepArray.optString(i)
                    if (str.isNotBlank()) stepsList.add(CookingStep(stepNumber = i + 1, instruction = str))
                }
            }
        }

        return GeneratedRecipe(
            title = r.optString("title", "New Recipe"),
            category = r.optString("category", "Main"),
            description = r.optString("description", ""),
            prepTimeMinutes = r.optInt("prepTimeMinutes", 15),
            cookTimeMinutes = r.optInt("cookTimeMinutes", 30),
            servings = r.optInt("servings", 4),
            ingredients = ingredientsList,
            steps = stepsList,
            notes = r.optString("notes", "")
        )
    }
}
