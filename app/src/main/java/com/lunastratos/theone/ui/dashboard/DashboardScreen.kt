package com.lunastratos.theone.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunastratos.theone.data.model.BillItem
import com.lunastratos.theone.data.model.Dashboard
import com.lunastratos.theone.data.model.UsageKind
import com.lunastratos.theone.data.model.UsageLine
import com.lunastratos.theone.ui.theme.DataColor
import com.lunastratos.theone.ui.theme.SmsColor
import com.lunastratos.theone.ui.theme.VoiceColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboard: Dashboard?,
    refreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("실시간 요금 조회", style = MaterialTheme.typography.titleMedium)
                        val phone = dashboard?.phone
                        if (!phone.isNullOrBlank()) {
                            Text(
                                phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "로그아웃")
                    }
                },
            )
        },
    ) { inner ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (dashboard == null) {
                    item {
                        EmptyOrError(error)
                    }
                    return@LazyColumn
                }

                item { PlanCard(dashboard) }

                // 실시간 사용량(데이터 → 문자 → 전화)을 위에
                if (dashboard.usages.isNotEmpty()) {
                    item {
                        Text(
                            "실시간 사용량",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                        )
                    }
                    items(dashboard.usages.sortedBy { usageOrder(it.kind) }) { line -> UsageCard(line) }
                }

                // 실시간 요금은 아래
                item { BillCard(dashboard) }

                item {
                    Text(
                        text = "마지막 갱신: ${formatTime(dashboard.updatedAt)}" +
                            if (dashboard.period.isNotBlank()) "  ·  기간 ${dashboard.period}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
                if (error != null) {
                    item {
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(d: Dashboard) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (d.network.isNotBlank()) {
                    Badge(d.network)
                    Spacer(Modifier.height(0.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(d.planName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (d.baseData.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "기본 데이터 ${d.baseData}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BillCard(d: Dashboard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "당월 예상 요금",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                d.billTotal,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (d.billItems.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                d.billItems.forEach { item -> BillRow(item) }
            }
        }
    }
}

@Composable
private fun BillRow(item: BillItem) {
    val isTotal = item.label == "당월요금계"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            item.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            item.amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun UsageCard(line: UsageLine) {
    val color = when (line.kind) {
        UsageKind.DATA -> DataColor
        UsageKind.VOICE -> VoiceColor
        UsageKind.SMS -> SmsColor
        UsageKind.OTHER -> MaterialTheme.colorScheme.primary
    }
    val animatedFraction by animateFloatAsState(
        targetValue = if (line.unlimited) 1f else line.fraction,
        label = "usage",
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(line.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (line.unlimited) "무제한" else "${(line.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = color,
                trackColor = color.copy(alpha = 0.15f),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LabelValue("사용", line.usedText, color)
                LabelValue("잔여", line.remainText, MaterialTheme.colorScheme.onSurface, end = true)
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String, valueColor: Color, end: Boolean = false) {
    Column(horizontalAlignment = if (end) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyOrError(error: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            error ?: "데이터를 불러오는 중입니다…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 사용량 표시 순서: 데이터 → 문자 → 전화 → 기타 */
private fun usageOrder(kind: UsageKind): Int = when (kind) {
    UsageKind.DATA -> 0
    UsageKind.SMS -> 1
    UsageKind.VOICE -> 2
    UsageKind.OTHER -> 3
}

private fun formatTime(epoch: Long): String =
    SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(Date(epoch))
