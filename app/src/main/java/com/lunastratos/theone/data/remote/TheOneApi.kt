package com.lunastratos.theone.data.remote

import com.lunastratos.theone.data.AuthStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** 세션이 만료되어 인증이 필요할 때 던지는 예외 */
class SessionExpiredException : Exception("세션이 만료되었습니다. 재로그인이 필요합니다.")

/** 아이디/비밀번호가 틀렸을 때 */
class LoginFailedException(message: String) : Exception(message)

/**
 * 더원모바일 셀프케어 API 클라이언트.
 *
 * 인증: ASP.NET WebForms 로그인. login.aspx GET 으로 __VIEWSTATE/__EVENTVALIDATION 을 파싱한 뒤
 * 자격증명을 POST 하면 UserId / ASP.NET_SessionId 쿠키가 발급된다. 이후 AJAX 엔드포인트는
 * 해당 쿠키로 인증된다.
 */
class TheOneApi(private val store: AuthStore) {

    private val cookieJar = PersistentCookieJar(store)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // ---- 로그인 -------------------------------------------------------------

    /** 자격증명으로 로그인하고 성공 시 자격증명을 저장한다. */
    suspend fun login(id: String, pw: String): Unit = withContext(Dispatchers.IO) {
        // 1) 로그인 페이지 GET → 숨김 필드 파싱
        val pageReq = Request.Builder()
            .url(LOGIN_URL)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        val html = client.newCall(pageReq).execute().use { resp ->
            resp.body?.string().orEmpty()
        }

        val viewState = extractHidden(html, "__VIEWSTATE")
        val viewStateGen = extractHidden(html, "__VIEWSTATEGENERATOR")
        val eventValidation = extractHidden(html, "__EVENTVALIDATION")

        // 2) 자격증명 POST
        val form = FormBody.Builder()
            .add("__EVENTTARGET", "")
            .add("__EVENTARGUMENT", "")
            .add("__VIEWSTATE", viewState)
            .add("__VIEWSTATEGENERATOR", viewStateGen)
            .add("__EVENTVALIDATION", eventValidation)
            .add("ctl00\$ContentPlaceHolder1\$txtId", id)
            .add("ctl00\$ContentPlaceHolder1\$txtPw", pw)
            .add("ctl00\$ContentPlaceHolder1\$btnLogin", "로그인하기")
            .build()

        val loginReq = Request.Builder()
            .url(LOGIN_URL)
            .header("User-Agent", USER_AGENT)
            .header("Referer", LOGIN_URL)
            .post(form)
            .build()

        client.newCall(loginReq).execute().use { /* 쿠키는 cookieJar 가 자동 저장 */ }

        // 3) 성공 판정: UserId 쿠키가 발급되었는가
        val loggedInId = currentUserIdCookie()
        if (loggedInId.isNullOrBlank()) {
            throw LoginFailedException("아이디 또는 비밀번호가 올바르지 않습니다.")
        }

        store.saveCredentials(id, pw)
    }

    /** 저장된 자격증명으로 재로그인 */
    private suspend fun reLoginWithStored() {
        val id = store.userId
        val pw = store.password
        if (id.isNullOrBlank() || pw.isNullOrBlank()) throw SessionExpiredException()
        login(id, pw)
    }

    // ---- 데이터 호출 --------------------------------------------------------

    /**
     * AJAX 엔드포인트에 JSON 을 POST 하고 응답 본문을 반환한다.
     * 세션이 만료되어 HTML(로그인 페이지)이 돌아오면 저장된 자격증명으로 1회 재로그인 후 재시도한다.
     */
    suspend fun postJson(url: String, jsonBody: String): String = withContext(Dispatchers.IO) {
        try {
            rawPostJson(url, jsonBody)
        } catch (e: SessionExpiredException) {
            reLoginWithStored()
            rawPostJson(url, jsonBody)
        }
    }

    private fun rawPostJson(url: String, jsonBody: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "$BASE/view/mypage/usage_status.aspx")
            .header("X-Requested-With", "XMLHttpRequest")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            val trimmed = body.trimStart('﻿', ' ', '\n', '\r', '\t')
            // 인증 실패 시 JSON 대신 HTML(로그인 페이지)이 반환된다.
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                throw SessionExpiredException()
            }
            return trimmed
        }
    }

    private fun currentUserIdCookie(): String? {
        val req = Request.Builder().url(BASE).get().build()
        // cookieJar 에서 직접 읽기 위해 loadForRequest 사용
        return cookieJar.loadForRequest(req.url)
            .firstOrNull { it.name.equals("UserId", ignoreCase = true) }
            ?.value
    }

    fun logout() {
        cookieJar.clear()
        store.clear()
    }

    companion object {
        const val BASE = "https://theonem.co.kr"
        const val LOGIN_URL = "$BASE/view/login/login.aspx"
        const val SELFCARE_URL = "$BASE/common/component/common/AjaxSelfcare_KT.aspx"
        const val JOIN_URL = "$BASE/common/component/member/AjaxJoin.aspx"

        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) TheOneMobile/1.0"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val HIDDEN_REGEX_CACHE = HashMap<String, Regex>()

        /** ASP.NET 숨김 필드 value 추출 */
        private fun extractHidden(html: String, name: String): String {
            val regex = HIDDEN_REGEX_CACHE.getOrPut(name) {
                Regex("""id="$name"[^>]*value="([^"]*)"""")
            }
            return regex.find(html)?.groupValues?.get(1) ?: ""
        }
    }
}
