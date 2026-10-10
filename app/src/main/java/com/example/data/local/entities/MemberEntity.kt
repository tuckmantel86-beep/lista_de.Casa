package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val houseId: String,
    val name: String,
    val avatarColorIndex: Int = 0,
    val isCurrentUser: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
