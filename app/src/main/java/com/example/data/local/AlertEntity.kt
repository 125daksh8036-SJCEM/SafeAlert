package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alertType: String,
    val timestamp: Long,
    val deviceName: String,
    val deviceId: String,
    val source: String,
    val status: String,
    val locationDescription: String?
)
