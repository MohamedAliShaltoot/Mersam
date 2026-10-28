package com.example.trello.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.trello.data.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

data class CardWithStats(
    val id: Long,
    val listId: Long,
    val title: String,
    val description: String,
    val position: Double,
    val priority: String,
    val dueDate: Long?,
    val isCompleted: Boolean,
    val labels: String,
    val coverColor: String?,
    val createdAt: Long,
    val checklistTotalCount: Int = 0,
    val checklistCompletedCount: Int = 0
)

@Dao
interface CardDao {

    @Query(
        """
        SELECT
            c.id AS id,
            c.listId AS listId,
            c.title AS title,
            c.description AS description,
            c.position AS position,
            c.priority AS priority,
            c.dueDate AS dueDate,
            c.isCompleted AS isCompleted,
            c.labels AS labels,
            c.coverColor AS coverColor,
            c.createdAt AS createdAt,
            (SELECT COUNT(*) FROM checklist_items ci WHERE ci.cardId = c.id) AS checklistTotalCount,
            (SELECT COUNT(*) FROM checklist_items ci2 WHERE ci2.cardId = c.id AND ci2.isCompleted = 1) AS checklistCompletedCount
        FROM cards c
        WHERE c.listId = :listId
        ORDER BY c.position ASC
        """
    )
    fun getCardsForList(listId: Long): Flow<List<CardWithStats>>

    @Query("SELECT * FROM cards WHERE listId = :listId ORDER BY position ASC")
    suspend fun getCardsForListSync(listId: Long): List<CardEntity>

    @Query(
        """
        SELECT
            c.id AS id,
            c.listId AS listId,
            c.title AS title,
            c.description AS description,
            c.position AS position,
            c.priority AS priority,
            c.dueDate AS dueDate,
            c.isCompleted AS isCompleted,
            c.labels AS labels,
            c.coverColor AS coverColor,
            c.createdAt AS createdAt,
            (SELECT COUNT(*) FROM checklist_items ci WHERE ci.cardId = c.id) AS checklistTotalCount,
            (SELECT COUNT(*) FROM checklist_items ci2 WHERE ci2.cardId = c.id AND ci2.isCompleted = 1) AS checklistCompletedCount
        FROM cards c
        WHERE c.id = :cardId
        """
    )
    fun getCardById(cardId: Long): Flow<CardWithStats?>

    @Insert
    suspend fun insertCard(card: CardEntity): Long

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("SELECT MAX(position) FROM cards WHERE listId = :listId")
    suspend fun getMaxPosition(listId: Long): Double?

    @Query("UPDATE cards SET isCompleted = :completed WHERE id = :cardId")
    suspend fun setCompleted(cardId: Long, completed: Boolean)
}
