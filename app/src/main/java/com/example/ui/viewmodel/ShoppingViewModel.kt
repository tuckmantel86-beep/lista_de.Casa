package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundAndHapticHelper
import com.example.data.local.AppDatabase
import com.example.data.local.entities.HouseEntity
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.PurchaseHistoryEntity
import com.example.data.local.entities.PurchaseHistoryItemEntity
import com.example.data.local.entities.ShoppingItemEntity
import com.example.data.local.entities.ShoppingListEntity
import com.example.data.model.CatalogData
import com.example.data.model.CatalogItem
import com.example.data.model.Categories
import com.example.data.repository.ShoppingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

sealed class NavigationTab {
    object Lists : NavigationTab()
    object ActiveList : NavigationTab()
    object History : NavigationTab()
    object SupermarketMode : NavigationTab()
}

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShoppingRepository
    val soundHelper: SoundAndHapticHelper = SoundAndHapticHelper(application)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ShoppingRepository(application, db.shoppingDao())

        // Sync with Firebase Auth state
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                repository.attachUser(user.uid, user.displayName ?: "Você")
            } else {
                repository.detachUser()
            }
        }
        val currentUser = auth.currentUser
        if (currentUser != null) {
            repository.attachUser(currentUser.uid, currentUser.displayName ?: "Você")
        }
    }

    val isCloudSynced: StateFlow<Boolean> = repository.isCloudSynced
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val houses: StateFlow<List<HouseEntity>> = repository.allHouses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentHouse: StateFlow<HouseEntity?> = repository.selectedHouse
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val members: StateFlow<List<MemberEntity>> = currentHouse.flatMapLatest { house ->
        if (house != null) repository.getMembersByHouse(house.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMember: StateFlow<MemberEntity?> = members.combine(flowOf(Unit)) { memberList, _ ->
        val user = auth.currentUser
        if (user != null) {
            MemberEntity(
                id = user.uid,
                houseId = currentHouse.value?.id ?: "",
                name = user.displayName ?: "Você",
                avatarColorIndex = 0,
                isCurrentUser = true
            )
        } else {
            memberList.firstOrNull { it.isCurrentUser } ?: memberList.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeLists: StateFlow<List<ShoppingListEntity>> = currentHouse.flatMapLatest { house ->
        if (house != null) repository.getActiveLists(house.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val archivedLists: StateFlow<List<ShoppingListEntity>> = currentHouse.flatMapLatest { house ->
        if (house != null) repository.getArchivedLists(house.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedListId = MutableStateFlow<String?>(null)
    val selectedListId: StateFlow<String?> = _selectedListId.asStateFlow()

    private val _isAwaitingNewList = MutableStateFlow(false)
    val isAwaitingNewList: StateFlow<Boolean> = _isAwaitingNewList.asStateFlow()

    val currentList: StateFlow<ShoppingListEntity?> = combine(activeLists, _selectedListId, _isAwaitingNewList) { lists, selId, awaitingNew ->
        if (awaitingNew) {
            null
        } else if (selId != null) {
            lists.firstOrNull { it.id == selId } ?: lists.firstOrNull()
        } else {
            lists.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<ShoppingItemEntity>> = currentList.flatMapLatest { list ->
        if (list != null) repository.getItemsByList(list.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Purchase history & monthly expenses
    val purchaseHistory: StateFlow<List<PurchaseHistoryEntity>> = repository.allPurchaseHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    private val _currentTab = MutableStateFlow<NavigationTab>(NavigationTab.ActiveList)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _hideCompletedInSupermarket = MutableStateFlow(false)
    val hideCompletedInSupermarket: StateFlow<Boolean> = _hideCompletedInSupermarket.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showConfetti = MutableStateFlow(false)
    val showConfetti: StateFlow<Boolean> = _showConfetti.asStateFlow()

    private val _familyNotification = MutableStateFlow<String?>(null)
    val familyNotification: StateFlow<String?> = _familyNotification.asStateFlow()

    // Realtime Catalog suggestions based on active query
    val catalogSuggestions: StateFlow<List<CatalogItem>> = _searchQuery.combine(flowOf(Unit)) { query, _ ->
        if (query.length >= 2) CatalogData.search(query) else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun toggleHideCompleted() {
        _hideCompletedInSupermarket.value = !_hideCompletedInSupermarket.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearNotification() {
        _familyNotification.value = null
    }

    fun dismissConfetti() {
        _showConfetti.value = false
    }

    // --- House Actions ---
    fun selectHouse(houseId: String) {
        viewModelScope.launch {
            repository.selectHouse(houseId)
            _selectedListId.value = null
            _isAwaitingNewList.value = false
        }
    }

    fun createHouse(name: String) {
        viewModelScope.launch {
            val house = repository.createHouse(name)
            _selectedListId.value = null
            _isAwaitingNewList.value = false
            _familyNotification.value = "🏠 Nova casa '${house.name}' criada! Código de convite: ${house.code}"
            soundHelper.playSuccessChime()
        }
    }

    fun joinHouse(code: String) {
        viewModelScope.launch {
            val house = repository.joinHouseByCode(code)
            if (house != null) {
                _selectedListId.value = null
                _isAwaitingNewList.value = false
                _familyNotification.value = "🤝 Você conectou à casa '${house.name}'!"
                soundHelper.playSuccessChime()
            } else {
                _familyNotification.value = "⚠️ Código inválido ou casa não encontrada: $code"
            }
        }
    }

    // --- Member Actions ---
    fun selectMember(memberId: String) {
        val house = currentHouse.value ?: return
        viewModelScope.launch {
            repository.setCurrentMember(house.id, memberId)
        }
    }

    fun addMember(name: String) {
        val house = currentHouse.value ?: return
        val colors = listOf(1, 2, 3, 4, 5)
        val colorIndex = colors[Random.nextInt(colors.size)]
        viewModelScope.launch {
            repository.addMember(house.id, name, colorIndex)
            _familyNotification.value = "👤 Membro '$name' adicionado à família!"
            soundHelper.playSuccessChime()
        }
    }

    // --- List Actions ---
    fun selectList(listId: String) {
        _isAwaitingNewList.value = false
        _selectedListId.value = listId
        repository.selectActiveList(listId)
    }

    fun createList(name: String, icon: String = "🛒", colorHex: String = "#16A34A") {
        val house = currentHouse.value ?: return
        viewModelScope.launch {
            _isAwaitingNewList.value = false
            val newList = repository.createList(house.id, name, icon, colorHex)
            _selectedListId.value = newList.id
            repository.selectActiveList(newList.id)
            _familyNotification.value = "✨ Nova lista '${newList.name}' criada!"
            soundHelper.playSuccessChime()
        }
    }

    fun duplicateList(listId: String) {
        viewModelScope.launch {
            val duplicated = repository.duplicateList(listId)
            if (duplicated != null) {
                _isAwaitingNewList.value = false
                _selectedListId.value = duplicated.id
                _familyNotification.value = "📋 Lista duplicada com sucesso!"
                soundHelper.playSuccessChime()
            }
        }
    }

    fun archiveList(listId: String, archive: Boolean = true) {
        viewModelScope.launch {
            repository.archiveList(listId, archive)
            val msg = if (archive) "📦 Lista arquivada!" else "📂 Lista desarquivada!"
            _familyNotification.value = msg
        }
    }

    fun deleteList(listId: String) {
        viewModelScope.launch {
            repository.deleteList(listId)
            _familyNotification.value = "🗑️ Lista excluída!"
        }
    }

    fun addItem(name: String, categoryName: String? = null, quantity: String = "1", notes: String = "") {
        val house = currentHouse.value ?: return
        val member = currentMember.value?.name ?: "Você"

        val detectedCat = if (!categoryName.isNullOrBlank()) {
            Categories.getByName(categoryName)
        } else {
            Categories.detectCategory(name)
        }

        viewModelScope.launch {
            val list = currentList.value ?: run {
                _isAwaitingNewList.value = false
                val newList = repository.createList(house.id, "Lista de Compras", "🛒", "#16A34A")
                _selectedListId.value = newList.id
                repository.selectActiveList(newList.id)
                newList
            }

            repository.addItem(
                listId = list.id,
                houseId = house.id,
                name = name,
                category = detectedCat.id,
                quantity = quantity,
                notes = notes,
                addedByMemberName = member
            )
            _searchQuery.value = ""
        }
    }

    fun toggleItemCompleted(item: ShoppingItemEntity) {
        val newStatus = !item.isCompleted
        viewModelScope.launch {
            repository.toggleItemCompleted(item, newStatus)

            if (newStatus) {
                soundHelper.playCheckoutBeep()
                soundHelper.vibrateItemChecked()

                val currentItems = items.value
                val willBeAllCompleted = currentItems.all { if (it.id == item.id) true else it.isCompleted }
                if (willBeAllCompleted && currentItems.isNotEmpty()) {
                    _showConfetti.value = true
                    soundHelper.playSuccessChime()
                    soundHelper.vibrateCelebration()
                    _familyNotification.value = "🎉 Parabéns! Todos os itens da lista foram comprados! 🛒"
                }
            }
        }
    }

    fun updateItemPriceAndCount(itemId: String, unitPrice: Double, count: Int) {
        viewModelScope.launch {
            repository.updateItemPriceAndCount(itemId, unitPrice, count)
        }
    }

    fun updateItemCategory(itemId: String, newCategoryId: String) {
        viewModelScope.launch {
            val cat = Categories.getByName(newCategoryId)
            repository.updateItemCategory(itemId, cat.id)
            _familyNotification.value = "🏷️ Categoria atualizada para ${cat.emoji} ${cat.name} (${cat.englishName})!"
            soundHelper.playSuccessChime()
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
        }
    }

    fun clearCompletedItems() {
        val list = currentList.value ?: return
        viewModelScope.launch {
            repository.clearCompletedItems(list.id)
            _familyNotification.value = "🧹 Itens comprados removidos!"
        }
    }

    /**
     * Completes the supermarket shopping trip, saves the purchase and all bought items to
     * Purchase History with date, amounts, clears the current list completely, archives it, and sets
     * state to await the creation of a new shopping list.
     */
    fun finishPurchase(clearItems: Boolean = true) {
        val house = currentHouse.value ?: return
        val list = currentList.value ?: return
        val currentItems = items.value
        val completed = currentItems.filter { it.isCompleted }
        val itemsToRecord = if (completed.isNotEmpty()) completed else currentItems

        if (itemsToRecord.isEmpty()) return

        viewModelScope.launch {
            val history = repository.completePurchase(
                houseId = house.id,
                listId = list.id,
                listName = list.name,
                purchasedItems = itemsToRecord
            )
            // 1. Completely clear all items in the current list
            repository.deleteAllItemsInList(list.id)
            // 2. Archive this finished list so it's safely recorded in archives
            repository.archiveList(list.id, true)
            // 3. Reset active list and enter awaiting state for a new list
            _selectedListId.value = null
            _isAwaitingNewList.value = true

            soundHelper.playSuccessChime()
            soundHelper.vibrateCelebration()
            val formattedTotal = String.format(Locale("pt", "BR"), "R$ %.2f", history.totalAmount)
            _familyNotification.value = "🧾 Compra de $formattedTotal concluída! A lista foi limpa e salva no Histórico."
            _currentTab.value = NavigationTab.History
        }
    }

    fun getHistoryItems(historyId: String): Flow<List<PurchaseHistoryItemEntity>> {
        return repository.getPurchaseHistoryItems(historyId)
    }

    fun deletePurchaseHistory(historyId: String) {
        viewModelScope.launch {
            repository.deletePurchaseHistory(historyId)
            _familyNotification.value = "🗑️ Compra removida do histórico."
        }
    }

    fun simulateFamilyActivity() {
        val list = currentList.value ?: return
        val house = currentHouse.value ?: return
        val otherMembers = members.value.filter { !it.isCurrentUser }
        val memberName = if (otherMembers.isNotEmpty()) {
            otherMembers[Random.nextInt(otherMembers.size)].name
        } else {
            "Mariana"
        }

        val catalogItem = CatalogData.ITEMS[Random.nextInt(CatalogData.ITEMS.size)]

        viewModelScope.launch {
            repository.addItem(
                listId = list.id,
                houseId = house.id,
                name = catalogItem.name,
                category = catalogItem.categoryId,
                quantity = catalogItem.defaultQuantity,
                notes = "Sugerido por $memberName",
                addedByMemberName = memberName
            )
            _familyNotification.value = "$memberName acabou de adicionar ${catalogItem.name} ${catalogItem.emoji} à lista!"
            soundHelper.playFamilyActivitySound()
        }
    }

    fun exportToWhatsApp(context: android.content.Context) {
        val list = currentList.value ?: return
        val house = currentHouse.value ?: return
        val currentItems = items.value

        val formattedText = repository.formatForWhatsApp(
            houseName = house.name,
            houseCode = house.code,
            listName = list.name,
            items = currentItems
        )

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, formattedText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Exportar Lista da Casa para WhatsApp")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun signOut() {
        auth.signOut()
    }
}
