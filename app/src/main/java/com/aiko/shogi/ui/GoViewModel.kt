package com.aiko.shogi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiko.shogi.data.GoCoords
import com.aiko.shogi.data.model.GoGameState
import com.aiko.shogi.data.model.GoMoveRequest
import com.aiko.shogi.data.model.GoStartRequest
import com.aiko.shogi.data.remote.GoApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val game: GoGameState? = null,
    val legalMoves: List<String> = emptyList(),
    val engineOnline: Boolean? = null,
    val difficulty: String = "easy",
    val boardSize: Int = 9,
    val inGame: Boolean = false,
)

class GoViewModel(
    private val api: GoApi,
) : ViewModel() {

    private val _ui = MutableStateFlow(GoUiState())
    val ui: StateFlow<GoUiState> = _ui.asStateFlow()

    private fun errorMessage(e: Exception, fallback: String): String {
        if (e is retrofit2.HttpException) {
            runCatching {
                val body = e.response()?.errorBody()?.string().orEmpty()
                if (body.isNotBlank()) {
                    val detail = runCatching {
                        kotlinx.serialization.json.Json.parseToJsonElement(body)
                            .let { it as? kotlinx.serialization.json.JsonObject }
                            ?.get("detail")
                            ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    }.getOrNull()
                    if (!detail.isNullOrBlank()) return "HTTP ${e.code()}: $detail"
                }
            }
            return "HTTP ${e.code()}: ${e.message() ?: fallback}"
        }
        return e.message ?: fallback
    }

    fun refreshEngine() {
        viewModelScope.launch {
            try {
                val e = api.engine()
                _ui.update { it.copy(engineOnline = e.katago) }
            } catch (_: Exception) {
                _ui.update { it.copy(engineOnline = false) }
            }
        }
    }

    fun warmupEngine() {
        viewModelScope.launch {
            runCatching { api.warmup() }
        }
    }

    fun setDifficulty(level: String) {
        _ui.update { it.copy(difficulty = level) }
    }

    fun setBoardSize(size: Int) {
        if (size in listOf(9, 13, 19)) {
            _ui.update { it.copy(boardSize = size) }
        }
    }

    fun startGame() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val g = api.start(
                    GoStartRequest(
                        mode = "vs_ai",
                        size = _ui.value.boardSize,
                        difficulty = _ui.value.difficulty,
                        side = "black",
                    ),
                )
                val legal = runCatching { api.legalMoves().moves }.getOrDefault(emptyList())
                _ui.update {
                    it.copy(
                        loading = false,
                        game = g,
                        legalMoves = legal,
                        inGame = true,
                        engineOnline = g.engine == "katago",
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(loading = false, error = errorMessage(e, "Failed to start Go"))
                }
            }
        }
    }

    fun onIntersectionTap(row: Int, col: Int) {
        val state = _ui.value
        val game = state.game ?: return
        if (game.status != "playing" || game.turn != "black" || state.loading) return
        val gtp = GoCoords.toGtp(row, col, game.size)
        if (gtp !in state.legalMoves) return
        sendMove(gtp)
    }

    fun pass() {
        val state = _ui.value
        val game = state.game ?: return
        if (game.status != "playing" || game.turn != "black" || state.loading) return
        if ("pass" !in state.legalMoves && "PASS" !in state.legalMoves) return
        sendMove("pass")
    }

    fun sendMove(gtp: String) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val g = api.move(GoMoveRequest(move = gtp))
                val legal = if (g.status == "playing" && g.turn == "black") {
                    runCatching { api.legalMoves().moves }.getOrDefault(emptyList())
                } else emptyList()
                _ui.update {
                    it.copy(
                        loading = false,
                        game = g,
                        legalMoves = legal,
                        engineOnline = g.engine == "katago",
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
                GoUiState(
                    engineOnline = it.engineOnline,
                    difficulty = it.difficulty,
                    boardSize = it.boardSize,
                    inGame = false,
                )
            }
            refreshEngine()
        }
    }

    fun clearError() {
        _ui.update { it.copy(error = null) }
    }

    fun legalHintSquares(): Set<Pair<Int, Int>> {
        val game = _ui.value.game ?: return emptySet()
        return _ui.value.legalMoves.mapNotNull { m ->
            GoCoords.fromGtp(m, game.size)
        }.toSet()
    }

    fun lastMoveSquare(): Pair<Int, Int>? {
        val game = _ui.value.game ?: return null
        val m = game.last_move ?: return null
        return GoCoords.fromGtp(m, game.size)
    }
}
