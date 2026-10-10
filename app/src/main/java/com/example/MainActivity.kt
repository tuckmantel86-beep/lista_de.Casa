package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppTopBar
import com.example.ui.dialogs.AddItemDialog
import com.example.ui.dialogs.HouseDialog
import com.example.ui.dialogs.MemberDialog
import com.example.ui.dialogs.NewListDialog
import com.example.ui.screens.ActiveListScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ListsScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SupermarketModeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.ShoppingViewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val auth = remember { FirebaseAuth.getInstance() }
                val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val credentialManager = remember { CredentialManager.create(context) }
                var isSigningIn by remember { mutableStateOf(false) }
                var authError by remember { mutableStateOf<String?>(null) }

                if (currentUser == null) {
                    SignInScreen(
                        isLoading = isSigningIn,
                        errorMessage = authError,
                        onSignInWithGoogle = {
                            coroutineScope.launch {
                                isSigningIn = true
                                authError = null
                                try {
                                    val webClientId = context.getString(R.string.default_web_client_id)
                                    val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()
                                    val result = credentialManager.getCredential(request = request, context = context)
                                    val credential = result.credential
                                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                                        auth.signInWithCredential(authCredential).await()
                                    } else {
                                        authError = "Tipo de credencial incompatível."
                                    }
                                } catch (e: GetCredentialCancellationException) {
                                    Log.w("MainActivity", "Sign-in was cancelled by user: ${e.message}")
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Google sign-in error", e)
                                    authError = "Falha ao entrar com Google: ${e.localizedMessage ?: "Tente novamente."}"
                                } finally {
                                    isSigningIn = false
                                }
                            }
                        }
                    )
                } else {
                    ListaDaCasaApp()
                }
            }
        }
    }
}

