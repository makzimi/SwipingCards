package com.maxkach.swipingcardssample.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.maxkach.swipingcardssample.bank.BankExampleScreen
import com.maxkach.swipingcardssample.dating.DatingExampleScreen
import com.maxkach.swipingcardssample.dnd.DndExampleScreen

/**
 * Hoisted-state navigator. `null` current = gallery. Each example screen owns its own
 * deck + history state; leaving disposes it, so demos can't corrupt each other's state.
 */
@Composable
fun GalleryApp() {
    var current by remember { mutableStateOf<Destination?>(null) }
    val back = { current = null }

    BackHandler(enabled = current != null, onBack = back)

    when (current) {
        null -> GalleryScreen(onOpen = { current = it })
        Destination.Dating -> DatingExampleScreen(onBack = back)
        Destination.Bank -> BankExampleScreen(onBack = back)
        Destination.Dnd -> DndExampleScreen(onBack = back)
    }
}
