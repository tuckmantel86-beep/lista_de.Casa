package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_history")
data class PurchaseHistoryEntity(
    @PrimaryKey val id: String,
    val houseId: String,
    val listName: String,
    val totalAmount: Double,
    val itemCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val monthYearKey: String, // e.g. "Outubro 2026"
    val yearMonthSort: String // e.g. "2026-10" for sorting
)
