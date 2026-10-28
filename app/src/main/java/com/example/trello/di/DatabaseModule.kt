package com.example.trello.di

import android.content.Context
import androidx.room.Room
import com.example.trello.data.local.TrelloDatabase
import com.example.trello.data.local.dao.BoardDao
import com.example.trello.data.local.dao.CardDao
import com.example.trello.data.local.dao.CardListDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TrelloDatabase =
        Room.databaseBuilder(context, TrelloDatabase::class.java, "trello.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideBoardDao(database: TrelloDatabase): BoardDao = database.boardDao()

    @Provides
    fun provideCardListDao(database: TrelloDatabase): CardListDao = database.cardListDao()

    @Provides
    fun provideCardDao(database: TrelloDatabase): CardDao = database.cardDao()

    @Provides
    fun provideChecklistItemDao(database: TrelloDatabase): com.example.trello.data.local.dao.ChecklistItemDao =
        database.checklistItemDao()
}
