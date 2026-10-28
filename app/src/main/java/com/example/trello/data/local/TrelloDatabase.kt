package com.example.trello.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.trello.data.local.dao.BoardDao
import com.example.trello.data.local.dao.CardDao
import com.example.trello.data.local.dao.CardListDao
import com.example.trello.data.local.dao.ChecklistItemDao
import com.example.trello.data.local.entity.BoardEntity
import com.example.trello.data.local.entity.CardEntity
import com.example.trello.data.local.entity.CardListEntity
import com.example.trello.data.local.entity.ChecklistItemEntity

@Database(
    entities = [BoardEntity::class, CardListEntity::class, CardEntity::class, ChecklistItemEntity::class],
    version = 5,
    exportSchema = false
)
abstract class TrelloDatabase : RoomDatabase() {
    abstract fun boardDao(): BoardDao
    abstract fun cardListDao(): CardListDao
    abstract fun cardDao(): CardDao
    abstract fun checklistItemDao(): ChecklistItemDao
}
