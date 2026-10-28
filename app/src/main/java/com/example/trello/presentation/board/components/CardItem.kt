package com.example.trello.presentation.board.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.example.trello.domain.model.Card as TrelloCard
import com.example.trello.domain.model.Priority
import com.example.trello.presentation.common.components.DueDateChip
import com.example.trello.presentation.common.components.PriorityChip
import com.example.trello.presentation.common.components.toComposeColor
import com.example.trello.presentation.board.components.draganddrop.LocalDragDropState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CardItem(
    card: TrelloCard,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onDrop: (targetListId: Long, targetCardId: Long?, isTopHalf: Boolean) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    isOverlay: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var globalPosition by remember { mutableStateOf(Offset.Zero) }
    val dragDropState = LocalDragDropState.current
    val isDragged = !isOverlay && dragDropState.isDragging && dragDropState.draggedCard?.id == card.id

    androidx.compose.runtime.DisposableEffect(card.id) {
        onDispose { dragDropState.unregisterCard(card.id) }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(color, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete card",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    ) {
        ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                globalPosition = coordinates.positionInWindow()
                if (!isOverlay) {
                    dragDropState.registerCardBounds(card.id, card.listId, coordinates.boundsInWindow())
                }
            }
            .alpha(if (isDragged) 0f else 1f)
            .pointerInput(card) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        dragDropState.onDragStart(offset, card, globalPosition)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDropState.onDrag(dragAmount)
                    },
                    onDragEnd = {
                        val dropPos = dragDropState.cardPosition + dragDropState.dragPosition + dragDropState.dragOffset
                        
                        val targetCard = dragDropState.findTargetCard(dropPos)
                        if (targetCard != null && targetCard.second != card.id) {
                            onDrop(targetCard.first, targetCard.second, targetCard.third)
                        } else {
                            val targetListId = dragDropState.findTargetList(dropPos)?.first
                            if (targetListId != null) {
                                onDrop(targetListId, null, false)
                            }
                        }
                        
                        dragDropState.onDragInterrupt()
                    },
                    onDragCancel = {
                        dragDropState.onDragInterrupt()
                    }
                )
            }
    ) {
        Column {
            if (card.coverColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(card.coverColor.toComposeColor())
                )
            }
            Column(modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)) {
                if (card.labels.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(start = 34.dp, top = 2.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    card.labels.forEach { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(width = 32.dp, height = 8.dp)
                                .background(
                                    color = colorHex.toComposeColor(),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleComplete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (card.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = if (card.isCompleted) "Mark incomplete?" else "Mark complete 👍",
                        tint = if (card.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = card.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (card.isCompleted) TextDecoration.LineThrough else null,
                    color = if (card.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "Card actions", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu( expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 8.dp,
                        shadowElevation = 12.dp,
                        containerColor = MaterialTheme.colorScheme.surface) {

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Move to list",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onMove()
                            }
                        )
                        HorizontalDivider()

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            if (card.priority != Priority.NONE || card.dueDate != null || card.hasChecklist) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 34.dp, top = 4.dp, end = 4.dp)
                ) {
                    if (card.priority != Priority.NONE) {
                        PriorityChip(card.priority)
                    }
                    card.dueDate?.let { due ->
                        DueDateChip(dueDateMillis = due, isOverdue = card.isOverdue)
                    }
                    if (card.hasChecklist) {
                        com.example.trello.presentation.common.components.ChecklistBadge(
                            completed = card.checklistCompletedCount,
                            total = card.checklistTotalCount
                        )
                    }
                }
            }
            }
        }
    }
}}