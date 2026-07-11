package com.maxkach.swipingcardssample.streaming

import com.maxkach.swipingcards.SwipeDirection
import com.maxkach.swipingcardssample.R
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

// Artwork is bundled as 2:3 .webp posters in res/drawable/ (show_<slug>).
val streamingTitles: List<StreamingTitle> = listOf(
    StreamingTitle("Stranger Things", listOf("Sci-Fi", "Horror", "80s", "Mystery"),
        "TOP 10", Artwork.Image(R.drawable.show_stranger_things, "Poster art for Stranger Things")),
    StreamingTitle("Squid Game", listOf("Thriller", "Survival", "Drama"),
        "#1 in TV", Artwork.Image(R.drawable.show_squid_game, "Poster art for Squid Game")),
    StreamingTitle("The Crown", listOf("Drama", "History", "Royalty"),
        "AWARD WINNER", Artwork.Image(R.drawable.show_the_crown, "Poster art for The Crown")),
    StreamingTitle("Dark", listOf("Sci-Fi", "Mystery", "Thriller"),
        null, Artwork.Image(R.drawable.show_dark, "Poster art for Dark")),
    StreamingTitle("Mindhunter", listOf("Crime", "Psychological", "Drama"),
        "MOST LIKED", Artwork.Image(R.drawable.show_mindhunter, "Poster art for Mindhunter")),
    StreamingTitle("KPop Demon Hunters", listOf("Animation", "Action", "Fantasy"),
        "NEW", Artwork.Image(R.drawable.show_kpop_demon_hunters, "Poster art for KPop Demon Hunters")),
)

fun streamingEventLabel(direction: SwipeDirection, title: String): String =
    if (isPositiveSwipe(direction)) "Added $title to My List" else "Skipped $title"
