package com.maxkach.swipingcardssample

import androidx.compose.runtime.Composable
import com.maxkach.swipingcardssample.gallery.GalleryApp
import com.maxkach.swipingcardssample.ui.theme.SwipingCardExampleTheme

/** Shared root of the example gallery, hosted identically on Android and iOS. */
@Composable
fun App() {
    SwipingCardExampleTheme {
        GalleryApp()
    }
}
