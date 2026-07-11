package com.maxkach.swipingcardssample.streaming

import androidx.compose.ui.graphics.Color
import com.maxkach.swipingcards.SwipeDirection
import com.maxkach.swipingcardssample.common.Artwork
import com.maxkach.swipingcardssample.common.isPositiveSwipe

/**
 * A streaming title for the Netflix-style demo. [genres] renders as a dot-separated
 * tag row; [badge] is an optional red flag (e.g. "TOP 10"). Titles are used as
 * nominative references in this personal UI demo; artwork is original mood/genre art,
 * not official posters.
 */
data class StreamingTitle(
    val title: String,
    val genres: List<String>,
    val badge: String?,
    val artwork: Artwork,
)

private fun placeholder(seed: Long, initials: String, title: String) =
    Artwork.Placeholder(Color(seed), initials, "Poster art for $title")

// Swap each `artwork` to Artwork.Image(R.drawable.show_<slug>, "Poster art for <Title>")
// once the generated 2:3 .webp posters land in res/drawable/ (slugs in the comments).
val streamingTitles: List<StreamingTitle> = listOf(
    StreamingTitle("Stranger Things", listOf("Sci-Fi", "Horror", "80s", "Mystery"),
        "TOP 10", placeholder(0xFF7A1E1E, "ST", "Stranger Things")), // show_stranger_things
    StreamingTitle("Squid Game", listOf("Thriller", "Survival", "Drama"),
        "#1 in TV", placeholder(0xFF1E6E5A, "SG", "Squid Game")), // show_squid_game
    StreamingTitle("The Crown", listOf("Drama", "History", "Royalty"),
        "AWARD WINNER", placeholder(0xFF2C3E70, "TC", "The Crown")), // show_the_crown
    StreamingTitle("Dark", listOf("Sci-Fi", "Mystery", "Thriller"),
        null, placeholder(0xFF25303A, "DK", "Dark")), // show_dark
    StreamingTitle("Mindhunter", listOf("Crime", "Psychological", "Drama"),
        "MOST LIKED", placeholder(0xFF3A2E22, "MH", "Mindhunter")), // show_mindhunter
    StreamingTitle("KPop Demon Hunters", listOf("Animation", "Action", "Fantasy"),
        "NEW", placeholder(0xFF6E2A7A, "KP", "KPop Demon Hunters")), // show_kpop_demon_hunters
)

fun streamingEventLabel(direction: SwipeDirection, title: String): String =
    if (isPositiveSwipe(direction)) "Added $title to My List" else "Skipped $title"
