package com.aiko.games.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.aiko.games.data.model.GoStone
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Goban-style board inspired by classic desktop Go UIs (e.g. Hoshi).
 * Drawn entirely with Compose Canvas — no external textures/assets.
 */
@Composable
fun GoBoardView(
    size: Int,
    stones: List<GoStone>,
    hints: Set<Pair<Int, Int>> = emptySet(),
    lastMove: Pair<Int, Int>? = null,
    enabled: Boolean = true,
    onTap: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stoneMap = remember(stones) {
        stones.associate { (it.row to it.col) to it.color }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(size, enabled, stones) {
                detectTapGestures { offset ->
                    if (!enabled) return@detectTapGestures
                    // Use the same coerced board size as drawing so tap math
                    // can never disagree with the rendered grid.
                    val n = size.coerceIn(9, 19)
                    val pad = min(this.size.width, this.size.height) * 0.06f
                    val usable = min(this.size.width, this.size.height) - 2f * pad
                    val step = usable / (n - 1).coerceAtLeast(1)
                    val originX = (this.size.width - usable) / 2f
                    val originY = (this.size.height - usable) / 2f
                    // Nearest intersection (not floor): round then verify
                    // the tap is within half a cell of it.
                    val col = ((offset.x - originX) / step).roundToInt().coerceIn(0, n - 1)
                    val row = ((offset.y - originY) / step).roundToInt().coerceIn(0, n - 1)
                    val cx = originX + col * step
                    val cy = originY + row * step
                    val dist = kotlin.math.hypot(offset.x - cx, offset.y - cy)
                    if (dist <= step * 0.5f && stoneMap[row to col] == null) {
                        onTap(row, col)
                    }
                }
            },
    ) {
        val n = size.coerceIn(9, 19)
        val pad = min(this.size.width, this.size.height) * 0.06f
        val usable = min(this.size.width, this.size.height) - 2f * pad
        val step = usable / (n - 1).coerceAtLeast(1)
        val originX = (this.size.width - usable) / 2f
        val originY = (this.size.height - usable) / 2f

        drawGobanBackground()
        drawWoodGrain()

        // Grid
        val lineColor = Color(0xFF3A2A18)
        for (i in 0 until n) {
            val x = originX + i * step
            val y = originY + i * step
            drawLine(lineColor, Offset(originX, y), Offset(originX + usable, y), strokeWidth = 1.6f)
            drawLine(lineColor, Offset(x, originY), Offset(x, originY + usable), strokeWidth = 1.6f)
        }

        // Star points (hoshi)
        val stars = starPoints(n)
        for ((r, c) in stars) {
            val cx = originX + c * step
            val cy = originY + r * step
            drawCircle(lineColor, radius = step * 0.09f, center = Offset(cx, cy))
        }

        // Soft legal-move hints under stones layer
        for ((r, c) in hints) {
            if (stoneMap.containsKey(r to c)) continue
            val cx = originX + c * step
            val cy = originY + r * step
            drawCircle(
                color = Color(0x554CAF50),
                radius = step * 0.12f,
                center = Offset(cx, cy),
            )
        }

        // Stones
        for ((key, color) in stoneMap) {
            val (r, c) = key
            val cx = originX + c * step
            val cy = originY + r * step
            val isLast = lastMove?.let { it.first == r && it.second == c } == true
            drawStone(
                center = Offset(cx, cy),
                radius = step * 0.46f,
                black = color.equals("black", ignoreCase = true),
                markLast = isLast,
            )
        }
    }
}

private fun starPoints(n: Int): List<Pair<Int, Int>> = when (n) {
    9 -> listOf(2 to 2, 2 to 6, 6 to 2, 6 to 6, 4 to 4)
    13 -> listOf(3 to 3, 3 to 9, 9 to 3, 9 to 9, 6 to 6)
    19 -> listOf(
        3 to 3, 3 to 9, 3 to 15,
        9 to 3, 9 to 9, 9 to 15,
        15 to 3, 15 to 9, 15 to 15,
    )
    else -> emptyList()
}

private fun DrawScope.drawGobanBackground() {
    // Warm kaya-like base with subtle radial vignette
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE8C992),
                Color(0xFFD4A96A),
                Color(0xFFC4894A),
            ),
            center = Offset(size.width * 0.45f, size.height * 0.4f),
            radius = size.minDimension * 0.85f,
        ),
    )
    // Outer rim
    drawRect(
        color = Color(0xFF5C3D22),
        style = Stroke(width = size.minDimension * 0.018f),
    )
}

private fun DrawScope.drawWoodGrain() {
    // Soft diagonal grain lines — pure procedural, not a texture file
    val grain = Color(0x22A06030)
    val step = size.minDimension / 28f
    var x = -size.height
    while (x < size.width + size.height) {
        drawLine(
            color = grain,
            start = Offset(x, 0f),
            end = Offset(x + size.height * 0.15f, size.height),
            strokeWidth = 1.2f,
        )
        x += step
    }
    // Slight top highlight
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x33FFFFFF),
                Color.Transparent,
            ),
            startY = 0f,
            endY = size.height * 0.35f,
        ),
    )
}

private fun DrawScope.drawStone(
    center: Offset,
    radius: Float,
    black: Boolean,
    markLast: Boolean,
) {
    // Drop shadow
    drawCircle(
        color = Color(0x66000000),
        radius = radius * 1.02f,
        center = Offset(center.x + radius * 0.08f, center.y + radius * 0.12f),
    )

    if (black) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF4A4A4A),
                    Color(0xFF1A1A1A),
                    Color(0xFF0A0A0A),
                ),
                center = Offset(center.x - radius * 0.28f, center.y - radius * 0.32f),
                radius = radius * 1.35f,
            ),
            radius = radius,
            center = center,
        )
        // Specular
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x66FFFFFF),
                    Color.Transparent,
                ),
                center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f),
                radius = radius * 0.55f,
            ),
            radius = radius * 0.45f,
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.32f),
        )
    } else {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF0F0F0),
                    Color(0xFFC8C8C8),
                ),
                center = Offset(center.x - radius * 0.25f, center.y - radius * 0.3f),
                radius = radius * 1.3f,
            ),
            radius = radius,
            center = center,
        )
        drawCircle(
            color = Color(0xFF8A8A8A),
            radius = radius,
            center = center,
            style = Stroke(width = radius * 0.04f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xAAFFFFFF),
                    Color.Transparent,
                ),
                center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f),
                radius = radius * 0.5f,
            ),
            radius = radius * 0.4f,
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.32f),
        )
    }

    if (markLast) {
        // Classic last-move marker (small ring / dot)
        drawCircle(
            color = if (black) Color(0xFFE53935) else Color(0xFFC62828),
            radius = radius * 0.22f,
            center = center,
            style = Stroke(width = radius * 0.1f),
        )
    }
}
