package com.medicalnotes.app.utils

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
    
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")
    
    override fun serialize(
        src: LocalTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return try {
            val timeString = src?.format(formatter) ?: "00:00"
            android.util.Log.d("LocalTimeAdapter", "Serializing time: $src -> $timeString")
            JsonPrimitive(timeString)
        } catch (e: Exception) {
            android.util.Log.e("LocalTimeAdapter", "Error serializing time: $src", e)
            JsonPrimitive("00:00")
        }
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
        } catch (e: DateTimeParseException) {
            android.util.Log.e("LocalTimeAdapter", "Invalid time format: ${json?.asString}, using 00:00", e)
            LocalTime.of(0, 0)
        } catch (e: Exception) {
            android.util.Log.e("LocalTimeAdapter", "Error parsing time: ${json?.asString}", e)
            LocalTime.of(0, 0)
        }
    }
} 