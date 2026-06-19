package com.lunastratos.theone.data

import android.content.Context
import com.lunastratos.theone.data.model.Dashboard
import com.lunastratos.theone.data.model.DashboardMapper
import com.lunastratos.theone.data.model.JoinResponse
import com.lunastratos.theone.data.model.X12Response
import com.lunastratos.theone.data.model.X18Response
import com.lunastratos.theone.data.remote.TheOneApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

/**
 * 로그인/조회를 담당하는 단일 진입점. 세 엔드포인트(가입정보·실시간 사용량·실시간 요금)를
 * 병렬 호출하여 [Dashboard] 로 합친다.
 */
class UsageRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val store = AuthStore.get(appContext)
    private val api = TheOneApi(store)
    private val snapshot = SnapshotStore(appContext)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val hasCredentials: Boolean get() = store.hasCredentials

    suspend fun login(id: String, pw: String) = api.login(id, pw)

    fun logout() {
        api.logout()
        snapshot.clear()
    }

    /** 캐시된 마지막 스냅샷(있으면) */
    fun cachedDashboard(): Dashboard? = snapshot.load()

    /**
     * 세 엔드포인트를 병렬 조회하여 대시보드를 만든다.
     * 일부 엔드포인트만 실패하면 성공한 데이터로 구성하고, 전부 실패하면 예외를 던진다.
     */
    suspend fun fetchDashboard(now: Long): Dashboard = coroutineScope {
        val joinDef = async { runCatching { decode<JoinResponse>(api.postJson(TheOneApi.JOIN_URL, JOIN_BODY)) } }
        val usageDef = async { runCatching { decode<X12Response>(api.postJson(TheOneApi.SELFCARE_URL, X12_BODY)) } }
        val billDef = async { runCatching { decode<X18Response>(api.postJson(TheOneApi.SELFCARE_URL, X18_BODY)) } }

        val joinR = joinDef.await()
        val usageR = usageDef.await()
        val billR = billDef.await()

        // 전부 실패하면 대표 예외를 던진다 (보통 SessionExpired/네트워크)
        if (joinR.isFailure && usageR.isFailure && billR.isFailure) {
            throw usageR.exceptionOrNull() ?: joinR.exceptionOrNull()
                ?: billR.exceptionOrNull() ?: IllegalStateException("데이터를 불러오지 못했습니다.")
        }

        val dashboard = DashboardMapper.map(
            join = joinR.getOrNull(),
            usage = usageR.getOrNull(),
            bill = billR.getOrNull(),
            now = now,
        )
        snapshot.save(dashboard)
        dashboard
    }

    private inline fun <reified T> decode(body: String): T = json.decodeFromString(body)

    companion object {
        private const val JOIN_BODY = """{"header":[{"type":"05"}],"body":[{}]}"""
        private const val X12_BODY = """{"header":[{"type":"X12"}],"body":[{}]}"""
        private const val X18_BODY = """{"header":[{"type":"X18"}],"body":[{}]}"""

        @Volatile
        private var instance: UsageRepository? = null

        fun get(context: Context): UsageRepository =
            instance ?: synchronized(this) {
                instance ?: UsageRepository(context).also { instance = it }
            }
    }
}
