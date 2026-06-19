package com.lunastratos.theone.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lunastratos.theone.data.UsageRepository
import com.lunastratos.theone.data.model.Dashboard
import com.lunastratos.theone.data.remote.LoginFailedException
import com.lunastratos.theone.data.remote.SessionExpiredException
import com.lunastratos.theone.widget.UsageWidgetScheduler
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = UsageRepository.get(app)

    /** true면 대시보드, false면 로그인 화면 */
    var loggedIn by mutableStateOf(repo.hasCredentials)
        private set

    // 로그인 화면 상태
    var loginLoading by mutableStateOf(false)
        private set
    var loginError by mutableStateOf<String?>(null)
        private set

    // 대시보드 상태
    var dashboard by mutableStateOf<Dashboard?>(repo.cachedDashboard())
        private set
    var refreshing by mutableStateOf(false)
        private set
    var dashboardError by mutableStateOf<String?>(null)
        private set

    init {
        if (loggedIn) refresh()
    }

    fun login(id: String, pw: String) {
        if (id.isBlank() || pw.isBlank()) {
            loginError = "아이디와 비밀번호를 입력하세요."
            return
        }
        viewModelScope.launch {
            loginLoading = true
            loginError = null
            try {
                repo.login(id.trim(), pw)
                loggedIn = true
                UsageWidgetScheduler.schedulePeriodic(getApplication())
                refresh()
            } catch (e: LoginFailedException) {
                loginError = e.message ?: "로그인에 실패했습니다."
            } catch (e: Exception) {
                loginError = "네트워크 오류가 발생했습니다. 잠시 후 다시 시도하세요."
            } finally {
                loginLoading = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing = true
            dashboardError = null
            try {
                dashboard = repo.fetchDashboard(System.currentTimeMillis())
                UsageWidgetScheduler.updateWidgets(getApplication())
            } catch (e: SessionExpiredException) {
                // 저장된 자격증명으로도 재로그인 실패 → 로그인 화면으로
                loggedIn = false
            } catch (e: Exception) {
                dashboardError = "데이터를 불러오지 못했습니다. 당겨서 새로고침 해보세요."
            } finally {
                refreshing = false
            }
        }
    }

    fun logout() {
        repo.logout()
        dashboard = null
        loggedIn = false
        UsageWidgetScheduler.cancel(getApplication())
    }
}
