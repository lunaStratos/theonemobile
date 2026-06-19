package com.lunastratos.theone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lunastratos.theone.ui.MainViewModel
import com.lunastratos.theone.ui.dashboard.DashboardScreen
import com.lunastratos.theone.ui.login.LoginScreen
import com.lunastratos.theone.ui.theme.TheOneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheOneTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: MainViewModel = viewModel()
                    if (vm.loggedIn) {
                        DashboardScreen(
                            dashboard = vm.dashboard,
                            refreshing = vm.refreshing,
                            error = vm.dashboardError,
                            onRefresh = vm::refresh,
                            onLogout = vm::logout,
                        )
                    } else {
                        LoginScreen(
                            loading = vm.loginLoading,
                            error = vm.loginError,
                            onLogin = vm::login,
                        )
                    }
                }
            }
        }
    }
}
