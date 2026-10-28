package com.example.trello.data.mapper

import com.example.trello.data.local.entity.CardEntity
import com.example.trello.domain.model.Card
import com.example.trello.domain.model.Priority

fun CardEntity.toDomain(): Card = Card(
    id = id,
    listId = listId,
    title = title,
    description = description,
    position = position,
    priority = Priority.fromNameOrDefault(priority),
    dueDate = dueDate,
    isCompleted = isCompleted,
    labels = if (labels.isBlank()) emptyList() else labels.split(","),
    coverColor = coverColor,
    createdAt = createdAt
)

fun com.example.trello.data.local.dao.CardWithStats.toDomain(): Card = Card(
    id = id,
    listId = listId,
    title = title,
    description = description,
    position = position,
    priority = Priority.fromNameOrDefault(priority),
    dueDate = dueDate,
    isCompleted = isCompleted,
    labels = if (labels.isBlank()) emptyList() else labels.split(","),
    coverColor = coverColor,
    createdAt = createdAt,
    checklistTotalCount = checklistTotalCount,
    checklistCompletedCount = checklistCompletedCount
)

fun Card.toEntity(): CardEntity = CardEntity(
    id = id,
    listId = listId,
    title = title,
    description = description,
    position = position,
    priority = priority.name,
    dueDate = dueDate,
    isCompleted = isCompleted,
    labels = labels.joinToString(","),
    coverColor = coverColor,
    createdAt = createdAt
)