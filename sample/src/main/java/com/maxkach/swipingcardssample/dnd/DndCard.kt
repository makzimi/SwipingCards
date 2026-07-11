package com.maxkach.swipingcardssample.dnd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxkach.swipingcards.SwipeDirection
import com.maxkach.swipingcardssample.common.ActionHints
import com.maxkach.swipingcardssample.common.Artwork
import com.maxkach.swipingcardssample.common.ArtworkImage
import com.maxkach.swipingcardssample.common.isPositiveSwipe

data class DndCard(
    val name: String,
    val role: String,
    val hp: Int,
    val ac: Int,
    val cr: String,
    val str: Int,
    val dex: Int,
    val flavor: String,
    val artwork: Artwork,
)

private val parchment = Color(0xFFEFE3C8)
private val ink = Color(0xFF2B2118)

private fun art(seed: Long, initials: String, name: String) =
    Artwork.Placeholder(Color(seed), initials, "Illustration of $name")

// A deliberately large roster (10) so the deck shows a deep hidden queue cycling
// through, not just the infinite loop. Swap each `artwork` to
// Artwork.Image(R.drawable.dnd_<slug>, "Illustration of <Name>") once the generated
// 1:1 .webp files land in res/drawable/ (slugs match the filenames in the comments).
val dndCards: List<DndCard> = listOf(
    DndCard("Lila Underbough", "Halfling Rogue", 22, 15, "3", 9, 17,
        "Never met a lock she liked.", art(0xFF7A5C3E, "L", "Lila Underbough")), // dnd_rogue
    DndCard("Kaelen Ashborn", "Tiefling Warlock", 27, 12, "4", 10, 14,
        "Made a deal she intends to break.", art(0xFF6B2D8C, "K", "Kaelen Ashborn")), // dnd_warlock
    DndCard("Gruk the Unbroken", "Half-Orc Barbarian", 45, 13, "5", 18, 12,
        "Anger is a renewable resource.", art(0xFF3E6B2D, "G", "Gruk the Unbroken")), // dnd_barbarian
    DndCard("Sylvara Nightbreeze", "Elf Ranger", 30, 14, "4", 12, 16,
        "Two arrows already in the air.", art(0xFF2D5C6B, "S", "Sylvara Nightbreeze")), // dnd_ranger
    DndCard("Ser Aldric Vane", "Human Fighter", 38, 18, "5", 16, 11,
        "Holds the line, every time.", art(0xFFB5952F, "A", "Ser Aldric Vane")), // dnd_fighter
    DndCard("Brother Halden", "Dwarf Cleric", 34, 18, "4", 14, 10,
        "Heals hard, hits harder.", art(0xFFC9A43A, "H", "Brother Halden")), // dnd_cleric
    DndCard("Fenn Quickstring", "Half-Elf Bard", 26, 13, "3", 10, 15,
        "Every tavern owes him a favor.", art(0xFF9C4FA8, "F", "Fenn Quickstring")), // dnd_bard
    DndCard("Ashmaw", "Young Red Dragon", 90, 18, "7", 19, 10,
        "Counts your coins while you sleep.", art(0xFF9E2B1E, "A", "Ashmaw the dragon")), // dnd_dragon
    DndCard("Bramblebeak", "Owlbear", 59, 13, "3", 20, 12,
        "Hoots first, mauls second.", art(0xFF6E5A34, "B", "Bramblebeak the owlbear")), // dnd_owlbear
    DndCard("Gorehide", "Cave Troll", 84, 15, "5", 18, 13,
        "Regrets nothing, regenerates everything.", art(0xFF4A5E3A, "G", "Gorehide the troll")), // dnd_troll
)

fun dndEventLabel(direction: SwipeDirection, name: String): String =
    if (isPositiveSwipe(direction)) "Recruited $name" else "Rejected $name"

@Composable
fun DndCardView(card: DndCard, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(parchment)
            .border(3.dp, ink.copy(alpha = 0.4f), shape)
            .padding(10.dp),
    ) {
        // Framed square art window — takes the leftover space above the stat
        // block and stays square by keying off the available height, so the
        // text/stat/hints below always fit (no clipping on shorter cards).
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, ink.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            ) {
                ArtworkImage(card.artwork, Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.padding(top = 8.dp))
        Text(card.name, color = ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(card.role, color = ink.copy(alpha = 0.8f), fontSize = 12.sp)
        Spacer(Modifier.padding(top = 6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("HP", card.hp.toString())
            Stat("AC", card.ac.toString())
            Stat("CR", card.cr)
            Stat("STR", card.str.toString())
            Stat("DEX", card.dex.toString())
        }
        Spacer(Modifier.padding(top = 6.dp))
        Text(card.flavor, color = ink.copy(alpha = 0.7f), fontSize = 11.sp)
        ActionHints(negative = "Reject", positive = "Recruit", contentColor = ink)
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = ink.copy(alpha = 0.6f), fontSize = 9.sp, textAlign = TextAlign.Center)
        Text(value, color = ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
