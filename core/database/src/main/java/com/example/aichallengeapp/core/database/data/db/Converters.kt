package com.example.aichallengeapp.core.database.data.db

import androidx.room.TypeConverter
import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromJson(value: String): List<ChatMessage> =
        json.decodeFromString(ListSerializer(ChatMessage.serializer()), value)

    @TypeConverter
    fun toJson(messages: List<ChatMessage>): String =
        json.encodeToString(ListSerializer(ChatMessage.serializer()), messages)
}
