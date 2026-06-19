package com.lunastratos.theone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ===== API 응답 (서버 JSON 그대로) ===== */

/** 가입정보 (AjaxJoin.aspx, type=05) */
@Serializable
data class JoinResponse(
    val result: String? = null,
    @SerialName("GDNM") val planName: String? = null,      // 요금제명
    @SerialName("SEEDATA") val baseData: String? = null,   // 기본 데이터 (예: 20GB)
    @SerialName("SEEVOICE") val baseVoice: String? = null,
    @SerialName("SEELETTER") val baseSms: String? = null,
    @SerialName("NETDIV") val network: String? = null,     // 5G / LTE
    @SerialName("MDN") val phone: String? = null,          // 010-****-1245
    @SerialName("DEVMODEL") val device: String? = null,
    @SerialName("LOPENDT") val openDate: String? = null,
    @SerialName("CONSTS") val status: String? = null,      // 사용중
)

/** 실시간 사용량 (AjaxSelfcare_KT.aspx, type=X12) */
@Serializable
data class X12Response(
    @SerialName("RESULT") val result: String? = null,
    @SerialName("DATA") val data: List<X12Item> = emptyList(),
)

@Serializable
data class X12Item(
    val strBunGun: String? = null,        // V/U=음성, D=문자, P=데이터
    val strSvcName: String? = null,       // 서비스명
    val strFreeMinTotal: String? = null,  // 총량 (데이터=MB, 음성=분, 문자=건), "무제한" 가능
    val strFreeMinUse: String? = null,    // 사용량
    val strFreeMinReMain: String? = null, // 잔여량
)

/** 실시간 요금 (AjaxSelfcare_KT.aspx, type=X18) */
@Serializable
data class X18Response(
    @SerialName("RESULT") val result: String? = null,
    val serachDay: String? = null,   // (서버 철자 그대로) 조회일
    val searchTime: String? = null,  // 조회 기간 (예: 06.01~06.19)
    val amntDto: List<AmntItem> = emptyList(),
)

@Serializable
data class AmntItem(
    @SerialName("SEQ") val seq: String? = null,
    @SerialName("GUBUN") val label: String? = null,    // 항목명, "당월요금계" 가 합계
    @SerialName("PAYMENT") val payment: String? = null, // 금액 (콤마 포함 문자열)
)
