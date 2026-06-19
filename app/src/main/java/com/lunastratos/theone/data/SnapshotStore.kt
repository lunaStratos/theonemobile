package com.lunastratos.theone.data

import android.content.Context
import com.lunastratos.theone.data.model.Dashboard
import kotlinx.serialization.json.Json

/**
 * 마지막으로 조회한 [Dashboard] 스냅샷을 저장/로드한다.
 * 앱이 닫혀 있어도 위젯이 마지막 데이터를 즉시 표시할 수 있게 한다.
 */
class SnapshotStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("theone_snapshot", Context.MODE_PRIVATE)

    fun save(dashboard: Dashboard) {
        prefs.edit().putString(KEY, json.encodeToString(Dashboard.serializer(), dashboard)).apply()
    }

    fun load(): Dashboard? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching { json.decodeFromString<Dashboard>(raw) }.getOrNull()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "dashboard"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
