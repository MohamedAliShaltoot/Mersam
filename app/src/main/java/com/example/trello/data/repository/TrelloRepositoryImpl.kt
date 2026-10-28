package com.example.trello.data.repository

import com.example.trello.data.local.dao.BoardDao
import com.example.trello.data.local.dao.CardDao
import com.example.trello.data.local.dao.CardListDao
import com.example.trello.data.mapper.toDomain
import com.example.trello.data.mapper.toEntity
import com.example.trello.di.IoDispatcher
import com.example.trello.domain.model.Board
import com.example.trello.domain.model.BoardList
import com.example.trello.domain.model.Card
import com.example.trello.domain.repository.TrelloRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.example.trello.data.local.dao.ChecklistItemDao
import com.example.trello.domain.model.ChecklistItem
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TrelloRepositoryImpl @Inject constructor(
    private val boardDao: BoardDao,
    private val listDao: CardListDao,
    private val cardDao: CardDao,
    private val checklistItemDao: ChecklistItemDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TrelloRepository {

    // Boards

    override fun observeBoards(query: String): Flow<List<Board>> =
        boardDao.getBoardsWithStats(query).map { rows -> rows.map { it.toDomain() } }

    override fun observeBoard(boardId: Long): Flow<Board?> =
        boardDao.getBoardById(boardId).map { it?.toDomain() }

    override suspend fun createBoard(title: String, description: String, colorHex: String): Long =
        withContext(ioDispatcher) {
            boardDao.insertBoard(
                Board(title = title, description = description, colorHex = colorHex).toEntity()
            )
        }

    override suspend fun updateBoard(board: Board) = withContext(ioDispatcher) {
        boardDao.updateBoard(board.toEntity())
    }

    override suspend fun deleteBoard(board: Board) = withContext(ioDispatcher) {
        boardDao.deleteBoard(board.toEntity())
    }

    override suspend fun restoreBoard(board: Board): Long = withContext(ioDispatcher) {
        // Re-insert with id = 0 so Room assigns a fresh primary key.
        boardDao.insertBoard(board.toEntity().copy(id = 0))
    }

    override suspend fun setBoardFavorite(boardId: Long, isFavorite: Boolean) =
        withContext(ioDispatcher) { boardDao.setFavorite(boardId, isFavorite) }

    // Lists

    override fun observeLists(boardId: Long): Flow<List<BoardList>> =
        listDao.getListsForBoard(boardId).flatMapLatest { lists ->
            if (lists.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(lists.map { list -> cardDao.getCardsForList(list.id) }) { cardArrays ->
                    lists.mapIndexed { index, list ->
                        list.toDomain(cardArrays[index].map { it.toDomain() })
                    }
                }
            }
        }

    override suspend fun createList(boardId: Long, title: String): Long = withContext(ioDispatcher) {
        val nextPosition = (listDao.getMaxPosition(boardId) ?: 0.0) + 1.0
        listDao.insertList(
            BoardList(boardId = boardId, title = title, position = nextPosition).toEntity()
        )
    }

    override suspend fun updateList(list: BoardList) = withContext(ioDispatcher) {
        listDao.updateList(list.toEntity())
    }

    override suspend fun renameList(list: BoardList, title: String) = withContext(ioDispatcher) {
        listDao.updateList(list.copy(title = title).toEntity())
    }

    override suspend fun deleteList(list: BoardList) = withContext(ioDispatcher) {
        listDao.deleteList(list.toEntity())
    }

    override suspend fun restoreList(list: BoardList): Long = withContext(ioDispatcher) {
        listDao.insertList(list.toEntity().copy(id = 0))
    }

    override suspend fun moveList(list: BoardList, targetListId: Long, isLeftHalf: Boolean) = withContext(ioDispatcher) {
        val lists = listDao.getListsForBoardSync(list.boardId).filter { it.id != list.id }
        val targetIndex = lists.indexOfFirst { it.id == targetListId }

        val newPosition = if (targetIndex != -1) {
            if (isLeftHalf) {
                val prevPos = if (targetIndex > 0) lists[targetIndex - 1].position else lists[targetIndex].position - 1.0
                (prevPos + lists[targetIndex].position) / 2.0
            } else {
                val nextPos = if (targetIndex < lists.lastIndex) lists[targetIndex + 1].position else lists[targetIndex].position + 1.0
                (lists[targetIndex].position + nextPos) / 2.0
            }
        } else {
            (listDao.getMaxPosition(list.boardId) ?: 0.0) + 1.0
        }
        
        listDao.updateList(list.copy(position = newPosition).toEntity())
    }

    // Cards

    override fun observeCard(cardId: Long): Flow<Card?> =
        cardDao.getCardById(cardId).map { it?.toDomain() }

    override suspend fun createCard(listId: Long, title: String): Long = withContext(ioDispatcher) {
        val nextPosition = (cardDao.getMaxPosition(listId) ?: 0.0) + 1.0
        cardDao.insertCard(Card(listId = listId, title = title, position = nextPosition).toEntity())
    }

    override suspend fun updateCard(card: Card) = withContext(ioDispatcher) {
        cardDao.updateCard(card.toEntity())
    }

    override suspend fun deleteCard(card: Card) = withContext(ioDispatcher) {
        cardDao.deleteCard(card.toEntity())
    }

    override suspend fun restoreCard(card: Card): Long = withContext(ioDispatcher) {
        cardDao.insertCard(card.toEntity().copy(id = 0))
    }

    override suspend fun setCardCompleted(cardId: Long, completed: Boolean) =
        withContext(ioDispatcher) { cardDao.setCompleted(cardId, completed) }

    override suspend fun moveCard(card: Card, targetListId: Long, targetCardId: Long?, isTopHalf: Boolean) = withContext(ioDispatcher) {
        if (targetCardId == null) {
            val nextPosition = (cardDao.getMaxPosition(targetListId) ?: 0.0) + 1.0
            cardDao.updateCard(card.copy(listId = targetListId, position = nextPosition).toEntity())
        } else {
            // Reordering cards within a list or dropping between cards
            val cards = cardDao.getCardsForListSync(targetListId).filter { it.id != card.id } // exclude self
            val targetIndex = cards.indexOfFirst { it.id == targetCardId }
            
            val newPosition = if (targetIndex != -1) {
                if (isTopHalf) {
                    val prevPos = if (targetIndex > 0) cards[targetIndex - 1].position else cards[targetIndex].position - 1.0
                    (prevPos + cards[targetIndex].position) / 2.0
                } else {
                    val nextPos = if (targetIndex < cards.lastIndex) cards[targetIndex + 1].position else cards[targetIndex].position + 1.0
                    (cards[targetIndex].position + nextPos) / 2.0
                }
            } else {
                (cardDao.getMaxPosition(targetListId) ?: 0.0) + 1.0
            }
            cardDao.updateCard(card.copy(listId = targetListId, position = newPosition).toEntity())
        }
    }

    // Checklist Items

    override fun observeChecklist(cardId: Long): Flow<List<ChecklistItem>> =
        checklistItemDao.getChecklistForCard(cardId).map { items -> items.map { it.toDomain() } }

    override suspend fun addChecklistItem(cardId: Long, title: String): Long = withContext(ioDispatcher) {
        val nextPosition = (checklistItemDao.getMaxPosition(cardId) ?: 0.0) + 1.0
        checklistItemDao.insertChecklistItem(
            ChecklistItem(cardId = cardId, title = title, position = nextPosition).toEntity()
        )
    }

    override suspend fun updateChecklistItem(item: ChecklistItem) = withContext(ioDispatcher) {
        checklistItemDao.updateChecklistItem(item.toEntity())
    }

    override suspend fun toggleChecklistItem(itemId: Long, isCompleted: Boolean) = withContext(ioDispatcher) {
        checklistItemDao.setCompleted(itemId, completed = isCompleted)
    }

    override suspend fun deleteChecklistItem(itemId: Long) = withContext(ioDispatcher) {
        checklistItemDao.deleteChecklistItemById(itemId)
    }
}
