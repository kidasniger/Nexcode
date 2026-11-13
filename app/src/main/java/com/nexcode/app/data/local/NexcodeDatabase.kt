package com.nexcode.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nexcode.app.data.local.dao.BuildDao
import com.nexcode.app.data.local.dao.ConversationDao
import com.nexcode.app.data.local.dao.FileDao
import com.nexcode.app.data.local.dao.ProjectDao
import com.nexcode.app.data.local.entity.BuildEntity
import com.nexcode.app.data.local.entity.ConversationEntity
import com.nexcode.app.data.local.entity.FileEntity
import com.nexcode.app.data.local.entity.ProjectEntity

@Database(
    entities = [
        ProjectEntity::class,
        FileEntity::class,
        ConversationEntity::class,
        BuildEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NexcodeDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun fileDao(): FileDao
    abstract fun conversationDao(): ConversationDao
    abstract fun buildDao(): BuildDao

    companion object {
        @Volatile
        private var INSTANCE: NexcodeDatabase? = null

        fun getInstance(context: Context): NexcodeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexcodeDatabase::class.java,
                    "nexcode.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
