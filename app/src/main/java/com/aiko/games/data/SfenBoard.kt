package com.aiko.games.data

/**
 * Minimal SFEN board + hand parse for display.
 * SFEN: <board> <turn> <hands> <move>
 * Hands: e.g. "R2P" black rook + 2 pawns, "r" white rook, "-" empty.
 * Uppercase = Black (先手), lowercase = White (後手).
 */
data class Cell(
    val symbol: Char,
    val promoted: Boolean = false,
)

data class HandPiece(
    val symbol: Char, // always uppercase type letter: P L N S G B R
    val count: Int,
    val forBlack: Boolean,
) {
    val usiLetter: Char get() = if (forBlack) symbol.uppercaseChar() else symbol.lowercaseChar()
}

object SfenBoard {

    fun parseGrid(sfen: String): Array<Array<Cell?>> {
        val boardPart = sfen.trim().split(" ").firstOrNull().orEmpty()
        val ranks = boardPart.split("/")
        val grid = Array(9) { arrayOfNulls<Cell>(9) }
        ranks.take(9).forEachIndexed { row, rank ->
            var col = 0
            var i = 0
            while (i < rank.length && col < 9) {
                val c = rank[i]
                when {
                    c.isDigit() -> {
                        col += c - '0'
                        i++
                    }
                    c == '+' -> {
                        if (i + 1 < rank.length) {
                            grid[row][col] = Cell(rank[i + 1], promoted = true)
                            col++
                            i += 2
                        } else {
                            i++
                        }
                    }
                    else -> {
                        grid[row][col] = Cell(c, promoted = false)
                        col++
                        i++
                    }
                }
            }
        }
        return grid
    }

    /** Parse hand field into piece counts. */
    fun parseHands(sfen: String): List<HandPiece> {
        val parts = sfen.trim().split(" ")
        val handField = parts.getOrNull(2) ?: "-"
        if (handField == "-" || handField.isBlank()) return emptyList()

        val counts = linkedMapOf<Pair<Char, Boolean>, Int>() // (type upper, forBlack) -> n
        var i = 0
        while (i < handField.length) {
            var n = 0
            while (i < handField.length && handField[i].isDigit()) {
                n = n * 10 + (handField[i] - '0')
                i++
            }
            if (i >= handField.length) break
            val ch = handField[i]
            i++
            if (ch !in "PLNSGBRplnsgbr") continue
            val forBlack = ch.isUpperCase()
            val type = ch.uppercaseChar()
            val key = type to forBlack
            counts[key] = (counts[key] ?: 0) + if (n > 0) n else 1
        }
        return counts.map { (k, n) -> HandPiece(k.first, n, k.second) }
    }

    fun blackHand(sfen: String): List<HandPiece> =
        parseHands(sfen).filter { it.forBlack && it.count > 0 }

    fun whiteHand(sfen: String): List<HandPiece> =
        parseHands(sfen).filter { !it.forBlack && it.count > 0 }

    fun glyph(cell: Cell?): String {
        if (cell == null) return ""
        // Kings: Black 玉 vs White 王 (SFEN uses K/k for both sides).
        if (cell.symbol.uppercaseChar() == 'K') {
            return if (isBlack(cell)) "玉" else "王"
        }
        return glyphFor(cell.symbol, cell.promoted)
    }

    fun glyphFor(symbol: Char, promoted: Boolean = false): String {
        return when (symbol.uppercaseChar()) {
            'P' -> if (promoted) "と" else "歩"
            'L' -> if (promoted) "成香" else "香"
            'N' -> if (promoted) "成桂" else "桂"
            'S' -> if (promoted) "成銀" else "銀"
            'G' -> "金"
            'B' -> if (promoted) "馬" else "角"
            'R' -> if (promoted) "龍" else "飛"
            'K' -> "玉"
            else -> symbol.toString()
        }
    }

    fun isBlack(cell: Cell): Boolean = cell.symbol.isUpperCase()

    fun usiSquareToRc(sq: String): Pair<Int, Int>? {
        if (sq.length < 2) return null
        val file = sq[0]
        val rank = sq[1]
        if (file !in '1'..'9' || rank !in 'a'..'i') return null
        val col = '9' - file
        val row = rank - 'a'
        if (row !in 0..8 || col !in 0..8) return null
        return row to col
    }

    fun rcToUsi(row: Int, col: Int): String {
        val file = ('9' - col)
        val rank = ('a' + row)
        return "$file$rank"
    }
}
