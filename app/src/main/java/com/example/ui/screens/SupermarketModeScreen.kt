package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ShoppingItemEntity
import com.example.data.local.entities.ShoppingListEntity
import com.example.data.model.Categories
import com.example.data.model.GroceryCategory
import com.example.ui.components.getMemberColor
import com.example.ui.confetti.ConfettiCelebration
import com.example.ui.dialogs.EditPriceDialog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupermarketModeScreen(
    currentList: ShoppingListEntity?,
    items: List<ShoppingItemEntity>,
    hideCompleted: Boolean,
    showConfetti: Boolean,
    onToggleHideCompleted: () -> Unit,
    onToggleItem: (ShoppingItemEntity) -> Unit,
    onUpdatePriceAndCount: (itemId: String, unitPrice: Double, count: Int) -> Unit,
    onFinishPurchase: (clearItems: Boolean) -> Unit,
    onExportToWhatsApp: () -> Unit,
    onExitSupermarketMode: () -> Unit,
    onDismissConfetti: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalItems = items.size
    val completedItems = items.count { it.isCompleted }
    val remainingItems = totalItems - completedItems
    val progress = if (totalItems > 0) completedItems.toFloat() / totalItems.toFloat() else 0f
    val progressPercent = (progress * 100).toInt()

    // Real-time purchase sum calculation
    val completedPurchaseTotal = items.filter { it.isCompleted }.sumOf { it.totalPrice }
    val fullListPotentialTotal = items.sumOf { it.totalPrice }

    // Option to toggle visibility of the total amount
    var showTotalAmount by remember { mutableStateOf(true) }

    // Item being edited for price and count
    var editingItemForPrice by remember { mutableStateOf<ShoppingItemEntity?>(null) }

    // Confirmation dialog before finishing purchase
    var showFinishDialog by remember { mutableStateOf(false) }
    var clearItemsOnFinish by remember { mutableStateOf(true) }

    // Filter items based on "Filtro de Faltantes"
    val displayedItems = remember(items, hideCompleted) {
        if (hideCompleted) items.filter { !it.isCompleted } else items
    }

    // Group items strictly in Supermarket Corredor Sequence (1 to 11)
    val groupedByAisle = remember(displayedItems) {
        displayedItems.groupBy { Categories.getByName(it.category) }
            .toSortedMap(compareBy { it.aisleNumber })
    }

    // Collapsed aisles state
    var collapsedAisles by remember { mutableStateOf(setOf<String>()) }
    val allAisleIds = remember(groupedByAisle) { groupedByAisle.keys.map { it.id }.toSet() }
    val areAllAislesCollapsed = allAisleIds.isNotEmpty() && collapsedAisles.containsAll(allAisleIds)

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onExitSupermarketMode,
                                    modifier = Modifier.testTag("btn_exit_supermarket_mode")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Sair do Modo Compras"
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🛒", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Modo Compras",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = currentList?.name ?: "Lista",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Toggle Show/Hide Total Amount
                                IconButton(
                                    onClick = { showTotalAmount = !showTotalAmount },
                                    modifier = Modifier.testTag("btn_toggle_show_total")
                                ) {
                                    Icon(
                                        imageVector = if (showTotalAmount) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (showTotalAmount) "Ocultar valor total" else "Mostrar valor total",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // WhatsApp Export
                                IconButton(
                                    onClick = onExportToWhatsApp,
                                    modifier = Modifier.testTag("supermarket_share_whatsapp")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Exportar WhatsApp",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Filtro de Faltantes Toggle Button
                                FilterChip(
                                    selected = hideCompleted,
                                    onClick = onToggleHideCompleted,
                                    label = {
                                        Text(
                                            text = if (hideCompleted) "Faltantes" else "Todos",
                                            fontSize = 11.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (hideCompleted) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("toggle_hide_completed")
                                )
                            }
                        }

                        // Dynamic Supermarket Progress & Total Spent Banner
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "$completedItems de $totalItems itens comprados",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    // Live Total Amount in Cart
                                    if (showTotalAmount) {
                                        val totalToDisplay = if (completedPurchaseTotal > 0.0) completedPurchaseTotal else fullListPotentialTotal
                                        val totalLabel = if (completedPurchaseTotal > 0.0) "Total no Carrinho" else "Total dos Itens"
                                        Text(
                                            text = "$totalLabel: " + String.format(Locale("pt", "BR"), "R$ %.2f", totalToDisplay),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (completedPurchaseTotal > 0.0 && fullListPotentialTotal > completedPurchaseTotal) {
                                            Text(
                                                text = "Previsão total: " + String.format(Locale("pt", "BR"), "R$ %.2f", fullListPotentialTotal),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Total no Carrinho: ••••••",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Text(
                                    text = "$progressPercent%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (remainingItems > 0) {
                                    Text(
                                        text = "Faltam $remainingItems itens",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                } else if (totalItems > 0) {
                                    Text(
                                        text = "🎉 Carrinho completo!",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                if (groupedByAisle.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            collapsedAisles = if (areAllAislesCollapsed) emptySet() else allAisleIds
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text(
                                            text = if (areAllAislesCollapsed) "Expandir corredores" else "Recolher corredores",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // Bottom Ergonomic Quick Summary with Total and Finish Button
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val bottomTotal = if (completedPurchaseTotal > 0.0) completedPurchaseTotal else fullListPotentialTotal
                            Text(
                                text = if (showTotalAmount) String.format(Locale("pt", "BR"), "R$ %.2f", bottomTotal) else "R$ •••••",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (completedItems > 0) "$completedItems itens comprados" else "$totalItems itens na lista",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showFinishDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("btn_finish_supermarket")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Concluir Compras", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (displayedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(text = "🎉", fontSize = 54.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (hideCompleted && completedItems > 0) "Todos os itens foram comprados!" else "Nenhum item na lista",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (hideCompleted) "Desative o filtro 'Faltantes' para rever os itens ou conclua suas compras."
                            else "Volte para a lista e adicione produtos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = { showFinishDialog = true }) {
                            Text("Finalizar e Salvar no Histórico")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    groupedByAisle.forEach { (category, catItems) ->
                        val isCollapsed = collapsedAisles.contains(category.id)
                        val catRemaining = catItems.count { !it.isCompleted }

                        item(key = "supermarket_aisle_${category.id}") {
                            SupermarketAisleCollapsibleHeader(
                                category = category,
                                isCollapsed = isCollapsed,
                                catRemaining = catRemaining,
                                totalCount = catItems.size,
                                onToggle = {
                                    collapsedAisles = if (isCollapsed) {
                                        collapsedAisles - category.id
                                    } else {
                                        collapsedAisles + category.id
                                    }
                                }
                            )
                        }

                        if (!isCollapsed) {
                            items(catItems, key = { it.id }) { item ->
                                SupermarketItemRow(
                                    item = item,
                                    onToggle = { onToggleItem(item) },
                                    onEditPrice = { editingItemForPrice = item },
                                    onUpdateCount = { newCount ->
                                        onUpdatePriceAndCount(item.id, item.unitPrice, newCount)
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Fullscreen Confetti Particle Animation when hitting 100%!
        if (showConfetti) {
            ConfettiCelebration(
                onFinished = onDismissConfetti,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Price & Count Editor Dialog
    if (editingItemForPrice != null) {
        val item = editingItemForPrice!!
        EditPriceDialog(
            itemName = item.name,
            currentPrice = item.unitPrice,
            currentCount = item.itemCount,
            onConfirm = { price, count ->
                onUpdatePriceAndCount(item.id, price, count)
                if (price > 0.0 && !item.isCompleted) {
                    onToggleItem(item)
                }
                editingItemForPrice = null
            },
            onDismiss = { editingItemForPrice = null }
        )
    }

    // Finish Purchase Dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Concluir Compras", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Deseja salvar esta compra no Histórico Mensal de Gastos?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Gasto:", fontWeight = FontWeight.Medium)
                                Text(
                                    String.format(Locale("pt", "BR"), "R$ %.2f", completedPurchaseTotal),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Itens Comprados:", style = MaterialTheme.typography.bodySmall)
                                Text("$completedItems itens", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(text = "🧹", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "A lista atual será limpa e salva no Histórico, aguardando a criação de uma nova lista de compras.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        onFinishPurchase(true)
                    },
                    modifier = Modifier.testTag("btn_confirm_finish_purchase")
                ) {
                    Text("Concluir e Limpar Lista")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Continuar Comprando")
                }
            }
        )
    }
}

@Composable
private fun SupermarketItemRow(
    item: ShoppingItemEntity,
    onToggle: () -> Unit,
    onEditPrice: () -> Unit,
    onUpdateCount: (newCount: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val memberColor = getMemberColor(item.addedByMemberName)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (item.isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (item.isCompleted) 0.dp else 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("supermarket_item_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Line 1: Checkbox + Name + Notes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isCompleted) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (item.isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Comprado",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Bold,
                        color = if (item.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (item.notes.isNotBlank()) {
                            Text(
                                text = item.notes,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(memberColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.addedByMemberName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Line 2: Ergonomic Stepper (- / +) and Unit Price Tag with Subtotal!
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stepper for count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { if (item.itemCount > 1) onUpdateCount(item.itemCount - 1) },
                        enabled = item.itemCount > 1,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Text(
                        text = "${item.itemCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = { onUpdateCount(item.itemCount + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                // Price Tag & Total Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (item.unitPrice > 0.0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable { onEditPrice() }
                        .testTag("edit_price_tag_${item.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Preço",
                            modifier = Modifier.size(14.dp),
                            tint = if (item.unitPrice > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (item.unitPrice > 0.0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %.2f", item.unitPrice) + if (item.itemCount > 1) " cada" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (item.itemCount > 1) {
                                    Text(
                                        text = "Total: " + String.format(Locale("pt", "BR"), "R$ %.2f", item.totalPrice),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Definir Preço",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupermarketAisleCollapsibleHeader(
    category: GroceryCategory,
    isCollapsed: Boolean,
    catRemaining: Int,
    totalCount: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isCollapsed) -90f else 0f,
        label = "supermarket_chevron"
    )

    Surface(
        color = category.color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("aisle_header_${category.id}")
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = category.emoji, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Corredor ${category.aisleNumber} • ${category.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = category.color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = category.color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = category.englishName,
                                style = MaterialTheme.typography.labelSmall,
                                color = category.color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCollapsed) "$totalCount iten${if (totalCount > 1) "s" else ""} • Toque para abrir" else category.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (catRemaining == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = if (catRemaining == 0) "Concluído ✓" else "$catRemaining restante${if (catRemaining > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (catRemaining == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isCollapsed) "Expandir corredor" else "Recolher corredor",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(rotationAngle)
                )
            }
        }
    }
}
