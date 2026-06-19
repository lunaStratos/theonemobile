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
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import com.lunastratos.theone.data.SnapshotStore
import com.lunastratos.theone.data.model.Dashboard
import com.lunastratos.theone.data.model.UsageKind

/**
 * 1x4 와이드 위젯 — 브랜드 + 데이터 / 음성 / 예상 요금을 한 줄에, 우측 새로고침.
 */
class WideUsageWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dashboard = SnapshotStore(context).load()
        provideContent {
            GlanceTheme {
                if (dashboard == null) EmptyCard(24.dp) else WideContent(dashboard)
            }
        }
    }
}

@Composable
private fun WideContent(d: Dashboard) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cardBg(24.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 브랜드 배지
        Column(modifier = openApp()) {
            Text("더원", style = value(13, Brand), maxLines = 1)
            Text("모바일", style = value(10, SubInk), maxLines = 1)
        }

        Spacer(GlanceModifier.width(10.dp))
        VDivider()

        // 데이터 / 문자 / 전화 순
        StatCell("데이터", d.dataValue(), Brand, 15)
        VDivider()
        StatCell("문자", d.remainOf(UsageKind.SMS), Ink, 15)
        VDivider()
        StatCell("전화", d.remainOf(UsageKind.VOICE), Ink, 15)

        // 새로고침
        Spacer(GlanceModifier.width(6.dp))
        Text(
            "↻",
            style = value(18, Brand),
            modifier = refreshClick().width(28.dp),
        )
    }
}
