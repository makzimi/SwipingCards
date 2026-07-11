package com.maxkach.swipingcardssample.dating

import androidx.compose.ui.graphics.Color
import com.maxkach.swipingcards.SwipeDirection
import com.maxkach.swipingcardssample.common.Artwork
import com.maxkach.swipingcardssample.common.isPositiveSwipe

data class DatingProfile(
    val name: String,
    val age: Int,
    val tagline: String,
    val interests: List<String>,
    val artwork: Artwork,
)

private fun placeholder(seed: Long, initials: String, name: String) =
    Artwork.Placeholder(Color(seed), initials, "Portrait of $name")

// Swap each `artwork` to Artwork.Image(R.drawable.dating_<name>, "Portrait of <Name>")
// once the generated 2:3 .webp files land in res/drawable/.
val datingProfiles: List<DatingProfile> = listOf(
    DatingProfile("Mira", 27, "Grows plants and playlists in equal measure.",
        listOf("Botany", "Vinyl", "Baking"), placeholder(0xFF6DBE8A, "M", "Mira")),
    DatingProfile("Rowan", 29, "Bouldering by day, terrible puns always.",
        listOf("Climbing", "Puns", "Coffee"), placeholder(0xFFE0894C, "R", "Rowan")),
    DatingProfile("Kai", 26, "Jazz drummer chasing the perfect late set.",
        listOf("Jazz", "Drums", "Ramen"), placeholder(0xFF5B6DC6, "K", "Kai")),
    DatingProfile("Sol", 31, "Throws clay, brews tea, keeps it calm.",
        listOf("Ceramics", "Tea", "Hiking"), placeholder(0xFFC9A66B, "S", "Sol")),
    DatingProfile("Nova", 28, "Ships indie games and sci-fi opinions.",
        listOf("Gamedev", "Sci-fi", "Synths"), placeholder(0xFFB06CC6, "N", "Nova")),
)

fun datingEventLabel(direction: SwipeDirection, name: String): String =
    if (isPositiveSwipe(direction)) "Liked $name" else "Passed $name"
