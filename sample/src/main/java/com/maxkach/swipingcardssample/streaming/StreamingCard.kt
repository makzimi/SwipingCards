package com.maxkach.swipingcardssample.streaming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxkach.swipingcardssample.common.ArtworkImage

private val netflixRed = Color(0xFFE50914)

@Composable
fun StreamingCard(show: StreamingTitle, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier.fillMaxSize().clip(shape).background(Color.Black)) {
        ArtworkImage(show.artwork, Modifier.fillMaxSize())
        // Bottom scrim so the title, tags, and buttons stay legible over any poster.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.4f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.95f),
                ),
            ),
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            show.badge?.let { Badge(it) }
            Text(
                show.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                show.genres.joinToString("  •  "),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PillButton(
                    label = "Play",
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black) },
                    background = Color.White,
                    textColor = Color.Black,
                )
                PillButton(
                    label = "My List",
                    icon = { Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White) },
                    background = Color.White.copy(alpha = 0.22f),
                    textColor = Color.White,
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text.uppercase(),
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(netflixRed)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun RowScope.PillButton(
    label: String,
    icon: @Composable () -> Unit,
    background: Color,
    textColor: Color,
) {
    Row(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
