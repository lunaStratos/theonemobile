package com.lunastratos.theone.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 1x1 / 1x2 / 1x4 위젯 리시버.
 * 첫 위젯이 추가되면 주기 갱신을 등록(KEEP 정책으로 중복 무해)하고,
 * 추가/갱신 시 즉시 1회 데이터를 가져온다.
 */
abstract class BaseUsageReceiver : GlanceAppWidgetReceiver() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        UsageWidgetScheduler.schedulePeriodic(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        UsageWidgetScheduler.refreshNow(context)
    }
}

class SmallUsageWidgetReceiver : BaseUsageReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallUsageWidget()
}

class MediumUsageWidgetReceiver : BaseUsageReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumUsageWidget()
}

class WideUsageWidgetReceiver : BaseUsageReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WideUsageWidget()
}
