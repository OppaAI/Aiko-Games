package com.aiko.shogi.data

import java.util.Locale

/** GTP letters skip I; rank 1 = bottom, row 0 = top = size. */
object GoCoords {
    private const val COLS = "ABCDEFGHJKLMNOPQRST"
    private val PASS_TOKENS = setOf("PASS", "PA")
    private val RESIGN_TOKENS = setOf("RESIGN", "R")

    fun toGtp(row: Int, col: Int, size: Int): String {
        require(row in 0 until size && col in 0 until size)
        return "${COLS[col]}${size - row}"
    }

    fun fromGtp(move: String, size: Int): Pair<Int, Int>? {
        val m = move.trim().uppercase(Locale.ROOT)
        if (m.isEmpty() || m in PASS_TOKENS || m in RESIGN_TOKENS) return null
        if (m[0] !in COLS) return null
        val col = COLS.indexOf(m[0])
        val rank = m.substring(1).toIntOrNull() ?: return null
        if (col !in 0 until size || rank !in 1..size) return null
        val row = size - rank
        return row to col
    }

    fun isPass(move: String): Boolean =
        move.trim().uppercase(Locale.ROOT) in PASS_TOKENS
}
