package com.example.trello.presentation.carddetail

import com.example.trello.domain.model.Card
import com.example.trello.domain.model.ChecklistItem
import com.example.trello.domain.model.Priority

object CardDetailContract {

    data class State(
        val card: Card? = null,
        val isLoading: Boolean = true,
        val title: String = "",
        val description: String = "",
        val priority: Priority = Priority.NONE,
        val dueDate: Long? = null,
        val isCompleted: Boolean = false,
        val labels: List<String> = emptyList(),
        val coverColor: String? = null,
        val checklist: List<ChecklistItem> = emptyList(),
        val showDeleteDialog: Boolean = false,
        val showDatePicker: Boolean = false,
        val isDirty: Boolean = false
    ) {
        val canSave: Boolean get() = title.isNotBlank()
    }

    sealed interface Intent {
        data class OnTitleChanged(val value: String) : Intent
        data class OnDescriptionChanged(val value: String) : Intent
        data class OnPriorityChanged(val priority: Priority) : Intent
        data object OnDueDateClicked : Intent
        data class OnDueDateSelected(val millis: Long?) : Intent
        data object OnDismissDatePicker : Intent
        data object OnClearDueDate : Intent
        data object OnToggleCompleted : Intent
        data class OnToggleLabel(val colorHex: String) : Intent
        data class OnCoverColorSelected(val colorHex: String?) : Intent
        data class OnAddChecklistItem(val title: String) : Intent
        data class OnUpdateChecklistItem(val itemId: Long, val title: String) : Intent
        data class OnToggleChecklistItem(val itemId: Long, val isCompleted: Boolean) : Intent
        data class OnDeleteChecklistItem(val itemId: Long) : Intent
        data object OnSaveClicked : Intent
        data object OnDeleteClicked : Intent
        data object OnDismissDeleteDialog : Intent
        data object OnConfirmDelete : Intent
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data class ShowMessage(val message: String) : Effect
    }
}
