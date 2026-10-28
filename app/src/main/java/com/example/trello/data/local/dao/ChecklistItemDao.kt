package com.example.trello.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.trello.data.local.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {

    @Query("SELECT * FROM checklist_items WHERE cardId = :cardId ORDER BY position ASC, id ASC")
    fun getChecklistForCard(cardId: Long): Flow<List<ChecklistItemEntity>>

    @Insert
    suspend fun insertChecklistItem(item: ChecklistItemEntity): Long

    @Update
    suspend fun updateChecklistItem(item: ChecklistItemEntity)

    @Delete
    suspend fun deleteChecklistItem(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteChecklistItemById(id: Long)

    @Query("UPDATE checklist_items SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("SELECT MAX(position) FROM checklist_items WHERE cardId = :cardId")
    suspend fun getMaxPosition(cardId: Long): Double?
}
