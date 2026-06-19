package com.lunastratos.theone.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lunastratos.theone.data.UsageRepository

/**
 * 백그라운드에서 대시보드를 갱신하고(스냅샷 저장) 위젯을 다시 그린다.
 * 주기 작업과 위젯 새로고침 버튼 양쪽에서 사용한다.
 */
class UsageWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = UsageRepository.get(applicationContext)
        if (!repo.hasCredentials) return Result.success() // 로그인 전이면 할 일 없음

        return try {
            repo.fetchDashboard(System.currentTimeMillis())
            updateAllWidgets(applicationContext)
            Result.success()
        } catch (e: Exception) {
            // 마지막 스냅샷이라도 다시 그려준다
            runCatching { updateAllWidgets(applicationContext) }
            if (runAttemptCount < 2) Result.retry() else Result.success()
        }
    }
}
