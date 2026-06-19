package com.lunastratos.theone.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** 위젯 갱신 작업 스케줄링 진입점 */
object UsageWidgetScheduler {

    private const val PERIODIC = "usage_widget_periodic"
    private const val ONESHOT = "usage_widget_oneshot"

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** 15분 주기 갱신(WorkManager 최소 15분). 로그인 성공 시 호출. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<UsageWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** 위젯의 새로고침 버튼 / 위젯 추가 시 즉시 1회 갱신 */
    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<UsageWorker>()
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONESHOT,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /** 이미 저장된 스냅샷으로 위젯 UI만 즉시 다시 그린다(네트워크 없이). */
    suspend fun updateWidgets(context: Context) {
        updateAllWidgets(context)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        WorkManager.getInstance(context).cancelUniqueWork(ONESHOT)
    }
}
