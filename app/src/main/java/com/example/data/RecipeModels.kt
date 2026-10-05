package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class IngredientItem(
    val name: String,
    val amount: String = "",
    val unit: String = ""
) {
    fun displayString(): String {
        return buildString {
            append(name)
            if (amount.isNotBlank()) {
                append(" - ")
                append(amount)
                if (unit.isNotBlank()) {
                    append(" ")
                    append(unit)
                }
            }
        }
    }
}

data class CookingStep(
    val stepNumber: Int,
    val instruction: String,
    val timerMinutes: Int = 0
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val description: String = "",
    val ingredientsJson: String = "[]",
    val stepsJson: String = "[]",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val prepTimeMinutes: Int = 15,
    val cookTimeMinutes: Int = 30,
    val servings: Int = 4,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun parseIngredients(): List<IngredientItem> {
        val list = mutableListOf<IngredientItem>()
        try {
            val jsonArray = JSONArray(ingredientsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i)
                if (obj != null) {
                    list.add(
                        IngredientItem(
                            name = obj.optString("name", ""),
                            amount = obj.optString("amount", ""),
                            unit = obj.optString("unit", "")
                        )
                    )
                } else {
                    val str = jsonArray.optString(i)
                    if (str.isNotBlank()) {
                        list.add(IngredientItem(name = str))
                    }
                }
            }
        } catch (_: Exception) {
            // fallback for plain lines
            ingredientsJson.lines().filter { it.isNotBlank() }.forEach {
                list.add(IngredientItem(name = it.trim()))
            }
        }
        return list
    }

    fun parseSteps(): List<CookingStep> {
        val list = mutableListOf<CookingStep>()
        try {
            val jsonArray = JSONArray(stepsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i)
                if (obj != null) {
                    list.add(
                        CookingStep(
                            stepNumber = obj.optInt("stepNumber", i + 1),
                            instruction = obj.optString("instruction", ""),
                            timerMinutes = obj.optInt("timerMinutes", 0)
                        )
                    )
                } else {
                    val str = jsonArray.optString(i)
                    if (str.isNotBlank()) {
                        list.add(CookingStep(stepNumber = i + 1, instruction = str))
                    }
                }
            }
        } catch (_: Exception) {
            stepsJson.lines().filter { it.isNotBlank() }.forEachIndexed { index, line ->
                list.add(CookingStep(stepNumber = index + 1, instruction = line.trim()))
            }
        }
        return list
    }

    companion object {
        fun ingredientsToJson(ingredients: List<IngredientItem>): String {
            val array = JSONArray()
            for (item in ingredients) {
                val obj = JSONObject()
                obj.put("name", item.name)
                obj.put("amount", item.amount)
                obj.put("unit", item.unit)
                array.put(obj)
            }
            return array.toString()
        }

        fun stepsToJson(steps: List<CookingStep>): String {
            val array = JSONArray()
            for (step in steps) {
                val obj = JSONObject()
                obj.put("stepNumber", step.stepNumber)
                obj.put("instruction", step.instruction)
                obj.put("timerMinutes", step.timerMinutes)
                array.put(obj)
            }
            return array.toString()
        }
    }
}
