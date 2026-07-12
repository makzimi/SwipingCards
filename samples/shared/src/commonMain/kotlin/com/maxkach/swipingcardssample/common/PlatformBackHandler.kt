package com.maxkach.swipingcardssample.common

import androidx.compose.runtime.Composable

/**
 * Handles the platform hardware/system back action.
 * Android delegates to the activity back dispatcher; iOS is a no-op (each screen
 * provides an in-UI back button).
 */
@Composable
internal expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
