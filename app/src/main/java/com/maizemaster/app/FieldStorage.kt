package com.maizemaster.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class FieldStorage(context: Context) {

    private val preferences = context.getSharedPreferences(
        "maizemaster_fields",
        Context.MODE_PRIVATE
    )

    fun loadFields(): MutableList<SavedField> {
        val fields = mutableListOf<SavedField>()
        val savedJson = preferences.getString(
            "saved_fields", "[]"
        ) ?: "[]"

        try {
            val array = JSONArray(savedJson)

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                fields.add(
                    SavedField(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        shape = item.getString("shape"),
                        width = item.getDouble("width"),
                        length = item.getDouble("length"),
                        rowSpacing = item.getDouble("rowSpacing"),
                        plantSpacing = item.getDouble("plantSpacing"),
                        maizeVariety = item.getString("maizeVariety"),
                        expectedYield = item.getDouble("expectedYield"),
                        harvestedHectares = item.optDouble(
                            "harvestedHectares", 0.0
                        ),
                        harvestedTons = item.optDouble(
                            "harvestedTons", 0.0
                        ),
                        plantingDate = item.optString(
                            "plantingDate", ""
                        ),
                        maturityDays = item.optInt(
                            "maturityDays", 0
                        ),
                        growthStage = item.optString(
                            "growthStage", ""
                        ),
                        growthNotes = item.optString(
                            "growthNotes", ""
                        ),
                        growthObservedDate = item.optString(
                            "growthObservedDate", ""
                        )
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "MaizeMaster",
                "Unable to read saved fields",
                e
            )
        }

        return fields
    }

    fun saveFields(fields: List<SavedField>): Boolean {
        val array = JSONArray()

        for (field in fields) {
            val item = JSONObject().apply {
                put("id", field.id)
                put("name", field.name)
                put("shape", field.shape)
                put("width", field.width)
                put("length", field.length)
                put("rowSpacing", field.rowSpacing)
                put("plantSpacing", field.plantSpacing)
                put("maizeVariety", field.maizeVariety)
                put("expectedYield", field.expectedYield)
                put("harvestedHectares", field.harvestedHectares)
                put("harvestedTons", field.harvestedTons)
                put("plantingDate", field.plantingDate)
                put("maturityDays", field.maturityDays)
                put("growthStage", field.growthStage)
                put("growthNotes", field.growthNotes)
                put("growthObservedDate", field.growthObservedDate)
            }

            array.put(item)
        }

        return preferences.edit()
            .putString("saved_fields", array.toString())
            .commit()
    }

    fun addField(field: SavedField): Boolean {
        val fields = loadFields()
        fields.add(field)
        return saveFields(fields)
    }

    fun updateField(updatedField: SavedField): Boolean {
        val fields = loadFields()
        val index = fields.indexOfFirst {
            it.id == updatedField.id
        }

        if (index == -1) return false

        fields[index] = updatedField
        return saveFields(fields)
    }

    fun deleteField(fieldId: String): Boolean {
        val fields = loadFields()
        val removed = fields.removeAll {
            it.id == fieldId
        }

        return removed && saveFields(fields)
    }
}