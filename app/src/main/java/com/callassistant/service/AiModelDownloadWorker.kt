package com.callassistant.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.callassistant.data.repository.AiModelRepository
import com.callassistant.util.AiModelDownloadNotifier
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class AiModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            AiDownloadEntryPoint::class.java
        )
        val repository = entryPoint.aiModelRepository()
        if (repository.modelStatus.value is com.callassistant.ai.model.AiModelStatus.Ready) {
            return Result.success()
        }

        val definition = repository.selectedDefinition()
        setForeground(
            AiModelDownloadNotifier.foregroundInfo(
                context = applicationContext,
                modelName = definition.displayName,
                progressPercent = 0,
                bytesDone = 0L,
                totalBytes = definition.sizeBytes
            )
        )

        return repository.downloadModel { _, _, _ -> }
            .fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (it is com.callassistant.data.repository.AiModelRepositoryImpl.CancelledDownload) {
                    Result.failure()
                } else {
                    Result.failure()
                }
            }
        )
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AiDownloadEntryPoint {
        fun aiModelRepository(): AiModelRepository
    }

    companion object {
        private const val WORK_NAME = "ai_model_download"

        fun enqueue(context: Context, replaceExisting: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<AiModelDownloadWorker>()
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
