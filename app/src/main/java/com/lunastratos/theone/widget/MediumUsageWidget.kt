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
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import com.lunastratos.theone.data.SnapshotStore
import com.lunastratos.theone.data.model.Dashboard
import com.lunastratos.theone.data.model.UsageKind

/**
 * 1x2 중형 위젯 — 데이터 / 문자 / 전화 잔여를 세 칸으로.
 */
class MediumUsageWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dashboard = SnapshotStore(context).load()
        provideContent {
            GlanceTheme {
                if (dashboard == null) EmptyCard(24.dp) else MediumContent(dashboard)
            }
        }
    }
}

@Composable
private fun MediumContent(d: Dashboard) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cardBg(24.dp)
            .padding(horizontal = 4.dp, vertical = 12.dp)
            .then(openApp()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatCell("데이터", d.dataValue(), Brand, 15, hPadding = 6.dp)
        VDivider()
        StatCell("문자", d.remainOf(UsageKind.SMS), Ink, 15, hPadding = 6.dp)
        VDivider()
        StatCell("전화", d.remainOf(UsageKind.VOICE), Ink, 15, hPadding = 6.dp)
    }
}
