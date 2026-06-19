package com.lunastratos.theone.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 로그인 자격증명과 세션 쿠키를 암호화하여 저장한다.
 * EncryptedSharedPreferences 생성에 실패하는 일부 기기(키스토어 손상 등)에서는
 * 일반 SharedPreferences 로 안전하게 폴백한다.
 */
class AuthStore private constructor(private val prefs: SharedPreferences) {

    var userId: String?
        get() = prefs.getString(KEY_ID, null)
        private set(value) = prefs.edit().putString(KEY_ID, value).apply()

    var password: String?
        get() = prefs.getString(KEY_PW, null)
        private set(value) = prefs.edit().putString(KEY_PW, value).apply()

    /** "name=value;name2=value2" 형태로 직렬화된 쿠키 */
    var cookieString: String?
        get() = prefs.getString(KEY_COOKIES, null)
        set(value) = prefs.edit().putString(KEY_COOKIES, value).apply()

    val hasCredentials: Boolean
        get() = !userId.isNullOrBlank() && !password.isNullOrBlank()

    fun saveCredentials(id: String, pw: String) {
        prefs.edit()
            .putString(KEY_ID, id)
            .putString(KEY_PW, pw)
            .apply()
    }

    /** 로그아웃: 모든 자격증명/쿠키 삭제 */
    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val FILE_NAME = "theone_secure_prefs"
        private const val KEY_ID = "user_id"
        private const val KEY_PW = "user_pw"
        private const val KEY_COOKIES = "session_cookies"

        @Volatile
        private var instance: AuthStore? = null

        fun get(context: Context): AuthStore =
            instance ?: synchronized(this) {
                instance ?: AuthStore(buildPrefs(context.applicationContext)).also { instance = it }
            }

        private fun buildPrefs(context: Context): SharedPreferences = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (t: Throwable) {
            // 암호화 저장소 생성 실패 시 일반 저장소로 폴백
            context.getSharedPreferences("${FILE_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }
}
