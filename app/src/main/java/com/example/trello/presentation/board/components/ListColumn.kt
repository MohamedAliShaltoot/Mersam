package com.example.trello.presentation.board.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.trello.domain.model.BoardList
import com.example.trello.domain.model.Card as TrelloCard
import com.example.trello.presentation.common.components.ConfirmDialog
import com.example.trello.presentation.board.components.draganddrop.LocalDragDropState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.positionInWindow

@Composable
fun ListColumn(
    list: BoardList,
    accentColor: Color,
    onAddCardClick: () -> Unit,
    onCardClick: (TrelloCard) -> Unit,
    onToggleComplete: (TrelloCard) -> Unit,
    onDeleteCard: (TrelloCard) -> Unit,
    onMoveCard: (TrelloCard, Long, Long?, Boolean) -> Unit,
    onMoveList: (BoardList, Long, Boolean) -> Unit,
    onRenameList: () -> Unit,
    onDeleteList: () -> Unit,
    modifier: Modifier = Modifier,
    isOverlay: Boolean = false
) {
    var showDeleteListDialog by remember { mutableStateOf(false) }
    val dragDropState = LocalDragDropState.current
    var listBounds by remember { mutableStateOf(Rect.Zero) }
    val listState = rememberLazyListState()
    var globalPosition by remember { mutableStateOf(Offset.Zero) }

    val isDragged = !isOverlay && dragDropState.isDragging && dragDropState.draggedList?.id == list.id

    LaunchedEffect(dragDropState.isDragging) {
        while (dragDropState.isDragging) {
            val dropPos = dragDropState.cardPosition + dragDropState.dragPosition + dragDropState.dragOffset
            if (listBounds.contains(dropPos)) {
                val currentY = dropPos.y
                val topEdgeThreshold = listBounds.top + 150f
                val bottomEdgeThreshold = listBounds.bottom - 150f

                var scrollAmount = 0f
                if (currentY > bottomEdgeThreshold) {
                    scrollAmount = (currentY - bottomEdgeThreshold) * 0.15f
                } else if (currentY < topEdgeThreshold) {
                    scrollAmount = (currentY - topEdgeThreshold) * 0.15f
                }

                if (scrollAmount != 0f) {
                    listState.scrollBy(scrollAmount)
                }
            }
            delay(16)
        }
    }

    Card(
        modifier = modifier
            .width(272.dp)
            .onGloballyPositioned { coordinates ->
                globalPosition = coordinates.positionInWindow()
                if (!isOverlay) {
                    listBounds = coordinates.boundsInWindow()
                    dragDropState.registerListBounds(list.id, listBounds)
                }
            }
            .alpha(if (isDragged) 0f else 1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {

            Row(modifier = Modifier
                .fillMaxWidth()
                .pointerInput(list) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            dragDropState.onListDragStart(offset, list, globalPosition)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDropState.onDrag(dragAmount)
                        },
                        onDragEnd = {
                            val dropPos = dragDropState.cardPosition + dragDropState.dragPosition + dragDropState.dragOffset
                            val targetList = dragDropState.findTargetList(dropPos)
                            if (targetList != null && targetList.first != list.id) {
                                onMoveList(list, targetList.first, targetList.second)
                            }
                            dragDropState.onDragInterrupt()
                        },
                        onDragCancel = {
                            dragDropState.onDragInterrupt()
                        }
                    )
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(accentColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = list.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onRenameList() }
                        .padding(vertical = 4.dp)
                )
                Text(
                    text = "${list.completedCount}/${list.cardCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                        .clickable { showDeleteListDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete list",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(list.cards, key = { it.id }) { card ->
                    CardItem(
                        card = card,
                        onClick = { onCardClick(card) },
                        onToggleComplete = { onToggleComplete(card) },
                        onDelete = { onDeleteCard(card) },
                        onMove = { onMoveCard(card, -1, null, false) }, // -1 for dialog move, not used by drag drop
                        onDrop = { targetListId, targetCardId, isTopHalf -> 
                            onMoveCard(card, targetListId, targetCardId, isTopHalf) 
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onAddCardClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add card")
            }
        }
    }

    if (showDeleteListDialog) {
        ConfirmDialog(
            title = "Delete list?",
            text = "\"${list.title}\" and all its cards will be deleted.",
            onDismiss = { showDeleteListDialog = false },
            onConfirm = onDeleteList
        )
    }
}