package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "houses")
data class HouseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String, // e.g. #CASA-8421
    val isSelected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
