package com.aiko.games.ui

import androidx.annotation.DrawableRes
import com.aiko.games.R
import com.aiko.games.data.Cell

/**
 * Bundled offline CC0 art from sunfish-shogi/shogi-images.
 *
 * Set: hitomoji_wood (一文字駒・木目) + board/light_458x500.png.
 * License: CC0 1.0 Universal — free to use, no attribution required.
 * Source: https://sunfish-shogi.github.io/shogi-images/#hitomoji_wood
 *
 * All PNGs live in res/drawable-nodpi as shogi_* so the board works
 * fully offline (no Coil / network). King mapping follows the app's
 * glyph convention: sente (black) = 玉 king2, gote (white) = 王 king.
 */
object KomaImages {
    @DrawableRes
    val BOARD_LIGHT_RES: Int = R.drawable.shogi_board_light

    /** Local drawable for a board cell, or null if empty/unknown. */
    @DrawableRes
    fun drawableFor(cell: Cell?): Int? {
        if (cell == null) return null
        return pieceDrawable(cell.symbol.uppercaseChar(), cell.promoted, cell.symbol.isUpperCase())
    }

    /** Hand piece (type letter uppercase, never promoted). */
    @DrawableRes
    fun handDrawable(type: Char, forBlack: Boolean): Int? {
        return pieceDrawable(type.uppercaseChar(), promoted = false, black = forBlack)
    }

    @DrawableRes
    private fun pieceDrawable(type: Char, promoted: Boolean, black: Boolean): Int? {
        // Sente (black) king = 玉 king2; gote (white) king = 王 king.
        if (type == 'K') {
            return if (black) R.drawable.shogi_black_king2 else R.drawable.shogi_white_king
        }
        if (black) {
            return when (type) {
                'P' -> if (promoted) R.drawable.shogi_black_prom_pawn else R.drawable.shogi_black_pawn
                'L' -> if (promoted) R.drawable.shogi_black_prom_lance else R.drawable.shogi_black_lance
                'N' -> if (promoted) R.drawable.shogi_black_prom_knight else R.drawable.shogi_black_knight
                'S' -> if (promoted) R.drawable.shogi_black_prom_silver else R.drawable.shogi_black_silver
                'G' -> R.drawable.shogi_black_gold
                'B' -> if (promoted) R.drawable.shogi_black_horse else R.drawable.shogi_black_bishop
                'R' -> if (promoted) R.drawable.shogi_black_dragon else R.drawable.shogi_black_rook
                else -> null
            }
        } else {
            return when (type) {
                'P' -> if (promoted) R.drawable.shogi_white_prom_pawn else R.drawable.shogi_white_pawn
                'L' -> if (promoted) R.drawable.shogi_white_prom_lance else R.drawable.shogi_white_lance
                'N' -> if (promoted) R.drawable.shogi_white_prom_knight else R.drawable.shogi_white_knight
                'S' -> if (promoted) R.drawable.shogi_white_prom_silver else R.drawable.shogi_white_silver
                'G' -> R.drawable.shogi_white_gold
                'B' -> if (promoted) R.drawable.shogi_white_horse else R.drawable.shogi_white_bishop
                'R' -> if (promoted) R.drawable.shogi_white_dragon else R.drawable.shogi_white_rook
                else -> null
            }
        }
    }

    /** True = sente (black, points up). False = gote (white, points down). */
    fun isBlackSide(cell: Cell): Boolean = cell.symbol.isUpperCase()
}
