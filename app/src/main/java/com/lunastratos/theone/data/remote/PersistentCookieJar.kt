package com.lunastratos.theone.data.remote

import com.lunastratos.theone.data.AuthStore
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * theonem.co.kr 의 세션 쿠키(ASP.NET_SessionId, UserId 등)를 메모리 캐시 + AuthStore 에
 * 영속 저장하는 CookieJar. 앱과 위젯/워커가 같은 세션을 공유하도록 한다.
 *
 * 모든 쿠키는 호스트 전용(host-only) 쿠키로 취급한다.
 */
class PersistentCookieJar(private val store: AuthStore) : CookieJar {

    // name -> value
    private val cache = ConcurrentHashMap<String, String>()

    init {
        store.cookieString?.split(";")?.forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val name = pair.substring(0, idx).trim()
                val value = pair.substring(idx + 1).trim()
                if (name.isNotEmpty()) cache[name] = value
            }
        }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        var changed = false
        for (cookie in cookies) {
            // 만료되었거나 값이 비면 삭제
            if (cookie.expiresAt < System.currentTimeMillis() || cookie.value.isEmpty()) {
                if (cache.remove(cookie.name) != null) changed = true
            } else {
                if (cache.put(cookie.name, cookie.value) != cookie.value) changed = true
            }
        }
        if (changed) persist()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (cache.isEmpty()) return emptyList()
        val host = url.host
        return cache.map { (name, value) ->
            Cookie.Builder()
                .name(name)
                .value(value)
                .domain(host)
                .path("/")
                .build()
        }
    }

    @Synchronized
    fun clear() {
        cache.clear()
        store.cookieString = null
    }

    private fun persist() {
        store.cookieString = cache.entries.joinToString(";") { "${it.key}=${it.value}" }
    }
}
