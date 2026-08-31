package com.example.data.local

import androidx.room.TypeConverter
import com.example.model.AnomalyType
import com.example.model.MatchMode
import com.example.model.PlatformType
import com.example.model.PriceThresholdMode

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString("|||") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split("|||").filter { it.isNotBlank() }
    }

    @TypeConverter
    fun fromPlatformTypeList(value: List<PlatformType>?): String {
        return value?.joinToString(",") { it.name } ?: ""
    }

    @TypeConverter
    fun toPlatformTypeList(value: String?): List<PlatformType> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",").mapNotNull {
            try {
                val trimmed = it.trim()
                when (trimmed) {
                    "YAHOO" -> PlatformType.YAHOO_CENTER
                    "FINDPRICE" -> PlatformType.SHOPEE
                    else -> PlatformType.valueOf(trimmed)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun fromPlatformType(value: PlatformType?): String {
        return value?.name ?: PlatformType.SHOPEE.name
    }

    @TypeConverter
    fun toPlatformType(value: String?): PlatformType {
        return try {
            if (value != null) {
                when (value) {
                    "YAHOO" -> PlatformType.YAHOO_CENTER
                    "FINDPRICE" -> PlatformType.SHOPEE
                    else -> PlatformType.valueOf(value)
                }
            } else PlatformType.SHOPEE
        } catch (e: Exception) {
            PlatformType.SHOPEE
        }
    }

    @TypeConverter
    fun fromMatchMode(value: MatchMode?): String {
        return value?.name ?: MatchMode.CONTAINS.name
    }

    @TypeConverter
    fun toMatchMode(value: String?): MatchMode {
        return try {
            if (value != null) MatchMode.valueOf(value) else MatchMode.CONTAINS
        } catch (e: Exception) {
            MatchMode.CONTAINS
        }
    }

    @TypeConverter
    fun fromPriceThresholdMode(value: PriceThresholdMode?): String {
        return value?.name ?: PriceThresholdMode.BOTH_OR.name
    }

    @TypeConverter
    fun toPriceThresholdMode(value: String?): PriceThresholdMode {
        return try {
            if (value != null) PriceThresholdMode.valueOf(value) else PriceThresholdMode.BOTH_OR
        } catch (e: Exception) {
            PriceThresholdMode.BOTH_OR
        }
    }

    @TypeConverter
    fun fromAnomalyType(value: AnomalyType?): String {
        return value?.name ?: AnomalyType.GOOD_PRICE.name
    }

    @TypeConverter
    fun toAnomalyType(value: String?): AnomalyType {
        return try {
            if (value != null) AnomalyType.valueOf(value) else AnomalyType.GOOD_PRICE
        } catch (e: Exception) {
            AnomalyType.GOOD_PRICE
        }
    }
}
