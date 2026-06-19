package com.lunastratos.theone.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.lunastratos.theone.data.SnapshotStore
import com.lunastratos.theone.data.model.Dashboard
import com.lunastratos.theone.data.model.UsageKind

/**
 * 1x1 소형 위젯 — 데이터 / 문자 / 전화 잔여량을 한 줄씩.
 */
class SmallUsageWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dashboard = SnapshotStore(context).load()
        provideContent {
            GlanceTheme {
                if (dashboard == null) EmptyCard(24.dp) else SmallContent(dashboard)
            }
        }
    }
}

@Composable
private fun SmallContent(d: Dashboard) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cardBg(24.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .then(openApp()),
    ) {
        MiniRow("데이터", d.dataValue(), Brand)
        Spacer(GlanceModifier.defaultWeight())
        MiniRow("문자", d.remainOf(UsageKind.SMS), Ink)
        Spacer(GlanceModifier.defaultWeight())
        MiniRow("전화", d.remainOf(UsageKind.VOICE), Ink)
    }
}

/** 라벨 + 값을 한 줄에 (좌: 라벨 / 우: 값) */
@Composable
private fun MiniRow(labelText: String, valueText: String, valueColor: androidx.glance.unit.ColorProvider) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(labelText, style = label(9), maxLines = 1)
        Spacer(GlanceModifier.defaultWeight())
        Text(valueText, style = value(13, valueColor), maxLines = 1)
    }
}
