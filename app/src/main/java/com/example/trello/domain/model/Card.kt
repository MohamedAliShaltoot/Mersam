package com.example.trello.domain.model

data class Card(
    val id: Long = 0,
    val listId: Long,
    val title: String,
    val description: String = "",
    val position: Double,
    val priority: Priority = Priority.NONE,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val labels: List<String> = emptyList(),
    val coverColor: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val checklistTotalCount: Int = 0,
    val checklistCompletedCount: Int = 0
) {
    val isOverdue: Boolean
        get() = dueDate != null && !isCompleted && dueDate < System.currentTimeMillis()

    val hasChecklist: Boolean
        get() = checklistTotalCount > 0

    val isChecklistComplete: Boolean
        get() = checklistTotalCount > 0 && checklistCompletedCount == checklistTotalCount
}
