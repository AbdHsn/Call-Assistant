package com.callassistant.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.callassistant.data.entity.BlockedNumber
import com.callassistant.data.entity.CallLogEntry
import com.callassistant.data.entity.Contact
import com.callassistant.data.entity.SmsMessage
import com.callassistant.data.entity.SpamRule

@Database(
    entities = [
        Contact::class,
        CallLogEntry::class,
        SmsMessage::class,
        SpamRule::class,
        BlockedNumber::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun callLogDao(): CallLogDao
    abstract fun smsDao(): SmsDao
    abstract fun spamRuleDao(): SpamRuleDao
    abstract fun blockedNumberDao(): BlockedNumberDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "call_assistant_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
