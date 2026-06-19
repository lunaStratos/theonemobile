package com.lunastratos.theone.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lunastratos.theone.MainActivity
import com.lunastratos.theone.R
import com.lunastratos.theone.data.model.Dashboard
import com.lunastratos.theone.data.model.UsageKind
import com.lunastratos.theone.data.model.UsageLine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* =====================================================================
 *  더원모바일 위젯 공통 디자인 시스템
 *  - 화이트 카드 배경 + 다크 텍스트 + 브랜드 블루 포인트 (T world 스타일).
 *  - 1x1 / 1x2 / 1x4 세 위젯이 이 토큰/헬퍼를 공유한다.
 * ===================================================================== */

/* ---- 컬러 토큰 (화이트 카드 위에서의 가독성 기준) ---- */
/** 값 텍스트 (거의 검정) */
internal val Ink = ColorProvider(Color(0xFF17181A))
/** 라벨/보조 텍스트 (그레이) */
internal val SubInk = ColorProvider(Color(0xFF8A8F98))
/** 브랜드 블루 포인트 (데이터 강조·새로고침) */
internal val Brand = ColorProvider(Color(0xFF1457E6))
/** 구분선 그레이 */
internal val DividerC = ColorProvider(Color(0xFFECEDF0))

/* ---- 클릭 동작 ---- */
internal fun openApp() = GlanceModifier.clickable(actionStartActivity<MainActivity>())
internal fun refreshClick() = GlanceModifier.clickable(actionRunCallback<RefreshAction>())

/* ---- 화이트 카드 배경 + 라운드 코너 ---- */
internal fun GlanceModifier.cardBg(corner: Dp = 24.dp): GlanceModifier = this
    .background(ImageProvider(R.drawable.widget_card), ContentScale.FillBounds)
    .cornerRadius(corner)

/* ---- 텍스트 스타일 헬퍼 ---- */
internal fun label(size: Int = 10) =
    TextStyle(color = SubInk, fontSize = size.sp, fontWeight = FontWeight.Medium)

internal fun value(size: Int, color: ColorProvider = Ink) =
    TextStyle(color = color, fontSize = size.sp, fontWeight = FontWeight.Bold)

/* ---- 세로 구분선 ---- */
@Composable
internal fun VDivider() {
    Spacer(GlanceModifier.width(1.dp).fillMaxHeight().background(DividerC))
}

/* ---- Row 안의 통계 셀 (라벨 + 값) ---- */
@Composable
internal fun RowScope.StatCell(
    labelText: String,
    valueText: String,
    valueColor: ColorProvider = Ink,
    valueSize: Int = 16,
    hPadding: Dp = 8.dp,
) {
    Column(modifier = GlanceModifier.defaultWeight().padding(horizontal = hPadding)) {
        Text(labelText, style = label(10), maxLines = 1)
        Spacer(GlanceModifier.height(2.dp))
        Text(valueText, style = value(valueSize, valueColor), maxLines = 1)
    }
}

/* ---- 미로그인 안내 (모든 위젯 공통) ---- */
@Composable
internal fun EmptyCard(corner: Dp) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cardBg(corner)
            .padding(14.dp)
            .then(openApp()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("더원요금조회", style = value(14, Brand))
        Spacer(GlanceModifier.height(4.dp))
        Text("탭하여 로그인", style = TextStyle(color = SubInk, fontSize = 11.sp))
    }
}

/* ---- 표시 값 추출 헬퍼 ---- */
internal fun Dashboard.dataValue(): String = when {
    dataLine == null -> "-"
    dataLine!!.unlimited -> "기본제공"
    else -> dataLine!!.remainText
}

internal fun Dashboard.usageOf(kind: UsageKind): UsageLine? =
    usages.firstOrNull { it.kind == kind }

/** 해당 종류의 잔여량 표시 문자열 (없으면 "-", 무제한이면 "기본제공") */
internal fun Dashboard.remainOf(kind: UsageKind): String {
    val line = usageOf(kind) ?: return "-"
    return if (line.unlimited) "기본제공" else line.remainText
}

internal fun formatTime(epoch: Long): String =
    SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(Date(epoch))

/* ---- 세 위젯을 한 번에 다시 그린다 ---- */
internal suspend fun updateAllWidgets(context: Context) {
    SmallUsageWidget().updateAll(context)
    MediumUsageWidget().updateAll(context)
    WideUsageWidget().updateAll(context)
}
