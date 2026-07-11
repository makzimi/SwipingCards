package com.maxkach.swipingcardssample.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/** Static "← negative | positive →" hints, since the library exposes no drag overlay. */
@Composable
fun ActionHints(
    negative: String,
    positive: String,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("← $negative", color = contentColor.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
        Text("$positive →", color = contentColor.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
    }
}
