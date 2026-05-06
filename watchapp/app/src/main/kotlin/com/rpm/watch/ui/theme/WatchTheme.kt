package com.rpm.watch.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun WatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
