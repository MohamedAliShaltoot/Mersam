package com.example.trello.presentation.carddetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.unit.dp
import com.example.trello.presentation.common.components.ConfirmDialog
import com.example.trello.presentation.common.components.DueDatePickerDialog
import com.example.trello.presentation.common.components.PrioritySelector
import com.example.trello.presentation.common.components.formatDueDate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: CardDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                CardDetailContract.Effect.NavigateBack -> onNavigateBack()
                is CardDetailContract.Effect.ShowMessage -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onIntent(CardDetailContract.Intent.OnDeleteClicked) },
                        enabled = state.card != null
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete card", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (state.card == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("This card no longer exists.")
            }
            return@Scaffold
        }

        val scrollState = rememberScrollState()
        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.onIntent(CardDetailContract.Intent.OnToggleCompleted) }) {
                    Icon(
                        imageVector = if (state.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = if (state.isCompleted) "Mark incomplete" else "Mark complete",
                        tint = if (state.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (state.isCompleted) "Completed" else "Mark as complete?",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onIntent(CardDetailContract.Intent.OnTitleChanged(it)) },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onIntent(CardDetailContract.Intent.OnDescriptionChanged(it)) },
                label = { Text("Description") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Priority", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            PrioritySelector(
                selected = state.priority,
                onSelect = { viewModel.onIntent(CardDetailContract.Intent.OnPriorityChanged(it)) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Labels", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            com.example.trello.presentation.common.components.LabelSelector(
                selectedLabels = state.labels,
                onToggleLabel = { viewModel.onIntent(CardDetailContract.Intent.OnToggleLabel(it)) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Cover", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            com.example.trello.presentation.common.components.CoverColorSelector(
                selectedColor = state.coverColor,
                onSelectColor = { viewModel.onIntent(CardDetailContract.Intent.OnCoverColorSelected(it)) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Due date", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { viewModel.onIntent(CardDetailContract.Intent.OnDueDateClicked) },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text(state.dueDate?.let { formatDueDate(it) } ?: "Set due date") }
                )
                if (state.dueDate != null) {
                    IconButton(onClick = { viewModel.onIntent(CardDetailContract.Intent.OnClearDueDate) }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear due date")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            com.example.trello.presentation.common.components.ChecklistSection(
                items = state.checklist,
                onToggleItem = { itemId, isCompleted ->
                    viewModel.onIntent(CardDetailContract.Intent.OnToggleChecklistItem(itemId, isCompleted))
                },
                onUpdateItem = { itemId, newTitle ->
                    viewModel.onIntent(CardDetailContract.Intent.OnUpdateChecklistItem(itemId, newTitle))
                },
                onDeleteItem = { itemId ->
                    viewModel.onIntent(CardDetailContract.Intent.OnDeleteChecklistItem(itemId))
                },
                onAddItem = { title ->
                    viewModel.onIntent(CardDetailContract.Intent.OnAddChecklistItem(title))
                },
                onStartAdding = {
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(120)
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onNavigateBack, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text("Cancel")
                }
                Button(
                    onClick = { viewModel.onIntent(CardDetailContract.Intent.OnSaveClicked) },
                    enabled = state.canSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save changes")
                }
            }
        }
    }

    if (state.showDatePicker) {
        DueDatePickerDialog(
            initialMillis = state.dueDate,
            onDismiss = { viewModel.onIntent(CardDetailContract.Intent.OnDismissDatePicker) },
            onConfirm = { millis -> viewModel.onIntent(CardDetailContract.Intent.OnDueDateSelected(millis)) }
        )
    }

    if (state.showDeleteDialog) {
        ConfirmDialog(
            title = "Delete card?",
            text = "This cannot be undone.",
            onDismiss = { viewModel.onIntent(CardDetailContract.Intent.OnDismissDeleteDialog) },
            onConfirm = { viewModel.onIntent(CardDetailContract.Intent.OnConfirmDelete) }
        )
    }
}
