package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.ExpenseSplit
import com.example.data.model.IncomeContribution
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        val array = JSONArray()
        list?.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val result = mutableListOf<String>()
            for (i in 0 until array.length()) {
                result.add(array.optString(i))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromExpenseSplitList(splits: List<ExpenseSplit>?): String {
        val array = JSONArray()
        splits?.forEach { split ->
            val obj = JSONObject().apply {
                put("id", split.id)
                put("expenseId", split.expenseId)
                put("participantId", split.participantId)
                put("amount", split.amount)
                put("percentage", split.percentage)
            }
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toExpenseSplitList(json: String?): List<ExpenseSplit> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val result = mutableListOf<ExpenseSplit>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                result.add(
                    ExpenseSplit(
                        id = obj.optString("id"),
                        expenseId = obj.optString("expenseId"),
                        participantId = obj.optString("participantId"),
                        amount = obj.optDouble("amount", 0.0),
                        percentage = obj.optDouble("percentage", 0.0)
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromIncomeContributionList(contributions: List<IncomeContribution>?): String {
        val array = JSONArray()
        contributions?.forEach { item ->
            val obj = JSONObject().apply {
                put("participantId", item.participantId)
                put("amount", item.amount)
                if (item.debtDiscount != null) {
                    put("debtDiscount", item.debtDiscount)
                }
            }
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toIncomeContributionList(json: String?): List<IncomeContribution> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val result = mutableListOf<IncomeContribution>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val discount = if (obj.has("debtDiscount") && !obj.isNull("debtDiscount")) {
                    obj.optDouble("debtDiscount", 0.0)
                } else null
                result.add(
                    IncomeContribution(
                        participantId = obj.optString("participantId"),
                        amount = obj.optDouble("amount", 0.0),
                        debtDiscount = discount
                    )
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}
