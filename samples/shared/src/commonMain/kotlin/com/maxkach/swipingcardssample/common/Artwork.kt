package com.maxkach.swipingcardssample.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Card artwork. [Image] renders a Compose Multiplatform drawable resource;
 * [Placeholder] renders a gradient with initials.
 */
sealed interface Artwork {
    val contentDescription: String

    data class Placeholder(
        val seed: Color,
        val initials: String,
        override val contentDescription: String,
    ) : Artwork

    data class Image(
        val resource: DrawableResource,
        override val contentDescription: String,
    ) : Artwork
}

@Composable
fun ArtworkImage(artwork: Artwork, modifier: Modifier = Modifier) {
    when (artwork) {
        is Artwork.Image -> Image(
            painter = painterResource(artwork.resource),
            contentDescription = artwork.contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )

        is Artwork.Placeholder -> PlaceholderArt(artwork, modifier)
    }
}

@Composable
private fun PlaceholderArt(placeholder: Artwork.Placeholder, modifier: Modifier) {
    val darker = Color(
        red = placeholder.seed.red * 0.55f,
        green = placeholder.seed.green * 0.55f,
        blue = placeholder.seed.blue * 0.55f,
        alpha = 1f,
    )
    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(placeholder.seed, darker)))
            .semantics { contentDescription = placeholder.contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = placeholder.initials,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp,
        )
        Icon(
            imageVector = Icons.Outlined.BrokenImage,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(20.dp),
        )
    }
}
