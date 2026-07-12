package com.maxkach.swipingcardssample.common

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses the in-UI back button; no system back to intercept.
}
