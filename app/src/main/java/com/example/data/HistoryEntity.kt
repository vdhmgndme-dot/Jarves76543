package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userInput: String,
    val intent: String,
    val actionPayload: String = "",
    val status: String, // SUCCESS, FAILED, APP_NOT_INSTALLED, REQUIRES_CONFIRMATION
    val spokenResponse: String,
    val executionTimeMs: Long = 0
)
