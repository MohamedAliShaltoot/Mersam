package com.example.trello.presentation.boards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.trello.presentation.common.components.BoardEditorDialog
import com.example.trello.presentation.common.components.BoardListItem
import com.example.trello.presentation.navigation.SharedColor
import com.example.trello.presentation.ai.AiAssistantDialog
import com.example.trello.presentation.ai.AiAssistantViewModel
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardsListScreen(
    onBoardClick: (Long) -> Unit,
    viewModel: BoardsListViewModel = hiltViewModel(),
    aiViewModel: AiAssistantViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAiDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is BoardsListContract.Effect.NavigateToBoard -> onBoardClick(effect.boardId)
                is BoardsListContract.Effect.ShowUndoSnackbar -> scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.message,
                        actionLabel = "Undo",
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(BoardsListContract.Intent.OnUndoDeleteBoard)
                    }
                }
                is BoardsListContract.Effect.ShowAddedSnackbar -> scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.message,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(BoardsListContract.Intent.OnUndoDeleteBoard)
                    }
                }
                is BoardsListContract.Effect.ShowMessage -> scope.launch {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { 
            TopAppBar(
                title = { Text("Your Boards") },
                actions = {
                    Surface(
                        onClick = { showAiDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Mersam AI",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mersam AI",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            ) 
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                containerColor = SharedColor.sharedColor,
                text = { Text("New board" , color = Color.White) },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                onClick = { viewModel.onIntent(BoardsListContract.Intent.OnCreateBoardClicked) },
                shape = RoundedCornerShape(8.dp),
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onIntent(BoardsListContract.Intent.OnSearchQueryChanged(it)) },
                singleLine = true,
                placeholder = { Text("Search boards") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.isEmpty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.searchQuery.isBlank()) "No boards yet. Tap + to create one."
                        else "No boards match \"${state.searchQuery}\".",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.boards, key = { it.id }) { board ->
                        BoardListItem(
                            board = board,
                            onClick = { viewModel.onIntent(BoardsListContract.Intent.OnBoardClicked(board.id)) },
                            onToggleFavorite = { viewModel.onIntent(BoardsListContract.Intent.OnToggleFavorite(board)) },
                            onEdit = { viewModel.onIntent(BoardsListContract.Intent.OnEditBoardClicked(board)) },
                            onDelete = { viewModel.onIntent(BoardsListContract.Intent.OnDeleteBoard(board)) }
                        )
                    }
                }
            }
        }
    }

    if (state.showCreateDialog) {
        val board = state.boardToEdit
        BoardEditorDialog(
            initialTitle = board?.title ?: "",
            initialDescription = board?.description ?: "",
            initialColorHex = board?.colorHex ?: com.example.trello.presentation.theme.BoardColorPalette.first(),
            onDismiss = { viewModel.onIntent(BoardsListContract.Intent.OnDismissCreateDialog) },
            onConfirm = { title, description, colorHex ->
                if (board != null) {
                    viewModel.onIntent(BoardsListContract.Intent.OnEditBoardConfirmed(title, description, colorHex))
                } else {
                    viewModel.onIntent(BoardsListContract.Intent.OnCreateBoardConfirmed(title, description, colorHex))
                }
            }
        )
    }

    if (showAiDialog) {
        val aiState by aiViewModel.uiState.collectAsState()
        AiAssistantDialog(
            uiState = aiState,
            onDismissRequest = {
                showAiDialog = false
                aiViewModel.reset()
            },
            onSendMessage = { prompt ->
                aiViewModel.sendMessage(prompt)
            }
        )
    }
}
