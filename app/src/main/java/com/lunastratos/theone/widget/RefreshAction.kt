package com.lunastratos.theone.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/** 위젯의 새로고침 버튼을 누르면 1회 갱신 작업을 큐에 넣는다. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        UsageWidgetScheduler.refreshNow(context)
    }
}
