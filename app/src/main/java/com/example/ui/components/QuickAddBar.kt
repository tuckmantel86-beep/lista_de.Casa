package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogItem
import com.example.data.model.Categories

@Composable
fun QuickAddBar(
    query: String,
    suggestions: List<CatalogItem>,
    onQueryChange: (String) -> Unit,
    onAddItem: (name: String, categoryName: String?, quantity: String) -> Unit,
    onOpenDetailedAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detectedCategory = remember(query) {
        if (query.isNotBlank()) Categories.detectCategory(query) else null
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Text input bar with detected category pill and add button (compact top version)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Adicionar item (ex: Banana, Leite...)",
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("quick_add_input"),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (query.isNotBlank()) {
                        onAddItem(query.trim(), null, "1")
                        onQueryChange("")
                    }
                }),
                trailingIcon = {
                    if (detectedCategory != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = detectedCategory.color.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = detectedCategory.emoji, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = detectedCategory.name.split(" ").first(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = detectedCategory.color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Add / Detailed button
            FilledIconButton(
                onClick = {
                    if (query.isNotBlank()) {
                        onAddItem(query.trim(), null, "1")
                        onQueryChange("")
                    } else {
                        onOpenDetailedAdd()
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("quick_add_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (query.isNotBlank()) Icons.Default.Add else Icons.Default.Tune,
                    contentDescription = if (query.isNotBlank()) "Adicionar Item" else "Opções detalhadas",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Autocomplete suggestions row (only appears when typing)
        AnimatedVisibility(visible = suggestions.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier.clickable {
                                onAddItem(item.name, item.categoryId, item.defaultQuantity)
                                onQueryChange("")
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = item.emoji, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
