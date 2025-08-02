package com.medicalnotes.app.utils

import com.google.gson.*
import com.medicalnotes.app.models.DosageFrequency
import java.lang.reflect.Type

class DosageFrequencyAdapter : JsonSerializer<DosageFrequency>, JsonDeserializer<DosageFrequency> {
    
    override fun serialize(
        src: DosageFrequency?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return try {
            val enumString = src?.name ?: DosageFrequency.DAILY.name
            android.util.Log.d("DosageFrequencyAdapter", "Serializing frequency: $src -> $enumString")
            JsonPrimitive(enumString)
        } catch (e: Exception) {
            android.util.Log.e("DosageFrequencyAdapter", "Error serializing frequency: $src", e)
            JsonPrimitive(DosageFrequency.DAILY.name)
        }
    }
    
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): DosageFrequency {
        return try {
            val enumString = json?.asString ?: DosageFrequency.DAILY.name
            val parsedEnum = DosageFrequency.valueOf(enumString)
            android.util.Log.d("DosageFrequencyAdapter", "Deserializing frequency: $enumString -> $parsedEnum")
            parsedEnum
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("DosageFrequencyAdapter", "Invalid frequency value: ${json?.asString}, using DAILY", e)
            DosageFrequency.DAILY
        } catch (e: Exception) {
            android.util.Log.e("DosageFrequencyAdapter", "Error parsing frequency: ${json?.asString}", e)
            DosageFrequency.DAILY
        }
    }
} 