package com.shvertex.casinotoolspro.ui.utilities

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shvertex.casinotoolspro.core.ProbabilityMath
import com.shvertex.casinotoolspro.theme.*

// ── Card Model ────────────────────────────────────────────────────────────────

enum class Suit(val symbol: String, val isRed: Boolean) {
    SPADES("♠", false), CLUBS("♣", false),
    HEARTS("♥", true),  DIAMONDS("♦", true)
}

data class PlayingCard(val rank: String, val suit: Suit) {
    val value: Int get() = when (rank) {
        "A"  -> 11
        "K", "Q", "J", "10" -> 10
        else -> rank.toIntOrNull() ?: 0
    }
    val displayName get() = "$rank${suit.symbol}"
}

val ALL_RANKS = listOf("A","2","3","4","5","6","7","8","9","10","J","Q","K")

// ── Basic Strategy Engine ─────────────────────────────────────────────────────

enum class BJAction(val label: String, val color: Color) {
    HIT("HIT", CTPColors.Dice),
    STAND("STAND", CTPColors.Green),
    DOUBLE("DOUBLE DOWN", CTPColors.Gold),
    SPLIT("SPLIT", CTPColors.Keno),
    SURRENDER("SURRENDER", CTPColors.Red)
}

fun basicStrategy(
    playerCards: List<PlayingCard>,
    dealerUpcard: PlayingCard?,
    canDouble: Boolean = true,
    canSplit: Boolean  = true,
    canSurrender: Boolean = false
): Pair<BJAction, String> {
    if (dealerUpcard == null || playerCards.size < 2) return BJAction.HIT to "Add cards to get advice"

    val dVal = dealerUpcard.value
    val isPair = playerCards.size == 2 && playerCards[0].rank == playerCards[1].rank

    // Handle Aces in hand
    fun handValue(): Pair<Int, Boolean> {
        var total = 0; var aces = 0
        playerCards.forEach { c -> if (c.rank == "A") aces++ else total += c.value }
        total += aces
        var soft = aces > 0
        while (total > 21 && aces > 0) { total -= 10; aces--; if (aces == 0) soft = false }
        return total to soft
    }

    val (total, isSoft) = handValue()

    // ── Pair splitting ───────────────────────────────────────────────────────
    if (isPair && canSplit) {
        val rank = playerCards[0].rank
        val action: BJAction? = when (rank) {
            "A"  -> BJAction.SPLIT
            "8"  -> BJAction.SPLIT
            "9"  -> if (dVal in listOf(7, 10, 11)) null else BJAction.SPLIT
            "7"  -> if (dVal <= 7) BJAction.SPLIT else null
            "6"  -> if (dVal in 2..6) BJAction.SPLIT else null
            "5"  -> null  // treat as hard 10
            "4"  -> if (dVal in 5..6) BJAction.SPLIT else null
            "3","2" -> if (dVal in 2..7) BJAction.SPLIT else null
            else -> null  // 10, J, Q, K — never split
        }
        if (action != null) {
            val reason = when (rank) {
                "A" -> "Always split Aces — two chances at 21"
                "8" -> "Always split 8s — 16 is the worst hand"
                "9" -> "Split 9s against weak dealer (not 7, 10, Ace)"
                "7" -> "Split 7s against dealer 2-7"
                "6" -> "Split 6s against dealer 2-6 only"
                "3","2" -> "Split small pairs vs dealer 2-7"
                else -> "Split recommended"
            }
            return action to reason
        }
    }

    // ── Soft hands ───────────────────────────────────────────────────────────
    if (isSoft) {
        return when (total) {
            20 -> BJAction.STAND to "Soft 20 (A-9): Always stand — best soft hand"
            19 -> if (dVal == 6 && canDouble) BJAction.DOUBLE to "Soft 19 vs 6: Double for maximum value"
                  else BJAction.STAND to "Soft 19: Stand — strong hand"
            18 -> when {
                dVal in 2..6 && canDouble -> BJAction.DOUBLE to "Soft 18 vs 2-6: Double to exploit dealer weakness"
                dVal in 7..8 -> BJAction.STAND to "Soft 18 vs 7-8: Stand — ties or beats dealer"
                else -> BJAction.HIT to "Soft 18 vs 9/10/A: Hit — dealer likely has 19+"
            }
            17 -> if (dVal in 3..6 && canDouble) BJAction.DOUBLE to "Soft 17 vs 3-6: Double — dealer weak"
                  else BJAction.HIT to "Soft 17: Hit — can't bust, need improvement"
            16 -> if (dVal in 4..6 && canDouble) BJAction.DOUBLE to "Soft 16 vs 4-6: Double — dealer very weak"
                  else BJAction.HIT to "Soft 16: Hit — low total needs improvement"
            15 -> if (dVal in 4..6 && canDouble) BJAction.DOUBLE to "Soft 15 vs 4-6: Double"
                  else BJAction.HIT to "Soft 15: Hit"
            13,14 -> if (dVal in 5..6 && canDouble) BJAction.DOUBLE to "Soft ${total} vs 5-6: Double"
                     else BJAction.HIT to "Soft $total: Hit"
            else -> BJAction.HIT to "Hit — soft hand needs improvement"
        }
    }

    // ── Surrender ────────────────────────────────────────────────────────────
    if (canSurrender) {
        if (total == 16 && dVal in listOf(9, 10, 11)) return BJAction.SURRENDER to "Hard 16 vs 9/10/A: Surrender saves half bet"
        if (total == 15 && dVal == 10) return BJAction.SURRENDER to "Hard 15 vs 10: Surrender — expected loss > 50%"
    }

    // ── Hard hands ───────────────────────────────────────────────────────────
    return when {
        total >= 17 -> BJAction.STAND to "Hard $total: Always stand — busting risk too high"
        total == 16 -> if (dVal in 2..6) BJAction.STAND to "Hard 16 vs 2-6: Stand — dealer likely busts"
                       else BJAction.HIT to "Hard 16 vs 7+: Hit — take the risk"
        total == 15 -> if (dVal in 2..6) BJAction.STAND to "Hard 15 vs 2-6: Stand"
                       else BJAction.HIT to "Hard 15 vs 7+: Hit"
        total == 14 -> if (dVal in 2..6) BJAction.STAND to "Hard 14 vs 2-6: Stand"
                       else BJAction.HIT to "Hard 14 vs 7+: Hit"
        total == 13 -> if (dVal in 2..6) BJAction.STAND to "Hard 13 vs 2-6: Stand"
                       else BJAction.HIT to "Hard 13 vs 7+: Hit"
        total == 12 -> if (dVal in 4..6) BJAction.STAND to "Hard 12 vs 4-6: Stand — dealer busts 40%"
                       else BJAction.HIT to "Hard 12 vs 2-3/7+: Hit"
        total == 11 -> if (canDouble) BJAction.DOUBLE to "Hard 11: Always double — best doubling hand"
                       else BJAction.HIT to "Hard 11: Hit — strong position"
        total == 10 -> if (dVal in 2..9 && canDouble) BJAction.DOUBLE to "Hard 10 vs 2-9: Double"
                       else BJAction.HIT to "Hard 10 vs 10/A: Hit"
        total == 9  -> if (dVal in 3..6 && canDouble) BJAction.DOUBLE to "Hard 9 vs 3-6: Double"
                       else BJAction.HIT to "Hard 9: Hit"
        else        -> BJAction.HIT to "Hard $total: Always hit — total too low"
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun BlackjackScreen(onBack: () -> Unit) {
    var playerCards  by remember { mutableStateOf<List<PlayingCard>>(emptyList()) }
    var dealerCards  by remember { mutableStateOf<List<PlayingCard>>(emptyList()) }
    var selectingFor by remember { mutableStateOf("player") }  // "player" or "dealer"
    var canDouble    by remember { mutableStateOf(true) }
    var canSurrender by remember { mutableStateOf(false) }
    var decksStr     by remember { mutableStateOf("6") }
    var hitSoft17    by remember { mutableStateOf(true) }
    var runningCount by remember { mutableIntStateOf(0) }
    var decksRemainingStr by remember { mutableStateOf("6.0") }

    val dealerUpcard = dealerCards.firstOrNull()
    val canSplit     = playerCards.size == 2

    val (action, reason) = remember(playerCards, dealerCards, canDouble, canSurrender) {
        basicStrategy(playerCards, dealerUpcard, canDouble, canSplit && true, canSurrender)
    }

    // Player hand value
    fun calcHandValue(cards: List<PlayingCard>): Pair<Int, Boolean> {
        var total = 0; var aces = 0
        cards.forEach { c -> if (c.rank == "A") aces++ else total += c.value }
        total += aces; var soft = aces > 0
        while (total > 21 && aces > 0) { total -= 10; aces--; if (aces == 0) soft = false }
        return total to soft
    }

    val (playerTotal, playerSoft) = calcHandValue(playerCards)
    val (dealerTotal, _) = calcHandValue(dealerCards)

    // Hi-Lo count update
    fun hiLoValue(rank: String): Int = when (rank) {
        in listOf("2","3","4","5","6") -> +1
        in listOf("10","J","Q","K","A") -> -1
        else -> 0
    }

    fun addCard(rank: String, suit: Suit) {
        val card = PlayingCard(rank, suit)
        if (selectingFor == "player") playerCards = playerCards + card
        else dealerCards = dealerCards + card
        runningCount += hiLoValue(rank)
    }

    Column(Modifier.fillMaxSize().background(CTPColors.Black)) {
        ScreenHeader("Blackjack Hub", "Interactive basic strategy advisor", CTPColors.Limbo, onBack)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Table ─────────────────────────────────────────────────────────
            BlackjackTable(
                playerCards  = playerCards,
                dealerCards  = dealerCards,
                playerTotal  = playerTotal,
                playerSoft   = playerSoft,
                dealerTotal  = dealerTotal,
                onClearPlayer = { playerCards = emptyList() },
                onClearDealer = { dealerCards = emptyList() }
            )

            // ── Strategy Advice ───────────────────────────────────────────────
            if (playerCards.size >= 2 && dealerCards.isNotEmpty()) {
                AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                    StrategyAdviceCard(action = action, reason = reason)
                }
            } else {
                CTPCard(accentColor = CTPColors.Limbo, showAccent = false) {
                    Text(
                        if (playerCards.isEmpty() && dealerCards.isEmpty())
                            "Tap 'Player' or 'Dealer' below, then pick cards from the selector."
                        else if (dealerCards.isEmpty()) "Now add the dealer's upcard."
                        else "Add your second card.",
                        style = CTPType.BodyMedium, color = CTPColors.TextSecondary
                    )
                }
            }

            // ── Who to Add For ────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { selectingFor = "player" },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = if (selectingFor == "player") CTPColors.Dice else CTPColors.Card),
                    shape   = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) { Text("▶ Player", style = CTPType.LabelLarge, color = if (selectingFor == "player") Color.White else CTPColors.TextMuted) }

                Button(
                    onClick = { selectingFor = "dealer" },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = if (selectingFor == "dealer") CTPColors.Red else CTPColors.Card),
                    shape   = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) { Text("▶ Dealer", style = CTPType.LabelLarge, color = if (selectingFor == "dealer") Color.White else CTPColors.TextMuted) }

                Button(
                    onClick = { playerCards = emptyList(); dealerCards = emptyList() },
                    colors  = ButtonDefaults.buttonColors(containerColor = CTPColors.CardElevated),
                    shape   = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(44.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) { Text("RESET", style = CTPType.LabelLarge, color = CTPColors.TextMuted) }
            }

            // ── Card Selector Grid ────────────────────────────────────────────
            CardSelectorGrid(onCardSelected = { rank ->
                // Default suit based on context (suit doesn't affect basic strategy)
                val suit = if (selectingFor == "player") Suit.SPADES else Suit.HEARTS
                addCard(rank, suit)
            })

            // ── Options ───────────────────────────────────────────────────────
            CTPCard(accentColor = CTPColors.Limbo) {
                Text("TABLE RULES", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
                Spacer(Modifier.height(6.dp))
                ToggleRow("Double Down allowed",  canDouble,    { canDouble = it })
                ToggleRow("Late Surrender allowed", canSurrender, { canSurrender = it })
                ToggleRow("Dealer hits soft 17",  hitSoft17,    { hitSoft17 = it })
                Spacer(Modifier.height(8.dp))
                val decks = decksStr.toIntOrNull()?.coerceIn(1, 8) ?: 6
                val edge  = ProbabilityMath.blackjackHouseEdge(decks, hitSoft17, canDouble, canSurrender)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CTPInput(decksStr, { decksStr = it }, "Decks", Modifier.weight(1f),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("House Edge", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                        Spacer(Modifier.height(8.dp))
                        Text("${"%.2f".format(edge)}%", style = CTPType.MonoLarge, color = CTPColors.Red)
                        Text("RTP: ${"%.2f".format(100.0 - edge)}%", style = CTPType.LabelMedium, color = CTPColors.Green)
                    }
                }
            }

            // ── Card Counter ──────────────────────────────────────────────────
            CardCounterWidget(
                runningCount     = runningCount,
                decksRemaining   = decksRemainingStr.toDoubleOrNull() ?: 6.0,
                onResetCount     = { runningCount = 0 },
                onDecksChange    = { decksRemainingStr = it }
            )

            // ── Basic Strategy reference ──────────────────────────────────────
            BasicStrategyCard()
        }
    }
}

