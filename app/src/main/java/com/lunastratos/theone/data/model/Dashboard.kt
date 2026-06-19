package com.lunastratos.theone.data.model

import kotlinx.serialization.Serializable

/* ===== UI/위젯용 도메인 모델 ===== */

@Serializable
enum class UsageKind { DATA, VOICE, SMS, OTHER }

@Serializable
data class UsageLine(
    val label: String,
    val kind: UsageKind,
    val unlimited: Boolean,
    val usedText: String,
    val remainText: String,
    val totalText: String,
    /** 사용률 0f..1f (무제한이면 0f) */
    val fraction: Float,
)

@Serializable
data class BillItem(
    val label: String,
    val amount: String, // "10,200원"
)

@Serializable
data class Dashboard(
    val planName: String,
    val network: String,
    val phone: String,
    val baseData: String,
    val usages: List<UsageLine>,
    /** 위젯에서 강조할 대표 데이터 라인(데이터-합계). 없으면 null */
    val dataLine: UsageLine?,
    val billTotal: String,   // "3,968원"
    val billItems: List<BillItem>,
    val period: String,      // "06.01~06.19"
    val updatedAt: Long,
)

/**
 * API 응답(JoinResponse / X12Response / X18Response)을 화면·위젯용 [Dashboard] 로 변환한다.
 * 변환 규칙은 웹(usage_status.js)의 표시 로직을 따른다.
 */
object DashboardMapper {

    fun map(
        join: JoinResponse?,
        usage: X12Response?,
        bill: X18Response?,
        now: Long,
    ): Dashboard {
        val usages = (usage?.data ?: emptyList())
            .mapNotNull { toUsageLine(it) }

        val dataLine = usages.firstOrNull { it.kind == UsageKind.DATA && it.label.contains("합계") }
            ?: usages.firstOrNull { it.kind == UsageKind.DATA }

        val items = (bill?.amntDto ?: emptyList())
            .filter { !it.label.isNullOrBlank() }
            .map { BillItem(it.label!!.trim(), formatWon(it.payment)) }

        val total = bill?.amntDto
            ?.firstOrNull { it.label?.trim() == "당월요금계" }
            ?.let { formatWon(it.payment) }
            ?: items.lastOrNull()?.amount
            ?: "-"

        return Dashboard(
            planName = join?.planName?.trim().orEmpty().ifBlank { "요금제 정보 없음" },
            network = join?.network?.trim().orEmpty(),
            phone = join?.phone?.trim().orEmpty(),
            baseData = join?.baseData?.trim().orEmpty(),
            usages = usages,
            dataLine = dataLine,
            billTotal = total,
            billItems = items,
            period = bill?.searchTime?.trim().orEmpty(),
            updatedAt = now,
        )
    }

    private fun toUsageLine(item: X12Item): UsageLine? {
        val totalRaw = item.strFreeMinTotal?.trim().orEmpty()
        // 총량이 비어있거나 0 이면 표시하지 않음 (웹 로직과 동일)
        val unlimited = totalRaw == "무제한"
        if (!unlimited && (totalRaw.isEmpty() || parseLong(totalRaw) == 0L)) return null

        val kind = when (item.strBunGun?.trim()?.uppercase()) {
            "P" -> UsageKind.DATA
            "V", "U" -> UsageKind.VOICE
            "D" -> UsageKind.SMS
            else -> UsageKind.OTHER
        }

        var name = item.strSvcName?.trim().orEmpty()
        if (name == "데이터-합계") name = "데이터"
        else if (name == "속도제어(QoS)데이터-합계") name = "속도제어(QoS) 이후 데이터"

        val usedVal = parseLong(item.strFreeMinUse)
        val remainVal = parseLong(item.strFreeMinReMain)
        val totalVal = if (unlimited) 0L else parseLong(totalRaw)

        val usedText: String
        val remainText: String
        val totalText: String
        when (kind) {
            UsageKind.DATA -> {
                usedText = formatData(usedVal)
                remainText = if (unlimited) "기본제공" else formatData(remainVal)
                totalText = if (unlimited) "무제한" else formatData(totalVal)
            }
            UsageKind.VOICE -> {
                usedText = "${comma(usedVal)}분"
                remainText = if (unlimited) "기본제공" else "${comma(remainVal)}분"
                totalText = if (unlimited) "무제한" else "${comma(totalVal)}분"
            }
            UsageKind.SMS -> {
                usedText = "${comma(usedVal)}건"
                remainText = if (unlimited) "기본제공" else "${comma(remainVal)}건"
                totalText = if (unlimited) "무제한" else "${comma(totalVal)}건"
            }
            UsageKind.OTHER -> {
                usedText = comma(usedVal)
                remainText = if (unlimited) "기본제공" else comma(remainVal)
                totalText = if (unlimited) "무제한" else comma(totalVal)
            }
        }

        val fraction = if (unlimited || totalVal <= 0L) 0f
        else (usedVal.toFloat() / totalVal.toFloat()).coerceIn(0f, 1f)

        return UsageLine(
            label = name.ifBlank { "사용량" },
            kind = kind,
            unlimited = unlimited,
            usedText = usedText,
            remainText = remainText,
            totalText = totalText,
            fraction = fraction,
        )
    }

    /** MB 값을 사람이 읽기 좋은 문자열로. 1024MB 이상이면 GB(소수 2자리). */
    private fun formatData(mb: Long): String {
        if (mb <= 0L) return "0MB"
        return if (mb >= 1024) {
            val gb = mb / 1024.0
            "${trimDecimal(gb)}GB"
        } else {
            "${comma(mb)}MB"
        }
    }

    private fun trimDecimal(value: Double): String {
        val rounded = String.format("%.2f", value)
        val noTrailing = rounded.trimEnd('0').trimEnd('.')
        // 천단위 콤마
        val parts = noTrailing.split(".")
        val intPart = parts[0].toLongOrNull()?.let { comma(it) } ?: parts[0]
        return if (parts.size > 1) "$intPart.${parts[1]}" else intPart
    }

    private fun comma(v: Long): String = "%,d".format(v)

    /** 금액 문자열을 "원" 단위로. 음수/콤마 보존. */
    private fun formatWon(raw: String?): String {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return "-"
        return if (s.endsWith("원")) s else "${s}원"
    }

    /** 숫자만 추출하여 Long 으로. 부호 보존. */
    private fun parseLong(raw: String?): Long {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return 0L
        val neg = s.startsWith("-")
        val digits = s.filter { it.isDigit() }
        if (digits.isEmpty()) return 0L
        val value = digits.toLongOrNull() ?: 0L
        return if (neg) -value else value
    }
}
