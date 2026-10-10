package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.local.ShoppingDao
import com.example.data.local.entities.HouseEntity
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.PurchaseHistoryEntity
import com.example.data.local.entities.PurchaseHistoryItemEntity
import com.example.data.local.entities.ShoppingItemEntity
import com.example.data.local.entities.ShoppingListEntity
import com.example.data.model.Categories
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class ShoppingRepository(
    private val db: FirebaseFirestore,
    private val dao: ShoppingDao
) {
    // Secondary constructor resolving named database ID from string resources
    constructor(context: Context, dao: ShoppingDao) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        dao
    )

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Current authenticated user
    private var currentUserId: String? = null
    private var currentUserName: String = "Você"

    // Firestore listener registrations
    private var housesListener: ListenerRegistration? = null
    private var listsListener: ListenerRegistration? = null
    private var itemsListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

    // Reactive StateFlows for real-time synchronization
    private val _houses = MutableStateFlow<List<HouseEntity>>(emptyList())
    val allHouses: Flow<List<HouseEntity>> = _houses.asStateFlow()

    private val _selectedHouse = MutableStateFlow<HouseEntity?>(null)
    val selectedHouse: Flow<HouseEntity?> = _selectedHouse.asStateFlow()

    private val _activeLists = MutableStateFlow<List<ShoppingListEntity>>(emptyList())
    private val _archivedLists = MutableStateFlow<List<ShoppingListEntity>>(emptyList())
    private val _items = MutableStateFlow<List<ShoppingItemEntity>>(emptyList())
    private val _purchaseHistory = MutableStateFlow<List<PurchaseHistoryEntity>>(emptyList())
    val allPurchaseHistory: Flow<List<PurchaseHistoryEntity>> = _purchaseHistory.asStateFlow()

    private val _isCloudSynced = MutableStateFlow(true)
    val isCloudSynced: Flow<Boolean> = _isCloudSynced.asStateFlow()

    private var activeListId: String? = null

    /**
     * Attaches authenticated user and starts real-time Firestore synchronization.
     */
    fun attachUser(userId: String, userName: String) {
        if (currentUserId == userId && housesListener != null) return
        detachUser()
        currentUserId = userId
        currentUserName = userName

        listenToHouses(userId)
    }

    fun detachUser() {
        housesListener?.remove()
        housesListener = null
        listsListener?.remove()
        listsListener = null
        itemsListener?.remove()
        itemsListener = null
        historyListener?.remove()
        historyListener = null
        currentUserId = null
    }

    private fun listenToHouses(userId: String) {
        val path = "houses"
        housesListener = db.collection(path)
            .whereArrayContains("memberUids", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    _isCloudSynced.value = false
                    return@addSnapshotListener
                }
                _isCloudSynced.value = true
                if (snapshot != null) {
                    val currentSelectedId = _selectedHouse.value?.id
                    val houseList = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val code = doc.getString("code") ?: "#CASA-0000"
                        HouseEntity(
                            id = doc.id,
                            name = name,
                            code = code,
                            isSelected = (doc.id == currentSelectedId)
                        )
                    }

                    if (houseList.isEmpty()) {
                        // Seed default house in Firestore
                        repositoryScope.launch {
                            seedDefaultCloudHouse(userId)
                        }
                    } else {
                        val activeHouse = houseList.firstOrNull { it.id == currentSelectedId }
                            ?: houseList.first()
                        _houses.value = houseList.map { it.copy(isSelected = it.id == activeHouse.id) }
                        _selectedHouse.value = activeHouse

                        listenToLists(activeHouse.id, userId)
                        listenToPurchaseHistory(activeHouse.id, userId)
                    }
                }
            }
    }

    private suspend fun seedDefaultCloudHouse(userId: String) {
        val houseId = "house-" + UUID.randomUUID().toString().take(8)
        val codeNum = Random.nextInt(1000, 9999)
        val code = "#CASA-$codeNum"
        val houseData = mapOf(
            "name" to "Casa Principal",
            "code" to code,
            "ownerId" to userId,
            "memberUids" to listOf(userId),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            db.collection("houses").document(houseId).set(houseData).await()

            // Seed default initial lists
            val listSemanaId = "list-semana-" + UUID.randomUUID().toString().take(6)
            val listSemana = mapOf(
                "houseId" to houseId,
                "name" to "Compra da Semana",
                "icon" to "🛒",
                "colorHex" to "#16A34A",
                "isArchived" to false,
                "memberUids" to listOf(userId),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val listChurrasco = mapOf(
                "houseId" to houseId,
                "name" to "Churrasco de Domingo",
                "icon" to "🥩",
                "colorHex" to "#E11D48",
                "isArchived" to false,
                "memberUids" to listOf(userId),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val listFeira = mapOf(
                "houseId" to houseId,
                "name" to "Feira",
                "icon" to "🥦",
                "colorHex" to "#22C55E",
                "isArchived" to false,
                "memberUids" to listOf(userId),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("shopping_lists").document(listSemanaId).set(listSemana).await()
            db.collection("shopping_lists").document("list-churrasco-" + UUID.randomUUID().toString().take(6)).set(listChurrasco).await()
            db.collection("shopping_lists").document("list-feira-" + UUID.randomUUID().toString().take(6)).set(listFeira).await()

            // Seed initial items for Compra da Semana
            val initialItems = listOf(
                Triple("Banana Nanica", "hortifruti", "1 dúzia"),
                Triple("Tomate Italiano", "hortifruti", "1 kg"),
                Triple("Pão Francês", "padaria", "6 unidades"),
                Triple("Queijo Mussarela Fatiado", "laticinios", "300 g"),
                Triple("Leite Integral (UHT)", "laticinios", "2 litros"),
                Triple("Peito de Frango (Filé)", "acougue", "1 kg"),
                Triple("Arroz Branco Tipo 1 (5kg)", "mercearia", "1 pacote"),
                Triple("Feijão Carioca (1kg)", "mercearia", "2 pacotes"),
                Triple("Café Torrado e Moído (500g)", "bebidas", "1 pacote"),
                Triple("Detergente Líquido Neutro", "limpeza", "2 frascos"),
                Triple("Papel Higiênico Folha Dupla", "higiene", "1 pacote"),
                Triple("Creme Dental com Flúor", "higiene", "2 tubos")
            )

            val batch = db.batch()
            for ((name, cat, qty) in initialItems) {
                val itemId = "item-" + UUID.randomUUID().toString()
                val itemData = mapOf(
                    "listId" to listSemanaId,
                    "houseId" to houseId,
                    "name" to name,
                    "category" to cat,
                    "quantity" to qty,
                    "itemCount" to 1,
                    "unitPrice" to 0.0,
                    "totalPrice" to 0.0,
                    "isCompleted" to false,
                    "notes" to "",
                    "addedByMemberName" to currentUserName,
                    "addedByUid" to userId,
                    "memberUids" to listOf(userId),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(db.collection("shopping_items").document(itemId), itemData)
            }
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "houses/$houseId")
        }
    }

    private fun listenToLists(houseId: String, userId: String) {
        listsListener?.remove()
        val path = "shopping_lists"
        listsListener = db.collection(path)
            .whereEqualTo("houseId", houseId)
            .whereArrayContains("memberUids", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    _isCloudSynced.value = false
                    return@addSnapshotListener
                }
                _isCloudSynced.value = true
                if (snapshot != null) {
                    val all = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("name") ?: return@mapNotNull null
                        ShoppingListEntity(
                            id = doc.id,
                            houseId = doc.getString("houseId") ?: houseId,
                            name = name,
                            icon = doc.getString("icon") ?: "🛒",
                            colorHex = doc.getString("colorHex") ?: "#16A34A",
                            isArchived = doc.getBoolean("isArchived") ?: false
                        )
                    }
                    _activeLists.value = all.filter { !it.isArchived }
                    _archivedLists.value = all.filter { it.isArchived }

                    // If active list is not set or not in lists, pick first
                    val currentListId = activeListId
                    val targetListId = if (currentListId != null && _activeLists.value.any { it.id == currentListId }) {
                        currentListId
                    } else {
                        _activeLists.value.firstOrNull()?.id
                    }
                    if (targetListId != null && targetListId != activeListId) {
                        selectActiveList(targetListId)
                    }
                }
            }
    }

    fun selectActiveList(listId: String) {
        activeListId = listId
        val userId = currentUserId ?: return
        listenToItems(listId, userId)
    }

    private fun listenToItems(listId: String, userId: String) {
        itemsListener?.remove()
        val path = "shopping_items"
        itemsListener = db.collection(path)
            .whereEqualTo("listId", listId)
            .whereArrayContains("memberUids", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    _isCloudSynced.value = false
                    return@addSnapshotListener
                }
                _isCloudSynced.value = true
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val catName = doc.getString("category") ?: "outros"
                        val cat = Categories.getByName(catName)
                        ShoppingItemEntity(
                            id = doc.id,
                            listId = doc.getString("listId") ?: listId,
                            houseId = doc.getString("houseId") ?: "",
                            name = name,
                            category = cat.id,
                            quantity = doc.getString("quantity") ?: "1",
                            itemCount = (doc.getLong("itemCount") ?: 1L).toInt(),
                            unitPrice = doc.getDouble("unitPrice") ?: 0.0,
                            isCompleted = doc.getBoolean("isCompleted") ?: false,
                            notes = doc.getString("notes") ?: "",
                            addedByMemberName = doc.getString("addedByMemberName") ?: "",
                            aisleOrder = cat.aisleNumber,
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                    // Sort items by completion and aisle order
                    _items.value = items.sortedWith(
                        compareBy<ShoppingItemEntity> { it.isCompleted }
                            .thenBy { it.aisleOrder }
                            .thenByDescending { it.createdAt }
                    )
                }
            }
    }

    private fun listenToPurchaseHistory(houseId: String, userId: String) {
        historyListener?.remove()
        val path = "purchase_history"
        historyListener = db.collection(path)
            .whereEqualTo("houseId", houseId)
            .whereArrayContains("memberUids", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val history = snapshot.documents.mapNotNull { doc ->
                        val listName = doc.getString("listName") ?: return@mapNotNull null
                        val time = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
                        val sortFormat = SimpleDateFormat("yyyy-MM", Locale.US)
                        val date = Date(time)

                        PurchaseHistoryEntity(
                            id = doc.id,
                            houseId = doc.getString("houseId") ?: houseId,
                            listName = listName,
                            totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                            itemCount = (doc.getLong("totalItemsCount") ?: 0L).toInt(),
                            timestamp = time,
                            monthYearKey = monthFormat.format(date).replaceFirstChar { it.uppercase() },
                            yearMonthSort = sortFormat.format(date)
                        )
                    }.sortedByDescending { it.timestamp }
                    _purchaseHistory.value = history
                }
            }
    }

    fun getPurchaseHistoryItems(historyId: String): Flow<List<PurchaseHistoryItemEntity>> =
        dao.getPurchaseHistoryItems(historyId)

    // --- Observable Flows for ViewModels ---
    fun getMembersByHouse(houseId: String): Flow<List<MemberEntity>> = dao.getMembersByHouse(houseId)
    fun getActiveLists(houseId: String): Flow<List<ShoppingListEntity>> = _activeLists.asStateFlow()
    fun getArchivedLists(houseId: String): Flow<List<ShoppingListEntity>> = _archivedLists.asStateFlow()
    fun getItemsByList(listId: String): Flow<List<ShoppingItemEntity>> {
        if (activeListId != listId) {
            selectActiveList(listId)
        }
        return _items.asStateFlow()
    }

    suspend fun initializeDefaultDataIfNeeded() {
        val userId = currentUserId ?: return
        val currentHouses = _houses.value
        if (currentHouses.isEmpty()) {
            seedDefaultCloudHouse(userId)
        }
    }

    // --- Item Write Operations ---
    suspend fun addItem(
        listId: String,
        houseId: String,
        name: String,
        category: String,
        quantity: String,
        notes: String,
        addedByMemberName: String
    ): ShoppingItemEntity {
        val userId = currentUserId ?: error("Usuário não autenticado")
        val cat = Categories.getByName(category)
        val itemId = "item-" + UUID.randomUUID().toString()
        val houseDoc = db.collection("houses").document(houseId).get().await()
        val memberUids = (houseDoc.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: listOf(userId)

        val itemData = mapOf(
            "listId" to listId,
            "houseId" to houseId,
            "name" to name.trim(),
            "category" to cat.id,
            "quantity" to quantity.ifBlank { "1" },
            "itemCount" to 1,
            "unitPrice" to 0.0,
            "totalPrice" to 0.0,
            "isCompleted" to false,
            "notes" to notes.trim(),
            "addedByMemberName" to addedByMemberName.ifBlank { currentUserName },
            "addedByUid" to userId,
            "memberUids" to memberUids,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            db.collection("shopping_items").document(itemId).set(itemData).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "shopping_items/$itemId")
        }

        return ShoppingItemEntity(
            id = itemId,
            listId = listId,
            houseId = houseId,
            name = name.trim(),
            category = cat.id,
            quantity = quantity.ifBlank { "1" },
            notes = notes.trim(),
            addedByMemberName = addedByMemberName.ifBlank { currentUserName },
            isCompleted = false,
            aisleOrder = cat.aisleNumber,
            createdAt = System.currentTimeMillis()
        )
    }

    suspend fun toggleItemCompleted(item: ShoppingItemEntity, newStatus: Boolean) {
        val docRef = db.collection("shopping_items").document(item.id)
        try {
            docRef.update(
                mapOf(
                    "isCompleted" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun updateItemPriceAndCount(itemId: String, unitPrice: Double, count: Int) {
        val docRef = db.collection("shopping_items").document(itemId)
        val totalPrice = unitPrice * count
        try {
            docRef.update(
                mapOf(
                    "unitPrice" to unitPrice,
                    "itemCount" to count,
                    "totalPrice" to totalPrice,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun updateItemCategory(itemId: String, newCategoryName: String) {
        val cat = Categories.getByName(newCategoryName)
        dao.updateItemCategory(itemId, cat.id, cat.aisleNumber)
        val docRef = db.collection("shopping_items").document(itemId)
        try {
            docRef.update(
                mapOf(
                    "category" to cat.id,
                    "aisleOrder" to cat.aisleNumber,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun deleteItem(itemId: String) {
        val docRef = db.collection("shopping_items").document(itemId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    suspend fun clearCompletedItems(listId: String) {
        val completedItems = _items.value.filter { it.listId == listId && it.isCompleted }
        if (completedItems.isEmpty()) return
        val batch = db.batch()
        for (item in completedItems) {
            batch.delete(db.collection("shopping_items").document(item.id))
        }
        try {
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "shopping_items")
        }
    }

    suspend fun deleteAllItemsInList(listId: String) {
        val listItems = _items.value.filter { it.listId == listId }
        if (listItems.isEmpty()) return
        val batch = db.batch()
        for (item in listItems) {
            batch.delete(db.collection("shopping_items").document(item.id))
        }
        try {
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "shopping_items")
        }
    }

    // --- Purchase Completion ---
    suspend fun completePurchase(
        houseId: String,
        listId: String,
        listName: String,
        purchasedItems: List<ShoppingItemEntity>
    ): PurchaseHistoryEntity {
        val userId = currentUserId ?: error("Usuário não autenticado")
        val houseDoc = db.collection("houses").document(houseId).get().await()
        val memberUids = (houseDoc.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: listOf(userId)

        val totalSpent = purchasedItems.sumOf { if (it.totalPrice > 0) it.totalPrice else (it.unitPrice * it.itemCount) }
        val totalCount = purchasedItems.sumOf { it.itemCount }
        val historyId = "hist-" + UUID.randomUUID().toString()
        val time = System.currentTimeMillis()

        val historyData = mapOf(
            "houseId" to houseId,
            "listName" to listName,
            "totalAmount" to totalSpent,
            "totalItemsCount" to totalCount,
            "completedByMember" to currentUserName,
            "memberUids" to memberUids,
            "timestamp" to time,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            db.collection("purchase_history").document(historyId).set(historyData).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "purchase_history/$historyId")
        }

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
        val sortFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = Date(time)

        return PurchaseHistoryEntity(
            id = historyId,
            houseId = houseId,
            listName = listName,
            totalAmount = totalSpent,
            itemCount = totalCount,
            timestamp = time,
            monthYearKey = monthFormat.format(date).replaceFirstChar { it.uppercase() },
            yearMonthSort = sortFormat.format(date)
        )
    }

    suspend fun deletePurchaseHistory(historyId: String) {
        val docRef = db.collection("purchase_history").document(historyId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    // --- List Operations ---
    suspend fun createList(houseId: String, name: String, icon: String = "🛒", colorHex: String = "#16A34A"): ShoppingListEntity {
        val userId = currentUserId ?: error("Usuário não autenticado")
        val listId = "list-" + UUID.randomUUID().toString()
        val houseDoc = db.collection("houses").document(houseId).get().await()
        val memberUids = (houseDoc.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: listOf(userId)

        val listData = mapOf(
            "houseId" to houseId,
            "name" to name.trim(),
            "icon" to icon,
            "colorHex" to colorHex,
            "isArchived" to false,
            "memberUids" to memberUids,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            db.collection("shopping_lists").document(listId).set(listData).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "shopping_lists/$listId")
        }

        return ShoppingListEntity(
            id = listId,
            houseId = houseId,
            name = name.trim(),
            icon = icon,
            colorHex = colorHex
        )
    }

    suspend fun duplicateList(listId: String): ShoppingListEntity? {
        val userId = currentUserId ?: return null
        val doc = db.collection("shopping_lists").document(listId).get().await()
        if (!doc.exists()) return null
        val houseId = doc.getString("houseId") ?: return null
        val name = doc.getString("name") ?: "Lista"
        val icon = doc.getString("icon") ?: "🛒"
        val colorHex = doc.getString("colorHex") ?: "#16A34A"

        val newList = createList(houseId, "$name (Cópia)", icon, colorHex)

        // Duplicate items
        val itemsSnapshot = db.collection("shopping_items")
            .whereEqualTo("listId", listId)
            .get().await()

        val batch = db.batch()
        for (itemDoc in itemsSnapshot.documents) {
            val newItemId = "item-" + UUID.randomUUID().toString()
            val data = itemDoc.data?.toMutableMap() ?: continue
            data["listId"] = newList.id
            data["isCompleted"] = false
            data["createdAt"] = FieldValue.serverTimestamp()
            data["updatedAt"] = FieldValue.serverTimestamp()
            batch.set(db.collection("shopping_items").document(newItemId), data)
        }
        batch.commit().await()
        return newList
    }

    suspend fun archiveList(listId: String, archive: Boolean = true) {
        val docRef = db.collection("shopping_lists").document(listId)
        try {
            docRef.update(
                mapOf(
                    "isArchived" to archive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun deleteList(listId: String) {
        deleteAllItemsInList(listId)
        val docRef = db.collection("shopping_lists").document(listId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    // --- House Operations ---
    suspend fun createHouse(name: String): HouseEntity {
        val userId = currentUserId ?: error("Usuário não autenticado")
        val houseId = "house-" + UUID.randomUUID().toString().take(8)
        val codeNum = Random.nextInt(1000, 9999)
        val code = "#CASA-$codeNum"

        val houseData = mapOf(
            "name" to name.trim(),
            "code" to code,
            "ownerId" to userId,
            "memberUids" to listOf(userId),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            db.collection("houses").document(houseId).set(houseData).await()
            // Create default list for this new house
            createList(houseId, "Lista Principal", "🛒", "#16A34A")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "houses/$houseId")
        }

        val house = HouseEntity(
            id = houseId,
            name = name.trim(),
            code = code,
            isSelected = true
        )
        selectHouse(houseId)
        return house
    }

    suspend fun joinHouseByCode(code: String): HouseEntity? {
        val userId = currentUserId ?: error("Usuário não autenticado")
        val cleanCode = if (code.startsWith("#")) code.trim().uppercase() else "#${code.trim().uppercase()}"

        val query = db.collection("houses").whereEqualTo("code", cleanCode).get().await()
        if (query.isEmpty) return null

        val houseDoc = query.documents.first()
        val houseId = houseDoc.id
        val houseName = houseDoc.getString("name") ?: "Casa da Família"

        try {
            // Add user to house memberUids
            houseDoc.reference.update("memberUids", FieldValue.arrayUnion(userId)).await()

            // Update memberUids on all lists of this house
            val lists = db.collection("shopping_lists").whereEqualTo("houseId", houseId).get().await()
            for (list in lists.documents) {
                list.reference.update("memberUids", FieldValue.arrayUnion(userId)).await()
            }

            // Update memberUids on all items of this house
            val items = db.collection("shopping_items").whereEqualTo("houseId", houseId).get().await()
            for (item in items.documents) {
                item.reference.update("memberUids", FieldValue.arrayUnion(userId)).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, houseDoc.reference.path)
        }

        selectHouse(houseId)
        return HouseEntity(
            id = houseId,
            name = houseName,
            code = cleanCode,
            isSelected = true
        )
    }

    suspend fun selectHouse(houseId: String) {
        val currentHouses = _houses.value
        val target = currentHouses.firstOrNull { it.id == houseId } ?: return
        _houses.value = currentHouses.map { it.copy(isSelected = it.id == houseId) }
        _selectedHouse.value = target

        val userId = currentUserId ?: return
        listenToLists(houseId, userId)
        listenToPurchaseHistory(houseId, userId)
    }

    // --- Member Management (Local and Family representation) ---
    suspend fun addMember(houseId: String, name: String, colorIndex: Int = 1) {
        val member = MemberEntity(
            id = UUID.randomUUID().toString(),
            houseId = houseId,
            name = name.trim(),
            avatarColorIndex = colorIndex,
            isCurrentUser = false
        )
        dao.insertMember(member)
    }

    suspend fun setCurrentMember(houseId: String, memberId: String) {
        dao.setCurrentMember(houseId, memberId)
    }

    /**
     * Format list nicely for WhatsApp sharing
     */
    fun formatForWhatsApp(
        houseName: String,
        houseCode: String,
        listName: String,
        items: List<ShoppingItemEntity>
    ): String {
        val completedCount = items.count { it.isCompleted }
        val totalCount = items.size
        val percent = if (totalCount > 0) (completedCount * 100) / totalCount else 0

        val sb = StringBuilder()
        sb.append("🛒 *Lista da Casa: $listName*\n")
        sb.append("🏠 $houseName ($houseCode)\n")
        sb.append("📊 Progresso: $completedCount/$totalCount itens comprados ($percent%)\n\n")

        val grouped = items.groupBy { Categories.getByName(it.category) }
            .toSortedMap(compareBy { it.aisleNumber })

        for ((cat, catItems) in grouped) {
            sb.append("${cat.emoji} *${cat.name.uppercase()}*\n")
            for (item in catItems) {
                val box = if (item.isCompleted) "✅" else "◻️"
                val qtyStr = if (item.quantity.isNotBlank() && item.quantity != "1") " (${item.quantity})" else ""
                val notesStr = if (item.notes.isNotBlank()) " - _${item.notes}_" else ""
                val authorStr = if (item.addedByMemberName.isNotBlank()) " [${item.addedByMemberName}]" else ""
                sb.append("$box ${item.name}$qtyStr$notesStr$authorStr\n")
            }
            sb.append("\n")
        }

        sb.append("Gerado por *Lista da Casa* 🛒")
        return sb.toString()
    }
}
