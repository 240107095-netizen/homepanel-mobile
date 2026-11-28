package com.example.homepanel.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Файл базы homepanel.db лежит в данных приложения и переживает перезапуск.
// version — номер схемы: поменяли таблицы — увеличиваем и пишем миграцию.
@Database(entities = [SavedTitle::class], version = 1, exportSchema = false)
abstract class HomepanelDatabase : RoomDatabase() {

    abstract fun savedTitleDao(): SavedTitleDao

    companion object {
        @Volatile
        private var instance: HomepanelDatabase? = null

        // Одна база на всё приложение
        fun get(context: Context): HomepanelDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, HomepanelDatabase::class.java, "homepanel.db")
                .build()
                .also { instance = it }
        }
    }
}
