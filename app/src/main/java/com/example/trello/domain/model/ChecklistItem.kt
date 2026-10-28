package com.example.trello.domain.model

data class ChecklistItem(
    val id: Long = 0,
    val cardId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val position: Double = 0.0
)
