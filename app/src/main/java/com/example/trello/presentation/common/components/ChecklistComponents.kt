package com.example.trello.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.trello.domain.model.ChecklistItem

/**
 * Compact, modern badge for card items on the Kanban board.
 * Perfectly calibrated to align with PriorityChip and DueDateChip.
 */
@Composable
fun ChecklistBadge(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    if (total == 0) return

    val isAllComplete = completed == total
    val bgColor = if (isAllComplete) {
        Color(0xFFE8F5E9)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isAllComplete) {
        Color(0xFF2E7D32)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isAllComplete) Icons.Default.CheckBox else Icons.Default.Checklist,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .size(13.dp)
                .padding(end = 4.dp)
        )
        Text(
            text = "$completed/$total",
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

/**
 * Full interactive Checklist section for CardDetailScreen.
 * Supports viewing progress, checking/unchecking, in-place title editing, deleting, and adding items.
 */
@Composable
fun ChecklistSection(
    items: List<ChecklistItem>,
    onToggleItem: (itemId: Long, isCompleted: Boolean) -> Unit,
    onUpdateItem: (itemId: Long, newTitle: String) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    onAddItem: (title: String) -> Unit,
    onStartAdding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isAddingItem by remember { mutableStateOf(false) }
    var newItemTitle by remember { mutableStateOf("") }
    var editingItemId by remember { mutableStateOf<Long?>(null) }
    var editingItemText by remember { mutableStateOf("") }

    val total = items.size
    val completed = items.count { it.isCompleted }
    val progress = if (total == 0) 0f else completed / total.toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Checklist",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (total > 0) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (completed == total) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
            }
        }

        if (total > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (completed == total) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // List of items
        items.forEach { item ->
            val isEditing = editingItemId == item.id

            if (isEditing) {
                // In-line Edit Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editingItemText,
                        onValueChange = { editingItemText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (editingItemText.isNotBlank()) {
                                    onUpdateItem(item.id, editingItemText.trim())
                                }
                                editingItemId = null
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (editingItemText.isNotBlank()) {
                                onUpdateItem(item.id, editingItemText.trim())
                            }
                            editingItemId = null
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { editingItemId = null },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Normal Row View
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleItem(item.id, !item.isCompleted) }
                        .padding(vertical = 3.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isCompleted,
                        onCheckedChange = { onToggleItem(item.id, it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                        color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            editingItemId = item.id
                            editingItemText = item.title
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit item",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDeleteItem(item.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete subtask",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Add item input or button
        Spacer(modifier = Modifier.height(6.dp))
        if (isAddingItem) {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newItemTitle,
                    onValueChange = { newItemTitle = it },
                    placeholder = { Text("Add an item...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newItemTitle.isNotBlank()) {
                                onAddItem(newItemTitle.trim())
                                newItemTitle = ""
                                onStartAdding()
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (newItemTitle.isNotBlank()) {
                                onAddItem(newItemTitle.trim())
                                newItemTitle = ""
                                onStartAdding()
                            }
                        },
                        enabled = newItemTitle.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add")
                    }
                    OutlinedButton(
                        onClick = {
                            isAddingItem = false
                            newItemTitle = ""
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        } else {
            TextButton(
                onClick = {
                    isAddingItem = true
                    onStartAdding()
                },
                modifier = Modifier.padding(start = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (total == 0) "Add a checklist item" else "Add an item")
            }
        }
    }
}