// ── Blackjack Table ───────────────────────────────────────────────────────────

@Composable
fun BlackjackTable(
    playerCards: List<PlayingCard>, dealerCards: List<PlayingCard>,
    playerTotal: Int, playerSoft: Boolean, dealerTotal: Int,
    onClearPlayer: () -> Unit, onClearDealer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.radialGradient(listOf(Color(0xFF1A3A2A), Color(0xFF0D1F14))))
            .border(1.dp, CTPColors.Green.copy(0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Dealer row
            HandRow(
                label   = "DEALER",
                cards   = dealerCards,
                total   = dealerTotal,
                soft    = false,
                color   = CTPColors.Red,
                onClear = onClearDealer
            )

            HorizontalDivider(color = CTPColors.Green.copy(0.2f))

            // Player row
            HandRow(
                label   = "PLAYER",
                cards   = playerCards,
                total   = playerTotal,
                soft    = playerSoft,
                color   = CTPColors.Dice,
                onClear = onClearPlayer
            )
        }
    }
}

@Composable
fun HandRow(label: String, cards: List<PlayingCard>, total: Int, soft: Boolean, color: Color, onClear: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = CTPType.LabelLarge, color = color, letterSpacing = 1.5.sp)
            if (cards.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val totalStr = if (soft && total <= 21) "Soft $total" else "$total"
                    Text(totalStr, style = CTPType.MonoLarge, color = if (total > 21) CTPColors.Red else color)
                    if (total > 21) Text("BUST", style = CTPType.LabelLarge, color = CTPColors.Red)
                    TextButton(onClick = onClear, contentPadding = PaddingValues(4.dp)) {
                        Text("Clear", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        if (cards.isEmpty()) {
            Box(
                Modifier.height(68.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CTPColors.Card.copy(0.5f))
                    .border(1.dp, color.copy(0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) { Text("No cards yet", style = CTPType.BodyMedium, color = CTPColors.TextMuted) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy((-16).dp)) {
                cards.forEachIndexed { i, card ->
                    PlayingCardView(card = card, modifier = Modifier.offset(x = (i * 6).dp))
                }
            }
        }
    }
}

@Composable
fun PlayingCardView(card: PlayingCard, modifier: Modifier = Modifier) {
    val textColor = if (card.suit.isRed) Color(0xFFCC0000) else Color(0xFF111111)
    Box(
        modifier = modifier
            .width(52.dp).height(72.dp)
            .shadow(4.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
            .padding(4.dp)
    ) {
        Text(card.rank, style = CTPType.LabelLarge.copy(fontSize = 14.sp), color = textColor,
            modifier = Modifier.align(Alignment.TopStart))
        Text(card.suit.symbol, style = CTPType.HeadlineMedium.copy(fontSize = 18.sp), color = textColor,
            modifier = Modifier.align(Alignment.Center))
        Text(card.rank, style = CTPType.LabelLarge.copy(fontSize = 14.sp), color = textColor,
            modifier = Modifier.align(Alignment.BottomEnd).rotate(180f))
    }
}

// ── Strategy Advice Card ──────────────────────────────────────────────────────

@Composable
fun StrategyAdviceCard(action: BJAction, reason: String) {
    val pulse = rememberInfiniteTransition(label = "advice_pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "advice_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(action.color.copy(0.2f), CTPColors.Card)))
            .border(2.dp, action.color.copy(alpha), RoundedCornerShape(14.dp))
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("BASIC STRATEGY SAYS", style = CTPType.LabelLarge, color = CTPColors.TextMuted, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text  = action.label,
                style = CTPType.DisplayLarge.copy(fontSize = 40.sp),
                color = action.color.copy(alpha = alpha)
            )
            Spacer(Modifier.height(8.dp))
            Text(reason, style = CTPType.BodyMedium, color = CTPColors.TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

// ── Card Selector Grid ────────────────────────────────────────────────────────

@Composable
fun CardSelectorGrid(onCardSelected: (String) -> Unit) {
    CTPCard(accentColor = CTPColors.Dice) {
        Text("SELECT CARD", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
        Spacer(Modifier.height(8.dp))
        // Rank grid — suits don't affect basic strategy
        ALL_RANKS.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { rank ->
                    val isTen = rank in listOf("10","J","Q","K")
                    Box(
                        modifier = Modifier
                            .weight(1f).height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isTen) CTPColors.Limbo.copy(0.15f) else CTPColors.CardElevated)
                            .border(1.dp, if (rank == "A") CTPColors.Gold.copy(0.6f) else CTPColors.Border, RoundedCornerShape(8.dp))
                            .clickable { onCardSelected(rank) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(rank, style = CTPType.HeadlineMedium.copy(fontSize = 15.sp),
                                color = when {
                                    rank == "A" -> CTPColors.Gold
                                    isTen -> CTPColors.Limbo
                                    else -> CTPColors.TextPrimary
                                })
                            Text("=${if (rank == "A") "11/1" else PlayingCard(rank, Suit.SPADES).value.toString()}",
                                style = CTPType.LabelMedium.copy(fontSize = 9.sp), color = CTPColors.TextMuted)
                        }
                    }
                }
                // Pad last row
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ── Card Counter Widget ───────────────────────────────────────────────────────

@Composable
fun CardCounterWidget(
    runningCount: Int,
    decksRemaining: Double,
    onResetCount: () -> Unit,
    onDecksChange: (String) -> Unit
) {
    val trueCount = if (decksRemaining > 0) runningCount / decksRemaining else 0.0
    val countColor = when {
        trueCount >= 3  -> CTPColors.Green
        trueCount >= 1  -> CTPColors.Dice
        trueCount <= -2 -> CTPColors.Red
        else            -> CTPColors.TextMuted
    }
    val countAdvice = when {
        trueCount >= 4  -> "Strong player advantage — bet MAX"
        trueCount >= 2  -> "Player edge — increase bet size"
        trueCount >= 1  -> "Slight edge — bet a bit more"
        trueCount <= -2 -> "Dealer advantage — bet minimum"
        else            -> "Neutral — standard bet"
    }

    CTPCard(accentColor = CTPColors.Gold) {
        Text("HI-LO CARD COUNTER", style = CTPType.LabelLarge, color = CTPColors.Gold)
        Text("Cards are counted automatically as you add them", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Running Count", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                Text("${if (runningCount >= 0) "+" else ""}$runningCount",
                    style = CTPType.MonoLarge.copy(fontSize = 28.sp), color = countColor)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("True Count", style = CTPType.LabelMedium, color = CTPColors.TextMuted)
                Text("${if (trueCount >= 0) "+" else ""}${"%.2f".format(trueCount)}",
                    style = CTPType.MonoLarge.copy(fontSize = 28.sp), color = countColor)
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CTPInput(decksRemaining.toString(), onDecksChange, "Decks Remaining", Modifier.weight(1f))
            CTPButton("RESET", onResetCount, Modifier.weight(1f).height(50.dp), CTPColors.CardElevated, CTPColors.TextMuted)
        }

        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(countColor.copy(0.1f))
                .border(1.dp, countColor.copy(0.3f), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Text(countAdvice, style = CTPType.BodyMedium, color = countColor, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(6.dp))
        Text("2-6 = +1  |  7-9 = 0  |  10/J/Q/K/A = -1", style = CTPType.LabelMedium, color = CTPColors.TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

// ── Toggle Row ────────────────────────────────────────────────────────────────

@Composable
fun ToggleRow(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style    = CTPType.BodyMedium,
            color    = CTPColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked  = value,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = CTPColors.Limbo,
                checkedTrackColor  = CTPColors.Limbo.copy(alpha = 0.4f)
            )
        )
    }
}

// ── Basic Strategy Quick Reference ────────────────────────────────────────────

@Composable
fun BasicStrategyCard() {
    CTPCard(accentColor = CTPColors.Dice) {
        Text("BASIC STRATEGY QUICK REFERENCE", style = CTPType.LabelLarge, color = CTPColors.TextMuted)
        Spacer(Modifier.height(8.dp))

        val sections = listOf(
            "HARD TOTALS" to listOf(
                "Hard 17+"   to "Always Stand",
                "Hard 16"    to "Stand vs 2-6 · Hit vs 7-A",
                "Hard 13-15" to "Stand vs 2-6 · Hit vs 7-A",
                "Hard 12"    to "Stand vs 4-6 · Hit otherwise",
                "Hard 11"    to "Always Double Down",
                "Hard 10"    to "Double vs 2-9 · Hit vs 10/A",
                "Hard 9"     to "Double vs 3-6 · Hit otherwise",
            ),
            "SOFT TOTALS" to listOf(
                "Soft 20 (A-9)" to "Always Stand",
                "Soft 19 (A-8)" to "Double vs 6 · Stand otherwise",
                "Soft 18 (A-7)" to "Double vs 2-6 · Stand vs 7-8 · Hit vs 9-A",
                "Soft 17 (A-6)" to "Double vs 3-6 · Hit otherwise",
                "Soft 13-16"    to "Double vs 4-6 · Hit otherwise",
            ),
            "PAIRS" to listOf(
                "A/A"     to "Always Split",
                "8/8"     to "Always Split",
                "9/9"     to "Split vs 2-6, 8-9 · Stand vs 7, 10, A",
                "7/7"     to "Split vs 2-7",
                "6/6"     to "Split vs 2-6",
                "5/5"     to "Never Split — treat as Hard 10",
                "4/4"     to "Split vs 5-6 only",
                "2/2,3/3" to "Split vs 2-7",
                "10/10"   to "Never Split",
            )
        )

        sections.forEach { (sectionTitle, rules) ->
            Spacer(Modifier.height(8.dp))
            Text(sectionTitle, style = CTPType.LabelLarge, color = CTPColors.Limbo, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            rules.forEach { (hand, action) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(hand,   style = CTPType.Mono,        color = CTPColors.TextPrimary,   modifier = Modifier.weight(1.2f))
                    Text(action, style = CTPType.LabelMedium, color = CTPColors.TextSecondary, modifier = Modifier.weight(1.8f), textAlign = TextAlign.End)
                }
                CTPDivider(Modifier.padding(vertical = 1.dp))
            }
        }
    }
}
