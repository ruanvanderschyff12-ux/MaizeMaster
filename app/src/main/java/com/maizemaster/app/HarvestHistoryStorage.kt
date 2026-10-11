
package com.maizemaster.app

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class HarvestHistoryStorage(context: Context) {

    private val preferences = context.getSharedPreferences(
        "maizemaster_harvest_history",
        Context.MODE_PRIVATE
    )

    fun loadRecords(fieldId: String): List<HarvestRecord> {
        return loadAllRecords().filter {
            it.fieldId == fieldId
        }.sortedByDescending { it.date }
    }

    private fun loadAllRecords(): MutableList<HarvestRecord> {
        val records = mutableListOf<HarvestRecord>()

        val json = preferences.getString(
            "harvest_records",
            "[]"
        ) ?: "[]"

        try {
            val array = JSONArray(json)

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                records.add(
                    HarvestRecord(
                        id = item.getString("id"),
                        fieldId = item.getString("fieldId"),
                        date = item.getString("date"),
                        hectares = item.getDouble("hectares"),
                        tons = item.getDouble("tons"),
                        notes = item.optString("notes", "")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(
                "MaizeMaster",
                "Unable to load harvest history",
                e
            )
        }

        return records
    }

    private fun saveAllRecords(
        records: List<HarvestRecord>
    ): Boolean {
        val array = JSONArray()

        for (record in records) {
            val item = JSONObject().apply {
                put("id", record.id)
                put("fieldId", record.fieldId)
                put("date", record.date)
                put("hectares", record.hectares)
                put("tons", record.tons)
                put("notes", record.notes)
            }

            array.put(item)
        }

        return preferences.edit()
            .putString("harvest_records", array.toString())
            .commit()
    }

    fun addRecord(record: HarvestRecord): Boolean {
        val records = loadAllRecords()
        records.add(record)
        return saveAllRecords(records)
    }

    fun updateRecord(updated: HarvestRecord): Boolean {
        val records = loadAllRecords()

        val index = records.indexOfFirst {
            it.id == updated.id &&
            it.fieldId == updated.fieldId
        }

        if (index == -1) return false

        records[index] = updated
        return saveAllRecords(records)
    }

    fun deleteRecord(
        fieldId: String,
        recordId: String
    ): Boolean {
        val records = loadAllRecords()

        val removed = records.removeAll {
            it.id == recordId &&
            it.fieldId == fieldId
        }

        return removed && saveAllRecords(records)
    }

    fun totalHectares(fieldId: String): Double {
        return loadRecords(fieldId).sumOf {
            it.hectares
        }
    }

    fun totalTons(fieldId: String): Double {
        return loadRecords(fieldId).sumOf {
            it.tons
        }
    }

    fun deleteFieldHistory(fieldId: String): Boolean {
        val records = loadAllRecords()
        records.removeAll { it.fieldId == fieldId }
        return saveAllRecords(records)
    }
}
