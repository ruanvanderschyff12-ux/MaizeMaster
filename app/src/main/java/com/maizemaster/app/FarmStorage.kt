
package com.maizemaster.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class FarmField(
    val id: Long,
    val name: String,
    val shape: String,
    val width: Double,
    val length: Double,
    val rowSpacing: Double,
    val plantSpacing: Double,
    val maizeType: String,
    val expectedYieldPerHa: Double,
    val harvestedTons: Double = 0.0
)

class FarmStorage(context: Context) {

    private val prefs = context.getSharedPreferences(
        "maizemaster_farm_data",
        Context.MODE_PRIVATE
    )

    fun saveFields(fields: List<FarmField>) {
        val array = JSONArray()

        for (field in fields) {
            array.put(JSONObject().apply {
                put("id", field.id)
                put("name", field.name)
                put("shape", field.shape)
                put("width", field.width)
                put("length", field.length)
                put("rowSpacing", field.rowSpacing)
                put("plantSpacing", field.plantSpacing)
                put("maizeType", field.maizeType)
                put("expectedYieldPerHa", field.expectedYieldPerHa)
                put("harvestedTons", field.harvestedTons)
            })
        }

        val saved = prefs.edit()
            .putString("fields", array.toString())
            .commit()

        if (!saved) {
            throw IllegalStateException(
                "Could not save field data"
            )
        }
    }

    fun loadFields(): List<FarmField> {
        val array = JSONArray(
            prefs.getString("fields", "[]") ?: "[]"
        )

        val fields = mutableListOf<FarmField>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            fields.add(
                FarmField(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    shape = obj.getString("shape"),
                    width = obj.getDouble("width"),
                    length = obj.getDouble("length"),
                    rowSpacing = obj.getDouble("rowSpacing"),
                    plantSpacing = obj.getDouble("plantSpacing"),
                    maizeType = obj.getString("maizeType"),
                    expectedYieldPerHa =
                        obj.getDouble("expectedYieldPerHa"),
                    harvestedTons =
                        obj.optDouble("harvestedTons", 0.0)
                )
            )
        }

        return fields
    }
}
