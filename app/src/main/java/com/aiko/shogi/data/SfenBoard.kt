package com.aiko.shogi.data

/**
 * Minimal SFEN board parse for display.
 * SFEN: ranks 9→1 top to bottom; within a rank, file 9→1 left to right.
 */
data class Cell(
    val symbol: Char,
    val promoted: Boolean = false,
)

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

    fun glyph(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.symbol.uppercaseChar()) {
            'P' -> if (cell.promoted) "と" else "歩"
            'L' -> if (cell.promoted) "成香" else "香"
            'N' -> if (cell.promoted) "成桂" else "桂"
            'S' -> if (cell.promoted) "成銀" else "銀"
            'G' -> "金"
            'B' -> if (cell.promoted) "馬" else "角"
            'R' -> if (cell.promoted) "龍" else "飛"
            'K' -> "玉"
            else -> cell.symbol.toString()
        }
    }

    fun isBlack(cell: Cell): Boolean = cell.symbol.isUpperCase()

    /** File 9 = col 0 … file 1 = col 8; rank 9 = row 0 … rank 1 = row 8. */
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
