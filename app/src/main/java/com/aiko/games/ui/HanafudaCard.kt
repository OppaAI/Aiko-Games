package com.aiko.games.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aiko.games.R
import com.aiko.games.data.model.KoiCard

/**
 * Real hanafuda faces, bundled offline in res/drawable-nodpi.
 *
 * Art: Louie Mantia, CC BY-SA 4.0 via Wikimedia Commons
 * (see credit in the Koi-Koi rules page).
 * Slot order matches the server deck (month*4 + slot):
 * special, tanzaku (where the month has one), then chaff.
 */
private val MONTH_PREFIX = arrayOf(
    "jan", "feb", "mar", "apr", "may", "jun",
    "jul", "aug", "sep", "oct", "nov", "dec",
)

private fun hanaStem(month: Int, slot: Int): String {
    val stems = when (month) {
        0, 2 -> arrayOf("hikari", "tanzaku", "kasu1", "kasu2")
        7 -> arrayOf("hikari", "tane", "kasu1", "kasu2")
        10 -> arrayOf("hikari", "tane", "tanzaku", "kasu")
        11 -> arrayOf("hikari", "kasu1", "kasu2", "kasu3")
        else -> arrayOf("tane", "tanzaku", "kasu1", "kasu2")
    }
    return "${MONTH_PREFIX[month]}_${stems[slot]}"
}

@DrawableRes
fun hanaRes(month: Int, slot: Int): Int = when (
    hanaStem(month.coerceIn(0, 11), slot.coerceIn(0, 3))
) {
    "jan_hikari" -> R.drawable.hana_jan_hikari
    "jan_tanzaku" -> R.drawable.hana_jan_tanzaku
    "jan_kasu1" -> R.drawable.hana_jan_kasu1
    "jan_kasu2" -> R.drawable.hana_jan_kasu2
    "feb_tane" -> R.drawable.hana_feb_tane
    "feb_tanzaku" -> R.drawable.hana_feb_tanzaku
    "feb_kasu1" -> R.drawable.hana_feb_kasu1
    "feb_kasu2" -> R.drawable.hana_feb_kasu2
    "mar_hikari" -> R.drawable.hana_mar_hikari
    "mar_tanzaku" -> R.drawable.hana_mar_tanzaku
    "mar_kasu1" -> R.drawable.hana_mar_kasu1
    "mar_kasu2" -> R.drawable.hana_mar_kasu2
    "apr_tane" -> R.drawable.hana_apr_tane
    "apr_tanzaku" -> R.drawable.hana_apr_tanzaku
    "apr_kasu1" -> R.drawable.hana_apr_kasu1
    "apr_kasu2" -> R.drawable.hana_apr_kasu2
    "may_tane" -> R.drawable.hana_may_tane
    "may_tanzaku" -> R.drawable.hana_may_tanzaku
    "may_kasu1" -> R.drawable.hana_may_kasu1
    "may_kasu2" -> R.drawable.hana_may_kasu2
    "jun_tane" -> R.drawable.hana_jun_tane
    "jun_tanzaku" -> R.drawable.hana_jun_tanzaku
    "jun_kasu1" -> R.drawable.hana_jun_kasu1
    "jun_kasu2" -> R.drawable.hana_jun_kasu2
    "jul_tane" -> R.drawable.hana_jul_tane
    "jul_tanzaku" -> R.drawable.hana_jul_tanzaku
    "jul_kasu1" -> R.drawable.hana_jul_kasu1
    "jul_kasu2" -> R.drawable.hana_jul_kasu2
    "aug_hikari" -> R.drawable.hana_aug_hikari
    "aug_tane" -> R.drawable.hana_aug_tane
    "aug_kasu1" -> R.drawable.hana_aug_kasu1
    "aug_kasu2" -> R.drawable.hana_aug_kasu2
    "sep_tane" -> R.drawable.hana_sep_tane
    "sep_tanzaku" -> R.drawable.hana_sep_tanzaku
    "sep_kasu1" -> R.drawable.hana_sep_kasu1
    "sep_kasu2" -> R.drawable.hana_sep_kasu2
    "oct_tane" -> R.drawable.hana_oct_tane
    "oct_tanzaku" -> R.drawable.hana_oct_tanzaku
    "oct_kasu1" -> R.drawable.hana_oct_kasu1
    "oct_kasu2" -> R.drawable.hana_oct_kasu2
    "nov_hikari" -> R.drawable.hana_nov_hikari
    "nov_tane" -> R.drawable.hana_nov_tane
    "nov_tanzaku" -> R.drawable.hana_nov_tanzaku
    "nov_kasu" -> R.drawable.hana_nov_kasu
    "dec_hikari" -> R.drawable.hana_dec_hikari
    "dec_kasu1" -> R.drawable.hana_dec_kasu1
    "dec_kasu2" -> R.drawable.hana_dec_kasu2
    "dec_kasu3" -> R.drawable.hana_dec_kasu3
    else -> R.drawable.hana_back
}

private val MONTH_LABEL: List<Pair<String, String>> = listOf(
    "松" to "🎍", "梅" to "🌸", "桜" to "🌸", "藤" to "💜",
    "菖蒲" to "🌿", "牡丹" to "🌺", "萩" to "🍀", "芒" to "🌾",
    "菊" to "🌼", "紅葉" to "🍁", "柳" to "☔", "桐" to "👑",
)

/** Month kanji + flower for UI banners (0=Jan … 11=Dec). */
fun monthLabel(month: Int): Pair<String, String> = MONTH_LABEL[month.coerceIn(0, 11)]

@Composable
fun HanafudaCard(
    card: KoiCard,
    width: Dp,
    selected: Boolean = false,
    highlighted: Boolean = false,
    dimmed: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val height = width * 1.55f
    val borderColor = when {
        selected -> Color(0xFFF06292)
        highlighted -> Color(0xFF66BB6A)
        else -> Color(0xFF8D6E63)
    }
    val (kanji, _) = monthLabel(card.month)
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (selected || highlighted) 3.dp else 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp),
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(hanaRes(card.month, card.id % 4)),
            contentDescription = "$kanji ${card.kind}",
            modifier = Modifier
                .size(width, height)
                .clip(RoundedCornerShape(8.dp))
                .alpha(if (dimmed) 0.45f else 1f),
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
fun HanafudaBack(width: Dp) {
    Box(
        modifier = Modifier
            .size(width, width * 1.55f)
            .clip(RoundedCornerShape(8.dp))
            .border(1.5.dp, Color(0xFF8E0000), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.hana_back),
            contentDescription = "Face-down card",
            modifier = Modifier
                .size(width, width * 1.55f)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.FillBounds,
        )
    }
}
