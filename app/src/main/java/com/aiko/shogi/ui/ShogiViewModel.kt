package com.aiko.shogi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class ShogiUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val game: GameState? = null,
    val legalMoves: List<String> = emptyList(),
    val selected: Pair<Int, Int>? = null,
    val engineOnline: Boolean? = null,
    val inGame: Boolean = false,
)

class ShogiViewModel(
    private val api: ShogiApi,
) : ViewModel() {

    private val _ui = MutableStateFlow(ShogiUiState())
    val ui: StateFlow<ShogiUiState> = _ui.asStateFlow()

    fun refreshEngine() {
        viewModelScope.launch {
            try {
                val e = api.engine()
                _ui.update { it.copy(engineOnline = e.yaneuraou) }
            } catch (_: Exception) {
                _ui.update { it.copy(engineOnline = false) }
            }
        }
    }

    fun startGame() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val g = api.start(StartRequest(mode = "vs_ai"))
                val legal = runCatching { api.legalMoves().moves }.getOrDefault(emptyList())
                _ui.update {
                    it.copy(
                        loading = false,
                        game = g,
                        legalMoves = legal,
                        selected = null,
                        inGame = true,
                        engineOnline = g.engine == "yaneuraou" || it.engineOnline,
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(loading = false, error = e.message ?: "Failed to start")
                }
            }
        }
    }

    fun onSquareTap(row: Int, col: Int) {
        val state = _ui.value
        val game = state.game ?: return
        if (game.status != "playing" || game.turn != "black") return
        if (state.loading) return

        val selected = state.selected
        if (selected == null) {
            // Select own piece if any move starts from here
            val fromUsi = SfenBoard.rcToUsi(row, col)
            val can = state.legalMoves.any { m ->
                !m.contains('*') && m.length >= 4 && m.startsWith(fromUsi)
            }
            if (can) {
                _ui.update { it.copy(selected = row to col, error = null) }
            }
            return
        }

        if (selected.first == row && selected.second == col) {
            _ui.update { it.copy(selected = null) }
            return
        }

        val fromUsi = SfenBoard.rcToUsi(selected.first, selected.second)
        val toUsi = SfenBoard.rcToUsi(row, col)
        val candidates = state.legalMoves.filter {
            it.startsWith(fromUsi + toUsi) || it == fromUsi + toUsi || it == fromUsi + toUsi + "+"
        }
        val move = when {
            candidates.any { it.endsWith("+") } && candidates.any { !it.endsWith("+") } ->
                // Prefer non-promote for MVP; user can force promote later
                candidates.first { !it.endsWith("+") }
            candidates.isNotEmpty() -> candidates.first()
            else -> null
        }
        if (move == null) {
            // Try re-select
            val from2 = SfenBoard.rcToUsi(row, col)
            val can = state.legalMoves.any { m ->
                !m.contains('*') && m.length >= 4 && m.startsWith(from2)
            }
            _ui.update {
                it.copy(selected = if (can) row to col else null)
            }
            return
        }
        sendMove(move)
    }

    fun sendMove(usi: String) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, selected = null) }
            try {
                val g = api.move(MoveRequest(move = usi))
                val legal = if (g.status == "playing" && g.turn == "black") {
                    runCatching { api.legalMoves().moves }.getOrDefault(emptyList())
                } else emptyList()
                _ui.update {
                    it.copy(
                        loading = false,
                        game = g,
                        legalMoves = legal,
                        engineOnline = g.engine == "yaneuraou" || it.engineOnline,
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(loading = false, error = e.message ?: "Move failed")
                }
                // Refresh legal moves
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
                ShogiUiState(engineOnline = it.engineOnline, inGame = false)
            }
            refreshEngine()
        }
    }

    fun clearError() {
        _ui.update { it.copy(error = null) }
    }

    /** Destination squares for the current selection. */
    fun hintSquares(): Set<Pair<Int, Int>> {
        val sel = _ui.value.selected ?: return emptySet()
        val from = SfenBoard.rcToUsi(sel.first, sel.second)
        return _ui.value.legalMoves.mapNotNull { m ->
            if (m.contains('*') || m.length < 4 || !m.startsWith(from)) null
            else SfenBoard.usiSquareToRc(m.substring(2, 4))
        }.toSet()
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
