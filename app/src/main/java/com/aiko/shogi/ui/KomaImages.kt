package com.aiko.shogi.ui

import com.aiko.shogi.data.Cell

/**
 * CC0 piece + board art from sunfish-shogi/shogi-images.
 *
 * Wood-grain set is **1-kanji only** upstream (一文字駒・木目).
 * Board: 盤 - 木材（明）458×500.
 *
 * https://sunfish-shogi.github.io/shogi-images/
 */
object KomaImages {
    private const val PIECE_BASE =
        "https://sunfish-shogi.github.io/shogi-images/hitomoji_wood"
    const val BOARD_LIGHT =
        "https://sunfish-shogi.github.io/shogi-images/board/light_458x500.png"

    /** Remote PNG for a board cell, or null if empty. */
    fun url(cell: Cell?): String? {
        if (cell == null) return null
        val black = cell.symbol.isUpperCase()
        val side = if (black) "black" else "white"
        val name = pieceFileName(cell.symbol.uppercaseChar(), cell.promoted) ?: return null
        return "$PIECE_BASE/${side}_$name.png"
    }

    /** Hand piece (type letter uppercase, not promoted). */
    fun handUrl(type: Char, forBlack: Boolean): String? {
        val side = if (forBlack) "black" else "white"
        val name = pieceFileName(type.uppercaseChar(), promoted = false) ?: return null
        return "$PIECE_BASE/${side}_$name.png"
    }

    private fun pieceFileName(type: Char, promoted: Boolean): String? = when (type) {
        'P' -> if (promoted) "prom_pawn" else "pawn"
        'L' -> if (promoted) "prom_lance" else "lance"
        'N' -> if (promoted) "prom_knight" else "knight"
        'S' -> if (promoted) "prom_silver" else "silver"
        'G' -> "gold"
        'B' -> if (promoted) "horse" else "bishop"
        'R' -> if (promoted) "dragon" else "rook"
        // Prefer jeweled king (玉) art for both sides when available as king.png
        'K' -> "king"
        else -> null
    }
}
