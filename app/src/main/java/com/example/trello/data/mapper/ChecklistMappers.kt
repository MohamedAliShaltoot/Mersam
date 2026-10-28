package com.example.trello.data.mapper

import com.example.trello.data.local.entity.ChecklistItemEntity
import com.example.trello.domain.model.ChecklistItem

fun ChecklistItemEntity.toDomain(): ChecklistItem = ChecklistItem(
    id = id,
    cardId = cardId,
    title = title,
    isCompleted = isCompleted,
    position = position
)

fun ChecklistItem.toEntity(): ChecklistItemEntity = ChecklistItemEntity(
    id = id,
    cardId = cardId,
    title = title,
    isCompleted = isCompleted,
    position = position
)
