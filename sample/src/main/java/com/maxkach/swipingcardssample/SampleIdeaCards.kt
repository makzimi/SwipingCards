package com.maxkach.swipingcardssample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class IdeaCard(
    val title: String,
    val description: String,
    val reason: String,
    val backgroundColor: Color,
    val orbColor: Color
)

val sampleIdeas = listOf(
    IdeaCard(
        title = "Mindful Moments",
        description = "A daily prompt app for reflection and gratitude.",
        reason = "Because you love blending practicality with creativity in daily tasks.",
        backgroundColor = Color(0xFFCBB8DC),
        orbColor = Color(0xFF9B8AB8)
    ),
    IdeaCard(
        title = "Idea Exchange",
        description = "A collaborative space for brainstorming and sharing ideas.",
        reason = "Because you enjoy exploring new ideas and sharing them with others.",
        backgroundColor = Color(0xFF4D8DA0),
        orbColor = Color(0xFF7BB5C6)
    ),
    IdeaCard(
        title = "Book Buddy",
        description = "A personalized book recommendation tool for avid readers.",
        reason = "Because you appreciate a good story and love discovering new books.",
        backgroundColor = Color(0xFF8E4CA8),
        orbColor = Color(0xFFB07CC6)
    ),
    IdeaCard(
        title = "Wellness Tracker",
        description = "A holistic health tracking app for mind and body.",
        reason = "Because you value balance and staying in tune with your well-being.",
        backgroundColor = Color(0xFF3AAF96),
        orbColor = Color(0xFF6DD5C3)
    ),
    IdeaCard(
        title = "Recipe Remix",
        description = "Turns the ingredients you have into dinner you'll love.",
        reason = "Because you like improvising and hate letting good food go to waste.",
        backgroundColor = Color(0xFFE08A5C),
        orbColor = Color(0xFFF0B08A)
    ),
    IdeaCard(
        title = "Focus Flow",
        description = "A gentle pomodoro timer that learns your best working hours.",
        reason = "Because you do your finest work in long, undistracted stretches.",
        backgroundColor = Color(0xFF3F5B8C),
        orbColor = Color(0xFF7189B8)
    ),
    IdeaCard(
        title = "Green Thumb",
        description = "Plant care reminders tuned to each species in your home.",
        reason = "Because you keep adopting plants and want them all to thrive.",
        backgroundColor = Color(0xFF5BA85C),
        orbColor = Color(0xFF8CCB8D)
    ),
    IdeaCard(
        title = "Sound Escape",
        description = "Layered ambient soundscapes for focus, rest, and sleep.",
        reason = "Because the right background sound changes how a room feels.",
        backgroundColor = Color(0xFF7A4CA8),
        orbColor = Color(0xFFA57CC6)
    ),
    IdeaCard(
        title = "Budget Buddy",
        description = "Friendly weekly money check-ins without the spreadsheets.",
        reason = "Because you want clarity on spending without the busywork.",
        backgroundColor = Color(0xFF2E9E8F),
        orbColor = Color(0xFF63C7B8)
    ),
    IdeaCard(
        title = "Trailblazer",
        description = "Discovers nearby hikes matched to your time and energy.",
        reason = "Because a good trail is your favorite way to reset.",
        backgroundColor = Color(0xFFB5763A),
        orbColor = Color(0xFFD6A06B)
    ),
)

@Composable
fun IdeaCardView(
    idea: IdeaCard,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(32.dp)
    val textColor = if (idea.backgroundColor.luminance() > 0.4f)
        Color(0xFF1A1A2E) else Color.White

    Column(
        modifier = modifier
            .fillMaxSize()
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = idea.backgroundColor.copy(alpha = 0.3f),
                spotColor = idea.backgroundColor.copy(alpha = 0.3f)
            )
            .clip(shape)
            .background(idea.backgroundColor)
            .padding(horizontal = 20.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(8.dp))
        GlossyOrb(
            color = idea.orbColor,
            size = 92.dp,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = idea.title,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = idea.description,
            color = textColor,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = idea.reason,
            color = textColor.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GlossyOrb(color: Color, size: Dp, modifier: Modifier = Modifier) {
    val sizePx = with(LocalDensity.current) { size.toPx() }
    val darkerColor = Color(
        red = (color.red * 0.6f).coerceIn(0f, 1f),
        green = (color.green * 0.6f).coerceIn(0f, 1f),
        blue = (color.blue * 0.6f).coerceIn(0f, 1f),
        alpha = 1f
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = 0.85f),
                        0.4f to color,
                        1.0f to darkerColor
                    ),
                    center = Offset(sizePx * 0.3f, sizePx * 0.3f),
                    radius = sizePx * 0.85f
                )
            )
    )
}

@Preview(showBackground = true, widthDp = 280, heightDp = 380)
@Composable
private fun IdeaCardViewPreview() {
    IdeaCardView(
        idea = sampleIdeas.first(),
        modifier = Modifier.padding(16.dp)
    )
}
