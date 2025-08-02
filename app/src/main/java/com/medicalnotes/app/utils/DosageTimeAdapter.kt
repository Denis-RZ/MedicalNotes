package com.medicalnotes.app.utils

import com.google.gson.*
import com.medicalnotes.app.models.DosageTime
import java.lang.reflect.Type

class DosageTimeAdapter : JsonSerializer<DosageTime>, JsonDeserializer<DosageTime> {
    
    override fun serialize(
        src: DosageTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return try {
            val enumString = src?.name ?: DosageTime.MORNING.name
            android.util.Log.d("DosageTimeAdapter", "Serializing dosage time: $src -> $enumString")
            JsonPrimitive(enumString)
        } catch (e: Exception) {
            android.util.Log.e("DosageTimeAdapter", "Error serializing dosage time: $src", e)
            JsonPrimitive(DosageTime.MORNING.name)
        }
    }
    
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): DosageTime {
        return try {
            val enumString = json?.asString ?: DosageTime.MORNING.name
            val parsedEnum = DosageTime.valueOf(enumString)
            android.util.Log.d("DosageTimeAdapter", "Deserializing dosage time: $enumString -> $parsedEnum")
            parsedEnum
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("DosageTimeAdapter", "Invalid dosage time value: ${json?.asString}, using MORNING", e)
            DosageTime.MORNING
        } catch (e: Exception) {
            android.util.Log.e("DosageTimeAdapter", "Error parsing dosage time: ${json?.asString}", e)
            DosageTime.MORNING
        }
    }
} 