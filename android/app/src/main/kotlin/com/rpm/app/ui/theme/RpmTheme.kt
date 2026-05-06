package com.rpm.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary    = Color(0xFF1565C0)   // Deep Blue
private val OnPrimary  = Color.White
private val Secondary  = Color(0xFF00897B)   // Teal
private val Background = Color(0xFFF5F5F5)
private val Surface    = Color.White
private val Error      = Color(0xFFC62828)

private val LightColors = lightColorScheme(
    primary         = Primary,
    onPrimary       = OnPrimary,
    secondary       = Secondary,
    background      = Background,
    surface         = Surface,
    error           = Error
)

@Composable
fun RpmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography  = Typography(),
        content     = content
    )
}
