package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.entities.HouseEntity
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.PurchaseHistoryEntity
import com.example.data.local.entities.PurchaseHistoryItemEntity
import com.example.data.local.entities.ShoppingItemEntity
import com.example.data.local.entities.ShoppingListEntity

@Database(
    entities = [
        HouseEntity::class,
        MemberEntity::class,
        ShoppingListEntity::class,
        ShoppingItemEntity::class,
        PurchaseHistoryEntity::class,
        PurchaseHistoryItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lista_da_casa.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
