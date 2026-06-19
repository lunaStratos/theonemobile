package com.lunastratos.theone.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 더원모바일 브랜드 컬러 (KT 알뜰폰 계열 — 딥 블루 + 시안 포인트)
val BrandPrimary = Color(0xFF1457E6)
val BrandPrimaryDark = Color(0xFF8AB4FF)
val BrandAccent = Color(0xFF00C2D1)
val DataColor = Color(0xFF1457E6)
val VoiceColor = Color(0xFF00A86B)
val SmsColor = Color(0xFFF59E0B)

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    secondary = BrandAccent,
    tertiary = VoiceColor,
)

private val DarkColors = darkColorScheme(
    primary = BrandPrimaryDark,
    secondary = BrandAccent,
    tertiary = VoiceColor,
)

@Composable
fun TheOneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
