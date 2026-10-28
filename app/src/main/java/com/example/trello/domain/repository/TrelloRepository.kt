package com.example.trello.domain.repository

import com.example.trello.domain.model.Board
import com.example.trello.domain.model.BoardList
import com.example.trello.domain.model.Card
import com.example.trello.domain.model.ChecklistItem
import kotlinx.coroutines.flow.Flow


interface TrelloRepository {

    // Boards
    fun observeBoards(query: String = ""): Flow<List<Board>>
    fun observeBoard(boardId: Long): Flow<Board?>
    suspend fun createBoard(title: String, description: String = "", colorHex: String): Long
    suspend fun updateBoard(board: Board)
    suspend fun deleteBoard(board: Board)
    suspend fun restoreBoard(board: Board): Long
    suspend fun setBoardFavorite(boardId: Long, isFavorite: Boolean)

    // Lists
    fun observeLists(boardId: Long): Flow<List<BoardList>>
    suspend fun createList(boardId: Long, title: String): Long
    suspend fun updateList(list: BoardList)
    suspend fun renameList(list: BoardList, title: String)
    suspend fun deleteList(list: BoardList)
    suspend fun restoreList(list: BoardList): Long
    suspend fun moveList(list: BoardList, targetListId: Long, isLeftHalf: Boolean)

    // Cards
    fun observeCard(cardId: Long): Flow<Card?>
    suspend fun createCard(listId: Long, title: String): Long
    suspend fun updateCard(card: Card)
    suspend fun deleteCard(card: Card)
    suspend fun restoreCard(card: Card): Long
    suspend fun setCardCompleted(cardId: Long, completed: Boolean)
    suspend fun moveCard(card: Card, targetListId: Long, targetCardId: Long? = null, isTopHalf: Boolean = false)

    // Checklist Items
    fun observeChecklist(cardId: Long): Flow<List<ChecklistItem>>
    suspend fun addChecklistItem(cardId: Long, title: String): Long
    suspend fun updateChecklistItem(item: ChecklistItem)
    suspend fun toggleChecklistItem(itemId: Long, isCompleted: Boolean)
    suspend fun deleteChecklistItem(itemId: Long)
}
