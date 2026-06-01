package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "layouts")
data class LayoutEntity(
    @PrimaryKey val id: String, // e.g. "preset_standard", "preset_racing", "preset_sim" or "custom_12345"
    val name: String,
    val isPreset: Boolean,
    val inputsJson: String, // JSON serialization of List<ControllerInput>
    val lastModified: Long = System.currentTimeMillis()
)
