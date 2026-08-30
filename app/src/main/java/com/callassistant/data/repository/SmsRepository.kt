package com.callassistant.data.repository

import android.content.Context
import android.provider.Telephony
import com.callassistant.data.db.AppDatabase
import com.callassistant.data.entity.SmsDirection
import com.callassistant.data.entity.SmsMessage
import com.callassistant.data.sync.SmsSyncer
import com.callassistant.util.DeletedEntriesStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface SmsRepository {
    fun observeSmsMessages(): Flow<List<SmsMessage>>
    suspend fun syncSms()
    suspend fun saveMessage(number: String, body: String, direction: SmsDirection)
    suspend fun deleteMessages(messages: List<SmsMessage>, onProgress: (deleted: Int, total: Int) -> Unit)
}

@Singleton
class SmsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val smsSyncer: SmsSyncer
) : SmsRepository {

    private fun smsKey(number: String, timestamp: Long, body: String) = "$number|$timestamp|$body"

    override fun observeSmsMessages(): Flow<List<SmsMessage>> = db.smsDao().getAll()

    override suspend fun syncSms() {
        val deletedKeys = DeletedEntriesStore.getDeletedSmsKeys(context)
        val synced = smsSyncer.sync()
            .filterNot { smsKey(it.number, it.timestamp, it.body) in deletedKeys }
        val existing = db.smsDao().getAll().first()
        val syncedKeys = synced.associateBy { smsKey(it.number, it.timestamp, it.body) }
        val merged = existing.filter { smsKey(it.number, it.timestamp, it.body) !in syncedKeys } + synced
        db.smsDao().deleteAll()
        db.smsDao().insertAll(merged)
    }

    override suspend fun saveMessage(number: String, body: String, direction: SmsDirection) {
        db.smsDao().insert(
            SmsMessage(number = number, body = body, timestamp = System.currentTimeMillis(), direction = direction)
        )
    }

    override suspend fun deleteMessages(messages: List<SmsMessage>, onProgress: (deleted: Int, total: Int) -> Unit) {
        val total = messages.size
        onProgress(0, total)
        val resolver = context.contentResolver
        messages.forEachIndexed { index, message ->
            DeletedEntriesStore.addDeletedSmsKeys(
                context,
                listOf(smsKey(message.number, message.timestamp, message.body))
            )
            try {
                resolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.DATE} = ?",
                    arrayOf(message.number, message.timestamp.toString())
                )
            } catch (_: SecurityException) {
            }
            db.smsDao().deleteByIds(listOf(message.id))
            onProgress(index + 1, total)
        }
    }
}
