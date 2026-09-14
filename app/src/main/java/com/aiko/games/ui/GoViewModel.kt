package com.aiko.games.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiko.games.data.ApiErrors
import com.aiko.games.data.SettingsRepository
import com.aiko.games.data.GoCoords
import com.aiko.games.data.model.GoGameState
import com.aiko.games.data.model.GoMoveRequest
import com.aiko.games.data.model.GoStartRequest
import com.aiko.games.data.remote.GoApi
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
    val difficulty: String = "medium",
    val difficulties: List<String> = listOf("easy", "medium", "hard"),
    val boardSize: Int = 9,
    val availableSizes: List<Int> = listOf(9, 13, 19),
    val side: String = "black",
    val useEngine: Boolean = true,
    val showHints: Boolean = true,
    val inGame: Boolean = false,
)

class GoViewModel(
    private val api: GoApi,
    private val repository: SettingsRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        GoUiState(
            difficulty = repository.getGoDifficulty(),
            side = repository.getGoSide(),
            boardSize = repository.getGoBoardSize(),
            useEngine = repository.isGoEngineEnabled(),
        )
    )
    val ui: StateFlow<GoUiState> = _ui.asStateFlow()

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
                        engineOnline = e.katago,
                        difficulties = orderDifficulties(e.difficulties.ifEmpty { it.difficulties }),
                        availableSizes = e.sizes.ifEmpty { it.availableSizes },
                    )
                }
            } catch (_: Exception) {
                _ui.update { it.copy(engineOnline = false) }
            }
        }
    }

    /** Canonical order so Medium stays in the middle (server sends alphabetical). */
    private fun orderDifficulties(levels: List<String>): List<String> {
        val order = listOf("easy", "medium", "hard")
        return levels.sortedBy { order.indexOf(it.lowercase()).takeIf { i -> i >= 0 } ?: 99 }
    }

    fun warmupEngine() {
        viewModelScope.launch {
            runCatching { api.warmup() }
        }
    }

    fun setDifficulty(level: String) {
        val normalized = level.trim().lowercase()
        if (normalized in _ui.value.difficulties) {
            _ui.update { it.copy(difficulty = normalized) }
            repository.setGoDifficulty(normalized)
        }
    }

    fun setSide(side: String) {
        val normalized = side.trim().lowercase()
        if (normalized == "black" || normalized == "white") {
            _ui.update { it.copy(side = normalized) }
            repository.setGoSide(normalized)
        }
    }

    fun setUseEngine(enabled: Boolean) {
        _ui.update { it.copy(useEngine = enabled) }
        repository.setGoEngineEnabled(enabled)
    }

    fun toggleHints() {
        _ui.update { it.copy(showHints = !it.showHints) }
    }

    fun setBoardSize(size: Int) {
        if (size in _ui.value.availableSizes) {
            _ui.update { it.copy(boardSize = size) }
            repository.setGoBoardSize(size)
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
                        side = _ui.value.side,
                        use_engine = _ui.value.useEngine,
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
                        boardSize = g.size,
                        side = userSide,
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
        if (!isUserTurn() || state.loading) return
        val gtp = GoCoords.toGtp(row, col, game.size)
        if (gtp !in state.legalMoves) return
        sendMove(gtp)
    }

    fun pass() {
        val state = _ui.value
        val game = state.game ?: return
        if (!isUserTurn() || state.loading) return
        if (state.legalMoves.none { GoCoords.isPass(it) }) return
        sendMove("pass")
    }

    fun sendMove(gtp: String) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                val g = api.move(GoMoveRequest(move = gtp))
                val legal = if (g.status == "playing" && g.turn == g.side) {
                    runCatching { api.legalMoves().moves }.getOrDefault(emptyList())
                } else emptyList()
                _ui.update {
                    it.copy(
                        loading = false,
                        game = g,
                        legalMoves = legal,
                        boardSize = g.size,
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
                it.copy(
                    loading = false,
                    game = null,
                    legalMoves = emptyList(),
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

    fun legalHintSquares(): Set<Pair<Int, Int>> {
        if (!_ui.value.showHints) return emptySet()
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
