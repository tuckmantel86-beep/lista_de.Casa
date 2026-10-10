package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.HouseEntity
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.PurchaseHistoryEntity
import com.example.data.local.entities.PurchaseHistoryItemEntity
import com.example.data.local.entities.ShoppingItemEntity
import com.example.data.local.entities.ShoppingListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    // --- HOUSES ---
    @Query("SELECT * FROM houses ORDER BY createdAt ASC")
    fun getAllHouses(): Flow<List<HouseEntity>>

    @Query("SELECT * FROM houses WHERE isSelected = 1 LIMIT 1")
    fun getSelectedHouse(): Flow<HouseEntity?>

    @Query("SELECT * FROM houses WHERE id = :id LIMIT 1")
    suspend fun getHouseById(id: String): HouseEntity?

    @Query("SELECT * FROM houses WHERE code = :code LIMIT 1")
    suspend fun getHouseByCode(code: String): HouseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHouse(house: HouseEntity)

    @Update
    suspend fun updateHouse(house: HouseEntity)

    @Query("UPDATE houses SET isSelected = CASE WHEN id = :houseId THEN 1 ELSE 0 END")
    suspend fun setSelectedHouse(houseId: String)

    @Query("DELETE FROM houses WHERE id = :houseId")
    suspend fun deleteHouse(houseId: String)

    // --- MEMBERS ---
    @Query("SELECT * FROM members WHERE houseId = :houseId ORDER BY createdAt ASC")
    fun getMembersByHouse(houseId: String): Flow<List<MemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)

    @Query("UPDATE members SET isCurrentUser = CASE WHEN id = :memberId THEN 1 ELSE 0 END WHERE houseId = :houseId")
    suspend fun setCurrentMember(houseId: String, memberId: String)

    @Query("DELETE FROM members WHERE id = :memberId")
    suspend fun deleteMember(memberId: String)

    // --- SHOPPING LISTS ---
    @Query("SELECT * FROM shopping_lists WHERE houseId = :houseId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveListsByHouse(houseId: String): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE houseId = :houseId AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedListsByHouse(houseId: String): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    suspend fun getListById(id: String): ShoppingListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ShoppingListEntity)

    @Update
    suspend fun updateList(list: ShoppingListEntity)

    @Query("UPDATE shopping_lists SET isArchived = :isArchived, updatedAt = :timestamp WHERE id = :listId")
    suspend fun setListArchived(listId: String, isArchived: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM shopping_lists WHERE id = :listId")
    suspend fun deleteList(listId: String)

    // --- SHOPPING ITEMS ---
    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY isCompleted ASC, aisleOrder ASC, createdAt DESC")
    fun getItemsByList(listId: String): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingItemEntity>)

    @Update
    suspend fun updateItem(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :itemId")
    suspend fun setItemCompleted(itemId: String, isCompleted: Boolean, completedAt: Long?)

    @Query("UPDATE shopping_items SET unitPrice = :unitPrice, itemCount = :count WHERE id = :itemId")
    suspend fun updateItemPriceAndCount(itemId: String, unitPrice: Double, count: Int)

    @Query("UPDATE shopping_items SET category = :category, aisleOrder = :aisleOrder WHERE id = :itemId")
    suspend fun updateItemCategory(itemId: String, category: String, aisleOrder: Int)

    @Query("DELETE FROM shopping_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)

    @Query("DELETE FROM shopping_items WHERE listId = :listId AND isCompleted = 1")
    suspend fun deleteCompletedItems(listId: String)

    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    suspend fun deleteAllItemsInList(listId: String)

    @Query("SELECT COUNT(*) FROM shopping_items WHERE listId = :listId")
    fun getItemCount(listId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM shopping_items WHERE listId = :listId AND isCompleted = 1")
    fun getCompletedItemCount(listId: String): Flow<Int>

    // --- PURCHASE HISTORY & EXPENSES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseHistory(history: PurchaseHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseHistoryItems(items: List<PurchaseHistoryItemEntity>)

    @Query("SELECT * FROM purchase_history WHERE houseId = :houseId ORDER BY timestamp DESC")
    fun getPurchaseHistoryByHouse(houseId: String): Flow<List<PurchaseHistoryEntity>>

    @Query("SELECT * FROM purchase_history_items WHERE historyId = :historyId")
    fun getPurchaseHistoryItems(historyId: String): Flow<List<PurchaseHistoryItemEntity>>

    @Query("DELETE FROM purchase_history WHERE id = :historyId")
    suspend fun deletePurchaseHistory(historyId: String)

    @Query("DELETE FROM purchase_history_items WHERE historyId = :historyId")
    suspend fun deletePurchaseHistoryItems(historyId: String)
}
