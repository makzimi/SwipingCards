package com.maxkach.swipingcardssample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Framework entry point consumed by the iOS app's SwiftUI wrapper. */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
