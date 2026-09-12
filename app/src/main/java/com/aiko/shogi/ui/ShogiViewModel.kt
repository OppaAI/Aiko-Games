package com.aiko.shogi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiko.shogi.data.ApiErrors
import com.aiko.shogi.data.SfenBoard
import com.aiko.shogi.data.model.GameState
import com.aiko.shogi.data.model.MoveRequest
import com.aiko.shogi.data.model.StartRequest
import com.aiko.shogi.data.remote.ShogiApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Board square selection, or a hand drop piece (USI letter e.g. 'P','B'). */
sealed class Selection {
    data class Square(val row: Int, val col: Int) : Selection()
    data class Hand(val piece: Char) : Selection() // uppercase type: P L N S G B R
}

data class PromoteChoice(
    val normal: String,
    val promoted: String,
)

data class ShogiUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val game: GameState? = null,
    val legalMoves: List<String> = emptyList(),
    val selected: Selection? = null,
    val promoteChoice: PromoteChoice? = null,
    val engineOnline: Boolean? = null,
    val difficulty: String = "easy",
    val side: String = "black",
    val difficulties: List<String> = listOf("easy", "medium", "hard"),
    val inGame: Boolean = false,
)

class ShogiViewModel(
    private val api: ShogiApi,
) : ViewModel() {

    private val _ui = MutableStateFlow(ShogiUiState())
    val ui: StateFlow<ShogiUiState> = _ui.asStateFlow()

    /** User's color for the current/next game; the board's side to move is authoritative. */
    fun userSide(): String = _ui.value.game?.side ?: _ui.value.side

    fun isUserTurn(): Boolean {
        val game = _ui.value.game ?: return false
        return game.status == "playing" && game.turn == game.side
    }

    private fun errorMessage(e: Exception, fallback: String): String =
        ApiErrors.message(e, fallback)

    fun refreshEngine() {
        viewModelScope.launch {
            try {
                val e = api.engine()
                _ui.update {
                    it.copy(
                        engineOnline = e.yaneuraou,
                        difficulties = e.difficulties.ifEmpty { it.difficulties },
                    )
                }
            } catch (_: Exception) {
                _ui.update { it.copy(engineOnline = false) }
            }
        }
    }

    /**
     * Best-effort: ask the server to pre-spawn and USI-handshake the engine
     * now, so the first move of a game has no extra latency. Fire-and-forget
     * — failures here are silently ignored since gameplay still falls back
     * to on-demand spawning (and the random-move fallback) either way.
     */
    fun warmupEngine() {
        viewModelScope.launch {
            runCatching { api.warmup() }
        }
    }

    fun setDifficulty(level: String) {
        val normalized = level.trim().lowercase()
        if (normalized in _ui.value.difficulties) {
            _ui.update { it.copy(difficulty = normalized) }
        }
    }

    fun setSide(side: String) {
        val normalized = side.trim().lowercase()
        if (normalized == "black" || normalized == "white") {
            _ui.update { it.copy(side = normalized) }
        }
    }

    fun startGame() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val g = api.start(
                    StartRequest(
                        mode = "vs_ai",
                        difficulty = _ui.value.difficulty,
                        side = _ui.value.side,
                    ),
                )
                val userSide = g.side.ifBlank { _ui.value.side }
                val legal = if (g.status == "playing" && g.turn == userSide) {
                    runCatching { api.legalMoves().moves }.getOrDefault(emptyList())
                } else emptyList()
                _ui.update {
                    it.copy(
                        loading = false,
                        game = g,
                        legalMoves = legal,
                        selected = null,
                        promoteChoice = null,
                        inGame = true,
                        side = userSide,
                        // Reflect the current game's engine, not the previous state --
                        // otherwise engineOnline can only ever flip to true and get stuck there.
                        engineOnline = g.engine == "yaneuraou",
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(loading = false, error = errorMessage(e, "Failed to start"))
                }
            }
        }
    }

    fun onHandPieceTap(piece: Char) {
        val state = _ui.value
        val game = state.game ?: return
        if (!isUserTurn() || state.loading) return
        if (state.promoteChoice != null) return

        val letter = piece.uppercaseChar()
        // Black drops use uppercase in USI typically: P*5e
        val can = state.legalMoves.any {
            '*' in it && it.substringBefore('*').equals(letter.toString(), ignoreCase = true)
        }
        if (!can) return

        val sel = state.selected
        if (sel is Selection.Hand && sel.piece == letter) {
            _ui.update { it.copy(selected = null) }
        } else {
            _ui.update { it.copy(selected = Selection.Hand(letter), error = null) }
        }
    }

    fun onSquareTap(row: Int, col: Int) {
        val state = _ui.value
        val game = state.game ?: return
        if (!isUserTurn()) return
        if (state.loading || state.promoteChoice != null) return

        when (val selected = state.selected) {
            null -> {
                val fromUsi = SfenBoard.rcToUsi(row, col)
                val can = state.legalMoves.any { m ->
                    !m.contains('*') && m.length >= 4 && m.startsWith(fromUsi)
                }
                if (can) {
                    _ui.update { it.copy(selected = Selection.Square(row, col), error = null) }
                }
            }

            is Selection.Square -> {
                if (selected.row == row && selected.col == col) {
                    _ui.update { it.copy(selected = null) }
                    return
                }
                val fromUsi = SfenBoard.rcToUsi(selected.row, selected.col)
                val toUsi = SfenBoard.rcToUsi(row, col)
                val base = fromUsi + toUsi
                val candidates = state.legalMoves.filter {
                    it == base || it == "$base+" || it.startsWith(base)
                }.distinct()

                when {
                    candidates.isEmpty() -> {
                        val can = state.legalMoves.any { m ->
                            !m.contains('*') && m.length >= 4 && m.startsWith(toUsi)
                        }
                        _ui.update {
                            it.copy(selected = if (can) Selection.Square(row, col) else null)
                        }
                    }
                    candidates.size == 1 -> sendMove(candidates.first())
                    candidates.any { it.endsWith("+") } && candidates.any { !it.endsWith("+") } -> {
                        val normal = candidates.first { !it.endsWith("+") }
                        val promoted = candidates.first { it.endsWith("+") }
                        _ui.update {
                            it.copy(promoteChoice = PromoteChoice(normal, promoted))
                        }
                    }
                    else -> sendMove(candidates.first())
                }
            }

            is Selection.Hand -> {
                val toUsi = SfenBoard.rcToUsi(row, col)
                val letter = selected.piece.uppercaseChar()
                val drop = state.legalMoves.firstOrNull { m ->
                    '*' in m &&
                        m.substringBefore('*').equals(letter.toString(), ignoreCase = true) &&
                        m.substringAfter('*').take(2) == toUsi
                }
                if (drop != null) {
                    sendMove(drop)
                } else {
                    // Maybe select a board piece instead
                    val can = state.legalMoves.any { m ->
                        !m.contains('*') && m.length >= 4 && m.startsWith(toUsi)
                    }
                    _ui.update {
                        it.copy(selected = if (can) Selection.Square(row, col) else null)
                    }
                }
            }
        }
    }

    fun confirmPromote(promote: Boolean) {
        val choice = _ui.value.promoteChoice ?: return
        sendMove(if (promote) choice.promoted else choice.normal)
    }

    fun dismissPromote() {
        _ui.update { it.copy(promoteChoice = null) }
    }

    fun sendMove(usi: String) {
        viewModelScope.launch {
            _ui.update {
                it.copy(loading = true, error = null, selected = null, promoteChoice = null)
            }
            try {
                val g = api.move(MoveRequest(move = usi))
                val legal = if (g.status == "playing" && g.turn == g.side) {
                    runCatching { api.legalMoves().moves }.getOrDefault(emptyList())
                } else emptyList()
                _ui.update {
                    it.copy(
                        loading = false,
                        game = g,
                        legalMoves = legal,
                        // Reflect the current game's engine, not the previous state --
                        // otherwise engineOnline can only ever flip to true and get stuck there.
                        engineOnline = g.engine == "yaneuraou",
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(loading = false, error = errorMessage(e, "Move failed"))
                }
                runCatching {
                    val legal = api.legalMoves().moves
                    _ui.update { it.copy(legalMoves = legal) }
                }
            }
        }
    }

    fun resign() {
        viewModelScope.launch {
            runCatching { api.resign() }
            _ui.update {
                it.copy(
                    loading = false,
                    game = null,
                    legalMoves = emptyList(),
                    selected = null,
                    promoteChoice = null,
                    error = null,
                    inGame = false,
                )
            }
            refreshEngine()
        }
    }

    fun clearError() {
        _ui.update { it.copy(error = null) }
    }

    fun hintSquares(): Set<Pair<Int, Int>> {
        val state = _ui.value
        return when (val sel = state.selected) {
            is Selection.Square -> {
                val from = SfenBoard.rcToUsi(sel.row, sel.col)
                state.legalMoves.mapNotNull { m ->
                    if (m.contains('*') || m.length < 4 || !m.startsWith(from)) null
                    else SfenBoard.usiSquareToRc(m.removeSuffix("+").substring(2, 4))
                }.toSet()
            }
            is Selection.Hand -> {
                val letter = sel.piece.uppercaseChar()
                state.legalMoves.mapNotNull { m ->
                    if ('*' !in m) return@mapNotNull null
                    if (!m.substringBefore('*').equals(letter.toString(), ignoreCase = true)) {
                        return@mapNotNull null
                    }
                    SfenBoard.usiSquareToRc(m.substringAfter('*').take(2))
                }.toSet()
            }
            null -> emptySet()
        }
    }

    fun lastMoveSquares(): Set<Pair<Int, Int>> {
        val usi = _ui.value.game?.last_move ?: return emptySet()
        if ('*' in usi) {
            val to = SfenBoard.usiSquareToRc(usi.substringAfter('*').take(2))
            return setOfNotNull(to)
        }
        if (usi.length < 4) return emptySet()
        val body = usi.removeSuffix("+")
        return setOfNotNull(
            SfenBoard.usiSquareToRc(body.take(2)),
            SfenBoard.usiSquareToRc(body.substring(2, 4)),
        )
    }
}