@Composable
fun ListaDaCasaApp(
    viewModel: ShoppingViewModel = viewModel()
) {
    val context = LocalContext.current

    // State flows from ViewModel
    val currentHouse by viewModel.currentHouse.collectAsStateWithLifecycle()
    val houses by viewModel.houses.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val currentMember by viewModel.currentMember.collectAsStateWithLifecycle()
    val activeLists by viewModel.activeLists.collectAsStateWithLifecycle()
    val archivedLists by viewModel.archivedLists.collectAsStateWithLifecycle()
    val currentList by viewModel.currentList.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val purchaseHistory by viewModel.purchaseHistory.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val hideCompleted by viewModel.hideCompletedInSupermarket.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val catalogSuggestions by viewModel.catalogSuggestions.collectAsStateWithLifecycle()
    val showConfetti by viewModel.showConfetti.collectAsStateWithLifecycle()
    val familyNotification by viewModel.familyNotification.collectAsStateWithLifecycle()
    val isCloudSynced by viewModel.isCloudSynced.collectAsStateWithLifecycle()

    // Dialog visibility states
    var showHouseDialog by remember { mutableStateOf(false) }
    var showMemberDialog by remember { mutableStateOf(false) }
    var showNewListDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    // Predictive back / BackHandler logic
    BackHandler(enabled = currentTab != NavigationTab.ActiveList) {
        when (currentTab) {
            NavigationTab.SupermarketMode -> viewModel.setTab(NavigationTab.ActiveList)
            NavigationTab.Lists -> viewModel.setTab(NavigationTab.ActiveList)
            NavigationTab.History -> viewModel.setTab(NavigationTab.ActiveList)
            else -> {}
        }
    }

    if (currentTab == NavigationTab.SupermarketMode) {
        SupermarketModeScreen(
            currentList = currentList,
            items = items,
            hideCompleted = hideCompleted,
            showConfetti = showConfetti,
            onToggleHideCompleted = { viewModel.toggleHideCompleted() },
            onToggleItem = { viewModel.toggleItemCompleted(it) },
            onUpdatePriceAndCount = { itemId, price, count ->
                viewModel.updateItemPriceAndCount(itemId, price, count)
            },
            onFinishPurchase = { clearItems ->
                viewModel.finishPurchase(clearItems)
            },
            onExportToWhatsApp = { viewModel.exportToWhatsApp(context) },
            onExitSupermarketMode = { viewModel.setTab(NavigationTab.ActiveList) },
            onDismissConfetti = { viewModel.dismissConfetti() }
        )
    } else {
        Scaffold(
            topBar = {
                AppTopBar(
                    currentHouse = currentHouse,
                    currentMember = currentMember,
                    notificationMessage = familyNotification,
                    isCloudSynced = isCloudSynced,
                    onHouseClick = { showHouseDialog = true },
                    onMemberClick = { showMemberDialog = true },
                    onDismissNotification = { viewModel.clearNotification() }
                )
            },
            bottomBar = {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.ActiveList,
                        onClick = { viewModel.setTab(NavigationTab.ActiveList) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == NavigationTab.ActiveList) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                                contentDescription = "Lista Atual"
                            )
                        },
                        label = { Text("Lista Atual") },
                        modifier = Modifier.testTag("tab_active_list")
                    )

                    NavigationBarItem(
                        selected = currentTab == NavigationTab.Lists,
                        onClick = { viewModel.setTab(NavigationTab.Lists) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == NavigationTab.Lists) Icons.Filled.FormatListBulleted else Icons.Outlined.FormatListBulleted,
                                contentDescription = "Todas as Listas"
                            )
                        },
                        label = { Text("Listas da Casa") },
                        modifier = Modifier.testTag("tab_all_lists")
                    )

                    NavigationBarItem(
                        selected = currentTab == NavigationTab.History,
                        onClick = { viewModel.setTab(NavigationTab.History) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == NavigationTab.History) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                                contentDescription = "Histórico & Gastos"
                            )
                        },
                        label = { Text("Histórico") },
                        modifier = Modifier.testTag("tab_history")
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    NavigationTab.ActiveList -> {
                        ActiveListScreen(
                            currentList = currentList,
                            items = items,
                            searchQuery = searchQuery,
                            catalogSuggestions = catalogSuggestions,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onAddItem = { name, cat, qty -> viewModel.addItem(name, cat, qty) },
                            onOpenDetailedAdd = { showAddItemDialog = true },
                            onToggleItem = { viewModel.toggleItemCompleted(it) },
                            onDeleteItem = { viewModel.deleteItem(it) },
                            onClearCompleted = { viewModel.clearCompletedItems() },
                            onStartSupermarketMode = { viewModel.setTab(NavigationTab.SupermarketMode) },
                            onExportToWhatsApp = { viewModel.exportToWhatsApp(context) },
                            onSwitchList = { viewModel.setTab(NavigationTab.Lists) },
                            onCreateNewList = { showNewListDialog = true },
                            onQuickCreateList = { viewModel.createList("Lista de Compras", "🛒", "#16A34A") },
                            onUpdateItemCategory = { itemId, newCat -> viewModel.updateItemCategory(itemId, newCat) }
                        )
                    }

                    NavigationTab.Lists -> {
                        ListsScreen(
                            activeLists = activeLists,
                            archivedLists = archivedLists,
                            currentListId = currentList?.id,
                            allItems = items,
                            onSelectList = { listId -> viewModel.selectList(listId) },
                            onStartShoppingMode = { listId ->
                                viewModel.selectList(listId)
                                viewModel.setTab(NavigationTab.SupermarketMode)
                            },
                            onDuplicateList = { viewModel.duplicateList(it) },
                            onArchiveList = { id, archive -> viewModel.archiveList(id, archive) },
                            onDeleteList = { viewModel.deleteList(it) },
                            onCreateNewList = { showNewListDialog = true }
                        )
                    }

                    NavigationTab.History -> {
                        HistoryScreen(
                            historyList = purchaseHistory,
                            onGetHistoryItems = { historyId -> viewModel.getHistoryItems(historyId) },
                            onDeleteHistory = { historyId -> viewModel.deletePurchaseHistory(historyId) }
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    // Dialogs
    if (showHouseDialog) {
        HouseDialog(
            houses = houses,
            currentHouse = currentHouse,
            onSelectHouse = { viewModel.selectHouse(it) },
            onCreateHouse = { viewModel.createHouse(it) },
            onJoinHouse = { viewModel.joinHouse(it) },
            onDismiss = { showHouseDialog = false }
        )
    }

    if (showMemberDialog) {
        MemberDialog(
            members = members,
            currentMember = currentMember,
            onSelectMember = { viewModel.selectMember(it) },
            onAddMember = { viewModel.addMember(it) },
            onSignOut = { viewModel.signOut() },
            onDismiss = { showMemberDialog = false }
        )
    }

    if (showNewListDialog) {
        NewListDialog(
            onConfirm = { name, icon, colorHex ->
                viewModel.createList(name, icon, colorHex)
                showNewListDialog = false
            },
            onDismiss = { showNewListDialog = false }
        )
    }

    if (showAddItemDialog) {
        AddItemDialog(
            initialName = searchQuery,
            onConfirm = { name, category, quantity, notes ->
                viewModel.addItem(name, category, quantity, notes)
                showAddItemDialog = false
            },
            onDismiss = { showAddItemDialog = false }
        )
    }
}
