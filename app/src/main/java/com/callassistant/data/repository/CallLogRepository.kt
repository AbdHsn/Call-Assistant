package com.callassistant.data.repository

import android.content.Context
import android.provider.CallLog
import com.callassistant.data.db.AppDatabase
import com.callassistant.data.entity.CallLogEntry
import com.callassistant.data.sync.CallLogSyncer
import com.callassistant.util.DeletedEntriesStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

interface CallLogRepository {
    fun observeCallLogs(): Flow<List<CallLogEntry>>
    suspend fun syncCallLogs()
    suspend fun deleteCallLogs(entries: List<CallLogEntry>)
}

@Singleton
class CallLogRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val callLogSyncer: CallLogSyncer
) : CallLogRepository {

    private fun callLogKey(number: String, timestamp: Long) = "$number|$timestamp"

    override fun observeCallLogs(): Flow<List<CallLogEntry>> = db.callLogDao().getAll()

    override suspend fun syncCallLogs() {
        val deletedKeys = DeletedEntriesStore.getDeletedCallLogKeys(context)
        val logs = callLogSyncer.sync()
            .filterNot { callLogKey(it.number, it.timestamp) in deletedKeys }
        db.callLogDao().deleteAll()
        db.callLogDao().insertAll(logs)
    }

    override suspend fun deleteCallLogs(entries: List<CallLogEntry>) {
        DeletedEntriesStore.addDeletedCallLogKeys(
            context,
            entries.map { callLogKey(it.number, it.timestamp) }
        )
        val resolver = context.contentResolver
        entries.forEach { entry ->
            try {
                resolver.delete(
                    CallLog.Calls.CONTENT_URI,
                    "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.DATE} = ?",
                    arrayOf(entry.number, entry.timestamp.toString())
                )
            } catch (_: SecurityException) {}
        }
        db.callLogDao().deleteByIds(entries.map { it.id })
    }
}
