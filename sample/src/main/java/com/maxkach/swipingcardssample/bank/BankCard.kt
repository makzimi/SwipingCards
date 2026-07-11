package com.maxkach.swipingcardssample.bank

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxkach.swipingcards.SwipeDirection
import com.maxkach.swipingcardssample.common.ActionHints
import com.maxkach.swipingcardssample.common.isPositiveSwipe

data class BankCard(
    val product: String,
    val last4: String,
    val holder: String,
    val expiry: String,
    val perk: String,
    val gradient: List<Color>,
)

// A deliberately small roster (3) — the "few cards" demo, contrasting the 10-card
// D&D deck. All data is fictional; these are not real cards and imply no real
// payment flow.
val bankCards: List<BankCard> = listOf(
    BankCard("Aurora Everyday", "4242", "A. EXAMPLE", "08/29", "1% back on everything",
        listOf(Color(0xFF3A7BD5), Color(0xFF00D2FF))),
    BankCard("Zephyr Travel", "8817", "A. EXAMPLE", "11/28", "No foreign transaction fees",
        listOf(Color(0xFF654EA3), Color(0xFFEAAFC8))),
    BankCard("Onyx Reserve", "1265", "A. EXAMPLE", "06/30", "Airport lounge access",
        listOf(Color(0xFF232526), Color(0xFF414345))),
)

fun bankEventLabel(direction: SwipeDirection, product: String, last4: String): String =
    if (isPositiveSwipe(direction)) "Selected card ending $last4" else "Skipped $product"

@Composable
fun BankCardView(card: BankCard, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(Brush.linearGradient(card.gradient)),
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(card.product, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("DEMO — NOT A REAL CARD", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
            }
            Spacer(Modifier.height(16.dp))
            // Procedural "chip" — abstract, not a real card network mark.
            Box(
                Modifier
                    .size(width = 40.dp, height = 30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.75f)),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "••••  ••••  ••••  ${card.last4}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CARDHOLDER", color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp)
                    Text(card.holder, color = Color.White, fontSize = 13.sp)
                }
                Column {
                    Text("EXPIRES", color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp)
                    Text(card.expiry, color = Color.White, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(card.perk, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            ActionHints(negative = "Skip", positive = "Select", modifier = Modifier.padding(top = 8.dp))
        }
    }
}
