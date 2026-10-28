package com.example.trello.presentation.carddetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trello.domain.model.Card
import com.example.trello.domain.repository.TrelloRepository
import com.example.trello.presentation.carddetail.CardDetailContract.Effect
import com.example.trello.presentation.carddetail.CardDetailContract.Intent
import com.example.trello.presentation.carddetail.CardDetailContract.State
import com.example.trello.presentation.navigation.NavDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val repository: TrelloRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Long = checkNotNull(savedStateHandle[NavDestination.CardDetail.ARG_CARD_ID])

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var hasHydratedDraft = false

    init {
        repository.observeCard(cardId)
            .onEach { card ->
                _state.update { current ->
                    if (!hasHydratedDraft && card != null) {
                        hasHydratedDraft = true
                        current.copy(
                            card = card,
                            isLoading = false,
                            title = card.title,
                            description = card.description,
                            priority = card.priority,
                            dueDate = card.dueDate,
                            isCompleted = card.isCompleted,
                            labels = card.labels,
                            coverColor = card.coverColor
                        )
                    } else {
                        current.copy(
                            card = card,
                            isLoading = false,
                            isCompleted = card?.isCompleted ?: current.isCompleted
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

        repository.observeChecklist(cardId)
            .onEach { items ->
                _state.update { it.copy(checklist = items) }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.OnTitleChanged ->
                _state.update { it.copy(title = intent.value, isDirty = true) }

            is Intent.OnDescriptionChanged ->
                _state.update { it.copy(description = intent.value, isDirty = true) }

            is Intent.OnPriorityChanged -> {
                _state.update { it.copy(priority = intent.priority) }
                persistPatch { it.copy(priority = intent.priority) }
            }

            Intent.OnDueDateClicked -> _state.update { it.copy(showDatePicker = true) }

            is Intent.OnDueDateSelected -> {
                _state.update { it.copy(dueDate = intent.millis, showDatePicker = false) }
                persistPatch { it.copy(dueDate = intent.millis) }
            }

            Intent.OnDismissDatePicker -> _state.update { it.copy(showDatePicker = false) }

            Intent.OnClearDueDate -> {
                _state.update { it.copy(dueDate = null) }
                persistPatch { it.copy(dueDate = null) }
            }

            Intent.OnToggleCompleted -> {
                val current = _state.value.card ?: return
                viewModelScope.launch { repository.setCardCompleted(current.id, !current.isCompleted) }
            }

            is Intent.OnToggleLabel -> {
                val currentLabels = _state.value.labels.toMutableList()
                if (currentLabels.contains(intent.colorHex)) {
                    currentLabels.remove(intent.colorHex)
                } else {
                    currentLabels.add(intent.colorHex)
                }
                _state.update { it.copy(labels = currentLabels) }
                persistPatch { it.copy(labels = currentLabels) }
            }

            is Intent.OnCoverColorSelected -> {
                _state.update { it.copy(coverColor = intent.colorHex) }
                persistPatch { it.copy(coverColor = intent.colorHex) }
            }

            is Intent.OnAddChecklistItem -> {
                if (intent.title.isNotBlank()) {
                    viewModelScope.launch {
                        repository.addChecklistItem(cardId, intent.title.trim())
                    }
                }
            }

            is Intent.OnUpdateChecklistItem -> {
                if (intent.title.isNotBlank()) {
                    viewModelScope.launch {
                        val currentItem = _state.value.checklist.find { it.id == intent.itemId }
                        if (currentItem != null) {
                            repository.updateChecklistItem(
                                currentItem.copy(title = intent.title.trim())
                            )
                        }
                    }
                }
            }

            is Intent.OnToggleChecklistItem -> {
                viewModelScope.launch {
                    repository.toggleChecklistItem(intent.itemId, intent.isCompleted)
                }
            }

            is Intent.OnDeleteChecklistItem -> {
                viewModelScope.launch {
                    repository.deleteChecklistItem(intent.itemId)
                }
            }

            Intent.OnSaveClicked -> {
                val snapshot = _state.value
                val current = snapshot.card ?: return
                if (!snapshot.canSave) return
                viewModelScope.launch {
                    repository.updateCard(
                        current.copy(
                            title = snapshot.title.trim(),
                            description = snapshot.description.trim(),
                            priority = snapshot.priority,
                            dueDate = snapshot.dueDate,
                            labels = snapshot.labels,
                            coverColor = snapshot.coverColor
                        )
                    )
                    _effect.send(Effect.NavigateBack)
                }
            }

            Intent.OnDeleteClicked -> _state.update { it.copy(showDeleteDialog = true) }

            Intent.OnDismissDeleteDialog -> _state.update { it.copy(showDeleteDialog = false) }

            Intent.OnConfirmDelete -> {
                val current = _state.value.card ?: return
                viewModelScope.launch {
                    repository.deleteCard(current)
                    _effect.send(Effect.NavigateBack)
                }
            }
        }
    }

    private fun persistPatch(transform: (Card) -> Card) {
        val current = _state.value.card ?: return
        viewModelScope.launch { repository.updateCard(transform(current)) }
    }
}
