package com.maxkach.swipingcardssample.dating

import com.maxkach.swipingcards.SwipeDirection
import com.maxkach.swipingcardssample.common.Artwork
import com.maxkach.swipingcardssample.common.isPositiveSwipe
import com.maxkach.swipingcardssample.resources.Res
import com.maxkach.swipingcardssample.resources.dating_kai
import com.maxkach.swipingcardssample.resources.dating_mira
import com.maxkach.swipingcardssample.resources.dating_nova
import com.maxkach.swipingcardssample.resources.dating_rowan
import com.maxkach.swipingcardssample.resources.dating_sol

data class DatingProfile(
    val name: String,
    val age: Int,
    val tagline: String,
    val interests: List<String>,
    val artwork: Artwork,
)

// Artwork is bundled as 2:3 .webp in res/drawable/ (dating_<name>).
val datingProfiles: List<DatingProfile> = listOf(
    DatingProfile("Mira", 27, "Grows plants and playlists in equal measure.",
        listOf("Botany", "Vinyl", "Baking"),
        Artwork.Image(Res.drawable.dating_mira, "Portrait of Mira")),
    DatingProfile("Rowan", 29, "Bouldering by day, terrible puns always.",
        listOf("Climbing", "Puns", "Coffee"),
        Artwork.Image(Res.drawable.dating_rowan, "Portrait of Rowan")),
    DatingProfile("Kai", 26, "Jazz drummer chasing the perfect late set.",
        listOf("Jazz", "Drums", "Ramen"),
        Artwork.Image(Res.drawable.dating_kai, "Portrait of Kai")),
    DatingProfile("Sol", 31, "Throws clay, brews tea, keeps it calm.",
        listOf("Ceramics", "Tea", "Hiking"),
        Artwork.Image(Res.drawable.dating_sol, "Portrait of Sol")),
    DatingProfile("Nova", 28, "Ships indie games and sci-fi opinions.",
        listOf("Gamedev", "Sci-fi", "Synths"),
        Artwork.Image(Res.drawable.dating_nova, "Portrait of Nova")),
)

fun datingEventLabel(direction: SwipeDirection, name: String): String =
    if (isPositiveSwipe(direction)) "Liked $name" else "Passed $name"
