package com.maxkach.swipingcardssample.dnd

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxkach.swipingcards.SwipeDirection
import com.maxkach.swipingcardssample.R
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

private val parchment = Color(0xFFF3E7C7)
private val parchmentEdge = Color(0xFFDCC79A)
private val bannerBand = Color(0xFFE8D6AC)
private val ink = Color(0xFF2B2118)
private val gold = Color(0xFF9A7B3C)

// A deliberately large roster (10) so the deck shows a deep hidden queue cycling
// through, not just the infinite loop. Artwork is bundled as 1:1 .webp in
// res/drawable/ (dnd_<slug>).
val dndCards: List<DndCard> = listOf(
    DndCard("Lila Underbough", "Halfling Rogue", 22, 15, "3", 9, 17,
        "Never met a lock she liked.",
        Artwork.Image(R.drawable.dnd_rogue, "Illustration of Lila Underbough, a Halfling Rogue")),
    DndCard("Kaelen Ashborn", "Tiefling Warlock", 27, 12, "4", 10, 14,
        "Made a deal she intends to break.",
        Artwork.Image(R.drawable.dnd_warlock, "Illustration of Kaelen Ashborn, a Tiefling Warlock")),
    DndCard("Gruk the Unbroken", "Half-Orc Barbarian", 45, 13, "5", 18, 12,
        "Anger is a renewable resource.",
        Artwork.Image(R.drawable.dnd_barbarian, "Illustration of Gruk the Unbroken, a Half-Orc Barbarian")),
    DndCard("Sylvara Nightbreeze", "Elf Ranger", 30, 14, "4", 12, 16,
        "Two arrows already in the air.",
        Artwork.Image(R.drawable.dnd_ranger, "Illustration of Sylvara Nightbreeze, an Elf Ranger")),
    DndCard("Ser Aldric Vane", "Human Fighter", 38, 18, "5", 16, 11,
        "Holds the line, every time.",
        Artwork.Image(R.drawable.dnd_fighter, "Illustration of Ser Aldric Vane, a Human Fighter")),
    DndCard("Brother Halden", "Dwarf Cleric", 34, 18, "4", 14, 10,
        "Heals hard, hits harder.",
        Artwork.Image(R.drawable.dnd_cleric, "Illustration of Brother Halden, a Dwarf Cleric")),
    DndCard("Fenn Quickstring", "Half-Elf Bard", 26, 13, "3", 10, 15,
        "Every tavern owes him a favor.",
        Artwork.Image(R.drawable.dnd_bard, "Illustration of Fenn Quickstring, a Half-Elf Bard")),
    DndCard("Ashmaw", "Young Red Dragon", 90, 18, "7", 19, 10,
        "Counts your coins while you sleep.",
        Artwork.Image(R.drawable.dnd_dragon, "Illustration of Ashmaw, a Young Red Dragon")),
    DndCard("Bramblebeak", "Owlbear", 59, 13, "3", 20, 12,
        "Hoots first, mauls second.",
        Artwork.Image(R.drawable.dnd_owlbear, "Illustration of Bramblebeak, an Owlbear")),
    DndCard("Gorehide", "Cave Troll", 84, 15, "5", 18, 13,
        "Regrets nothing, regenerates everything.",
        Artwork.Image(R.drawable.dnd_troll, "Illustration of Gorehide, a Cave Troll")),
)

fun dndEventLabel(direction: SwipeDirection, name: String): String =
    if (isPositiveSwipe(direction)) "Recruited $name" else "Rejected $name"

@Composable
fun DndCardView(card: DndCard, modifier: Modifier = Modifier) {
    val outerShape = RoundedCornerShape(16.dp)
    val innerShape = RoundedCornerShape(12.dp)
    val artShape = RoundedCornerShape(6.dp)
    Column(
        modifier
            .fillMaxSize()
            .clip(outerShape)
            .background(Brush.verticalGradient(listOf(parchment, parchmentEdge)))
            .border(3.dp, ink.copy(alpha = 0.55f), outerShape) // dark tooled outer edge
            .padding(4.dp)
            .border(1.5.dp, gold, innerShape)                  // bronze inner rule
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Title banner: name + small-caps class, over an ornamental divider.
        Text(
            card.name,
            color = ink,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Text(
            card.role.uppercase(),
            color = gold,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
        )
        OrnamentDivider(Modifier.padding(top = 6.dp, bottom = 8.dp))

        // Framed square art window — a bronze-bordered plate. Keyed off the
        // leftover height so it stays square and the block below never clips.
        Box(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(artShape)
                    .border(3.dp, gold, artShape),
            ) {
                ArtworkImage(card.artwork, Modifier.fillMaxSize())
                // Thin inner shadow line so the art sits inside the plate.
                Box(Modifier.fillMaxSize().border(1.dp, ink.copy(alpha = 0.35f), artShape))
            }
        }

        OrnamentDivider(Modifier.padding(top = 8.dp, bottom = 8.dp))

        // Stat block: a tooled ledger strip with hairline separators.
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(bannerBand.copy(alpha = 0.5f))
                .border(1.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatCell("HP", card.hp.toString(), Modifier.weight(1f))
            StatSeparator()
            StatCell("AC", card.ac.toString(), Modifier.weight(1f))
            StatSeparator()
            StatCell("CR", card.cr, Modifier.weight(1f))
            StatSeparator()
            StatCell("STR", card.str.toString(), Modifier.weight(1f))
            StatSeparator()
            StatCell("DEX", card.dex.toString(), Modifier.weight(1f))
        }

        Text(
            card.flavor,
            color = ink.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        ActionHints(
            negative = "Reject",
            positive = "Recruit",
            contentColor = ink,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = ink.copy(alpha = 0.6f), fontSize = 9.sp, letterSpacing = 1.sp,
            textAlign = TextAlign.Center)
        Text(value, color = ink, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun StatSeparator() {
    Box(Modifier.width(1.dp).height(22.dp).background(gold.copy(alpha = 0.35f)))
}

// A hairline rule with a small center diamond — the printed-card section break.
@Composable
private fun OrnamentDivider(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        val y = size.height / 2f
        val cx = size.width / 2f
        val gap = 7.dp.toPx()
        val d = 4.dp.toPx()
        val stroke = 1.5.dp.toPx()
        drawLine(gold, Offset(0f, y), Offset(cx - gap, y), strokeWidth = stroke)
        drawLine(gold, Offset(cx + gap, y), Offset(size.width, y), strokeWidth = stroke)
        val diamond = Path().apply {
            moveTo(cx, y - d)
            lineTo(cx + d, y)
            lineTo(cx, y + d)
            lineTo(cx - d, y)
            close()
        }
        drawPath(diamond, gold)
    }
}
