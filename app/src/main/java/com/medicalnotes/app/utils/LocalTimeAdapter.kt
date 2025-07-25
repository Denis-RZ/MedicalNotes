package com.medicalnotes.app.utils

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
    
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")
    
    override fun serialize(
        src: LocalTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        val timeString = src?.format(formatter) ?: "00:00"
        android.util.Log.d("LocalTimeAdapter", "Serializing time: $src -> $timeString")
        return JsonPrimitive(timeString)
    }
    
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LocalTime {
        return try {
            val timeString = json?.asString ?: "00:00"
            val parsedTime = LocalTime.parse(timeString, formatter)
            android.util.Log.d("LocalTimeAdapter", "Deserializing time: $timeString -> $parsedTime")
            parsedTime
        } catch (e: Exception) {
            android.util.Log.e("LocalTimeAdapter", "Error parsing time: ${json?.asString}", e)
            LocalTime.of(0, 0)
        }
    }
} 