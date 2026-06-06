package com.rpm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Light palette ─────────────────────────────────────────────────────────
private val Blue700      = Color(0xFF1565C0)
private val Blue900      = Color(0xFF0D47A1)
private val Teal600      = Color(0xFF00897B)
private val LightBg      = Color(0xFFF4F6FB)
private val LightVariant = Color(0xFFE4EAF5)
private val Red800       = Color(0xFFC62828)

// ── Dark palette ──────────────────────────────────────────────────────────
private val Blue200      = Color(0xFF90CAF9)
private val Teal200      = Color(0xFF80CBC4)
private val DarkBg       = Color(0xFF0F1117)
private val DarkSurface  = Color(0xFF1A1D27)
private val DarkVariant  = Color(0xFF252A3A)
private val Red300       = Color(0xFFEF9A9A)

private val LightColors = lightColorScheme(
    primary            = Blue700,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFD3E4FF),
    onPrimaryContainer = Blue900,
    secondary          = Teal600,
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFCCEFEB),
    onSecondaryContainer = Color(0xFF003731),
    background         = LightBg,
    surface            = Color.White,
    surfaceVariant     = LightVariant,
    onSurface          = Color(0xFF111827),
    onSurfaceVariant   = Color(0xFF6B7280),
    outline            = Color(0xFFD1D5DB),
    error              = Red800,
    onError            = Color.White,
)

private val DarkColors = darkColorScheme(
    primary            = Blue200,
    onPrimary          = Color(0xFF003060),
    primaryContainer   = Color(0xFF00448C),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary          = Teal200,
    onSecondary        = Color(0xFF003731),
    secondaryContainer = Color(0xFF005147),
    onSecondaryContainer = Teal200,
    background         = DarkBg,
    surface            = DarkSurface,
    surfaceVariant     = DarkVariant,
    onSurface          = Color(0xFFE6EBF4),
    onSurfaceVariant   = Color(0xFF9BA3AF),
    outline            = Color(0xFF374151),
    error              = Red300,
    onError            = Color(0xFF690000),
)

@Composable
fun RpmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = Typography(),
        content     = content,
    )
}
