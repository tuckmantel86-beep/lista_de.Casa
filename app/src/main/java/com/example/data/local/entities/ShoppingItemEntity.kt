package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val houseId: String,
    val name: String,
    val category: String, // e.g. "hortifruti", "padaria", etc.
    val quantity: String = "1",
    val notes: String = "",
    val addedByMemberName: String = "Você",
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val aisleOrder: Int = 11, // order 1 to 11 for supermarket aisles
    val unitPrice: Double = 0.0, // Price in BRL
    val itemCount: Int = 1, // Multiplier for price and purchase
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalPrice: Double
        get() = unitPrice * itemCount
}
