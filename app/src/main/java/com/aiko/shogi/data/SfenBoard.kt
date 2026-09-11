package com.aiko.shogi.data

/**
 * Minimal SFEN board parse for display.
 * SFEN board part: ranks 9→1, files a→i left-to-right in string order
 * (standard USI/SFEN: first rank is rank 9).
 */
data class Cell(
    val symbol: Char, // uppercase = Black (先手), lowercase = White (後手)
    val promoted: Boolean = false,
)

object SfenBoard {

    /** 9x9 grid: row 0 = rank 9, col 0 = file 9 (left of board from Black's view in standard diagrams). */
    fun parseGrid(sfen: String): Array<Array<Cell?>> {
        val boardPart = sfen.trim().split(" ").firstOrNull().orEmpty()
        val ranks = boardPart.split("/")
        val grid = Array(9) { arrayOfNulls<Cell>(9) }
        ranks.take(9).forEachIndexed { row, rank } ->
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
                        } else i++
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
        val base = when (cell.symbol.uppercaseChar()) {
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
        return base
    }

    /** USI square e.g. 7g → (row, col) in our grid. File 9=col0 … file 1=col8; rank 9=row0 … rank 1=row8. */
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

    /** Parse USI move into from/to (null from = drop). */
    fun parseUsiMove(usi: String): Triple<Pair<Int, Int>?, Pair<Int, Int>, Boolean> {
        // 7g7f / 8h2b+ / B*5e
        val m = usi.trim()
        if ('*' in m) {
            val parts = m.split('*')
            val to = usiSquareToRc(parts.getOrNull(1)?.take(2).orEmpty())
                ?: (0 to 0)
            return Triple(null, to, false)
        }
        val promote = m.endsWith("+")
        val body = if (promote) m.dropLast(1) else m
        require(body.length >= 4)
        val from = usiSquareToRc(body.substring(0, 2))
        val to = usiSquareToRc(body.substring(2, 4)) ?: (0 to 0)
        return Triple(from, to, promote)
    }
}
