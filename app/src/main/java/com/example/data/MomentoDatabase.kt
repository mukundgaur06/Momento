package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Task::class,
        Goal::class,
        Event::class,
        Expense::class,
        Habit::class,
        Note::class,
        UserProfile::class,
        CalendarEventLink::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MomentoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun goalDao(): GoalDao
    abstract fun eventDao(): EventDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun habitDao(): HabitDao
    abstract fun noteDao(): NoteDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun calendarEventLinkDao(): CalendarEventLinkDao

    companion object {
        @Volatile
        private var INSTANCE: MomentoDatabase? = null

        fun getDatabase(context: Context): MomentoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MomentoDatabase::class.java,
                    "momento_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `calendar_event_links` (`localEventId` INTEGER NOT NULL, `providerEventId` INTEGER NOT NULL, `calendarId` INTEGER NOT NULL, `lastSyncedAt` INTEGER NOT NULL, PRIMARY KEY(`localEventId`))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_calendar_event_links_providerEventId` ON `calendar_event_links` (`providerEventId`)")
            }
        }
    }
}
