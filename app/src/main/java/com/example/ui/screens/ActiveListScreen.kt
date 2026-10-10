package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ShoppingItemEntity
import com.example.data.local.entities.ShoppingListEntity
import com.example.data.model.CatalogItem
import com.example.data.model.Categories
import com.example.data.model.GroceryCategory
import com.example.ui.components.ItemRow
import com.example.ui.components.SearchAndQuickAddWidget
import com.example.ui.dialogs.AddItemDialog
import com.example.ui.dialogs.CatalogBrowserDialog
import com.example.ui.dialogs.ChangeCategoryDialog

@Composable
fun ActiveListScreen(
    currentList: ShoppingListEntity?,
    items: List<ShoppingItemEntity>,
    searchQuery: String,
    catalogSuggestions: List<CatalogItem>,
    onSearchQueryChange: (String) -> Unit,
    onAddItem: (name: String, categoryName: String?, quantity: String) -> Unit,
    onOpenDetailedAdd: () -> Unit,
    onToggleItem: (ShoppingItemEntity) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onStartSupermarketMode: () -> Unit,
    onExportToWhatsApp: () -> Unit,
    onSwitchList: () -> Unit,
    onCreateNewList: () -> Unit = {},
    onQuickCreateList: () -> Unit = {},
    onUpdateItemCategory: (itemId: String, newCategoryId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showCatalogBrowser by remember { mutableStateOf(false) }
    var itemToChangeCategory by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var quickAddInCategory by remember { mutableStateOf<GroceryCategory?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    // Dialog for changing an item's category
    if (itemToChangeCategory != null) {
        ChangeCategoryDialog(
            item = itemToChangeCategory!!,
            onSelectCategory = { newCategory ->
                onUpdateItemCategory(itemToChangeCategory!!.id, newCategory.id)
                itemToChangeCategory = null
            },
            onDismiss = { itemToChangeCategory = null }
        )
    }

    // Quick add dialog pre-selected in a specific category
    if (quickAddInCategory != null) {
        AddItemDialog(
            initialName = "",
            onConfirm = { name, cat, qty, notes ->
                onAddItem(name, cat, qty)
                quickAddInCategory = null
            },
            onDismiss = { quickAddInCategory = null }
        )
    }

    if (showCatalogBrowser) {
        CatalogBrowserDialog(
            onAddItem = { name, cat, qty ->
                onAddItem(name, cat, qty)
            },
            onDismiss = { showCatalogBrowser = false }
        )
    }

    if (currentList == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🛒✨", fontSize = 52.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Aguardando Nova Lista",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sua compra anterior foi concluída e salva no Histórico! A lista foi limpa e está pronta para a criação de uma nova lista de compras.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onCreateNewList,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_create_new_list_empty")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Criar Nova Lista", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FilledTonalButton(
                        onClick = onQuickCreateList,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_quick_create_list")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Criar Lista Rápida (\"Lista de Compras\")", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showCatalogBrowser = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_explore_catalog_empty")
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Explorar Catálogo Completo (550+ Itens)", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onSwitchList,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_see_all_lists_empty")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Todas as Listas e Histórico", fontSize = 13.sp)
                    }
                }
            }
        }
        return
    }

    val totalItems = items.size
    val completedItems = items.count { it.isCompleted }
    val progressPercent = if (totalItems > 0) (completedItems * 100) / totalItems else 0

    // Filter items based on active search query and category filter chip
    val filteredItems = remember(items, searchQuery, selectedCategoryFilter) {
        val base = if (searchQuery.isBlank()) items
        else items.filter {
            it.name.contains(searchQuery.trim(), ignoreCase = true) ||
            it.notes.contains(searchQuery.trim(), ignoreCase = true)
        }
        if (selectedCategoryFilter == null) base
        else base.filter { Categories.getByName(it.category).id == selectedCategoryFilter }
    }

    // Group items by category (ordered by supermarket aisle sequence 1 to 11)
    val groupedItems = remember(filteredItems) {
        filteredItems.groupBy { Categories.getByName(it.category) }
            .toSortedMap(compareBy { it.aisleNumber })
    }

    // State for tracking collapsed categories
    var collapsedCategories by remember { mutableStateOf(setOf<String>()) }

    val allCategoryIds = remember(groupedItems) { groupedItems.keys.map { it.id }.toSet() }
    val areAllCollapsed = allCategoryIds.isNotEmpty() && collapsedCategories.containsAll(allCategoryIds)

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxSize()) {
        // Ultra-compact Header Card at the top with Search & Quick Add Widget
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Row 1: List title with progress subtitle, and compact action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = currentList.icon, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentList.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "$completedItems de $totalItems itens comprados ($progressPercent%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Compact Modo Compras button
                        FilledTonalButton(
                            onClick = onStartSupermarketMode,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_enter_supermarket_mode")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Modo Compras",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Toggle Collapse/Expand all categories icon button
                        if (groupedItems.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    collapsedCategories = if (areAllCollapsed) emptySet() else allCategoryIds
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("btn_toggle_collapse_all")
                            ) {
                                Icon(
                                    imageVector = if (areAllCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                                    contentDescription = if (areAllCollapsed) "Expandir categorias" else "Recolher categorias",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // WhatsApp Share Button
                        IconButton(
                            onClick = onExportToWhatsApp,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_export_whatsapp")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartilhar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (completedItems > 0) {
                            IconButton(
                                onClick = onClearCompleted,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("btn_clear_completed")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = "Limpar comprados",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Thin progress line
                if (totalItems > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { completedItems.toFloat() / totalItems.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Unified Search & Quick-Add Widget with Auto-Categorization!
                SearchAndQuickAddWidget(
                    query = searchQuery,
                    suggestions = catalogSuggestions,
                    onQueryChange = onSearchQueryChange,
                    onAddItem = onAddItem,
                    onOpenDetailedAdd = onOpenDetailedAdd,
                    onOpenCatalogBrowser = { showCatalogBrowser = true }
                )
            }
        }

        // Category Filter & Organization Bar (helps users organize list by Produce, Dairy, Bakery, etc.)
        if (items.isNotEmpty()) {
            val categoriesInList = remember(items) {
                items.map { Categories.getByName(it.category) }.distinctBy { it.id }.sortedBy { it.aisleNumber }
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_filter_row")
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("Todas ($totalItems)", fontSize = 11.sp) },
                            leadingIcon = {
                                if (selectedCategoryFilter == null) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_chip_all")
                        )
                    }

                    items(categoriesInList, key = { it.id }) { cat ->
                        val isSelected = selectedCategoryFilter == cat.id
                        val countInCat = items.count { Categories.getByName(it.category).id == cat.id }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryFilter = if (isSelected) null else cat.id
                            },
                            label = {
                                Text("${cat.emoji} ${cat.name.split(" ").first()} ($countInCat)", fontSize = 11.sp)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.color.copy(alpha = 0.2f),
                                selectedLabelColor = cat.color
                            ),
                            modifier = Modifier.testTag("filter_chip_${cat.id}")
                        )
                    }
                }
            }
        }

        // Dedicated, full-height list for items and categories
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (filteredItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategoryFilter != null) "🔍" else "🛒",
                            fontSize = 44.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (selectedCategoryFilter != null) {
                                val catObj = Categories.getByName(selectedCategoryFilter!!)
                                "Nenhum item em ${catObj.emoji} ${catObj.name}"
                            } else if (searchQuery.isNotBlank()) {
                                "Nenhum item na lista para \"$searchQuery\""
                            } else {
                                "Nova Lista Pronta!"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedCategoryFilter != null) {
                                "Toque em 'Todas' para ver os outros corredores ou adicione um novo produto nesta categoria!"
                            } else if (searchQuery.isNotBlank()) {
                                "Toque no botão (+) acima para adicioná-lo automaticamente com a categoria detectada!"
                            } else {
                                "Digite acima, use os atalhos de essenciais ou explore mais de 550 itens no Catálogo Completo!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (searchQuery.isBlank() && selectedCategoryFilter == null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            FilledTonalButton(
                                onClick = { showCatalogBrowser = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Explorar Catálogo Completo (550+ Itens)")
                            }
                        }
                    }
                }
            } else {
                groupedItems.forEach { (category, catItems) ->
                    val isCollapsed = collapsedCategories.contains(category.id)
                    val catCompleted = catItems.count { it.isCompleted }

                    // Interactive Collapsible Category Header with bilingual title & quick action
                    item(key = "header_${category.id}") {
                        CategoryCollapsibleHeader(
                            category = category,
                            isCollapsed = isCollapsed,
                            catCompleted = catCompleted,
                            totalCount = catItems.size,
                            onToggle = {
                                collapsedCategories = if (isCollapsed) {
                                    collapsedCategories - category.id
                                } else {
                                    collapsedCategories + category.id
                                }
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            },
                            onAddInCategory = {
                                quickAddInCategory = category
                            }
                        )
                    }

                    // Render items if category is expanded
                    if (!isCollapsed) {
                        items(catItems, key = { it.id }) { item ->
                            ItemRow(
                                item = item,
                                onToggle = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onToggleItem(item)
                                },
                                onDelete = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onDeleteItem(item.id)
                                },
                                onChangeCategory = {
                                    itemToChangeCategory = it
                                },
                                isSupermarketMode = false
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CategoryCollapsibleHeader(
    category: GroceryCategory,
    isCollapsed: Boolean,
    catCompleted: Int,
    totalCount: Int,
    onToggle: () -> Unit,
    onAddInCategory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isCollapsed) -90f else 0f,
        label = "chevron_rotation"
    )

    Surface(
        color = category.color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .testTag("category_header_${category.id}")
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = category.emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = category.color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = category.color.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${category.englishName} · Corredor ${category.aisleNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = category.color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (isCollapsed) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$totalCount iten${if (totalCount > 1) "s" else ""} • Toque para expandir",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quick add button in this category
                IconButton(
                    onClick = onAddInCategory,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("btn_add_in_category_${category.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar em ${category.name}",
                        tint = category.color,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (catCompleted == totalCount) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = "$catCompleted/$totalCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (catCompleted == totalCount) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isCollapsed) "Expandir categoria" else "Recolher categoria",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationAngle)
                )
            }
        }
    }
}
