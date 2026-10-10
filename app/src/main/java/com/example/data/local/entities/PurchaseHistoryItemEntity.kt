package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_history_items")
data class PurchaseHistoryItemEntity(
    @PrimaryKey val id: String,
    val historyId: String,
    val name: String,
    val category: String,
    val quantity: String = "1",
    val count: Int = 1,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val addedByMemberName: String = "Você"
)
