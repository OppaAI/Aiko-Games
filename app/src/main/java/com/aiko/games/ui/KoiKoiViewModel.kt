package com.aiko.games.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiko.games.data.ApiErrors
import com.aiko.games.data.model.KoiMoveRequest
import com.aiko.games.data.model.KoiPlay
import com.aiko.games.data.model.KoiStartRequest
import com.aiko.games.data.model.KoiState
import com.aiko.games.data.remote.KoiKoiApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KoiUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val game: KoiState? = null,
    val plays: List<KoiPlay> = emptyList(),
    val selectedHand: Int? = null,
    val engineOnline: Boolean? = null,
    val difficulty: String = "medium",
    val difficulties: List<String> = listOf("easy", "medium", "hard"),
    val months: Int = 6,
    val availableMonths: List<Int> = listOf(3, 6, 12),
    val inGame: Boolean = false,
)

class KoiKoiViewModel(
    private val api: KoiKoiApi,
) : ViewModel() {

    private val _ui = MutableStateFlow(KoiUiState())
    val ui: StateFlow<KoiUiState> = _ui.asStateFlow()

    private fun errorMessage(e: Exception, fallback: String): String =
        ApiErrors.message(e, fallback)

    fun refreshEngine() {
        viewModelScope.launch {
            try {
                val e = api.engine()
                _ui.update {
                    it.copy(
                        engineOnline = e.aiko,
                        difficulties = orderDifficulties(e.difficulties.ifEmpty { it.difficulties }),
                        availableMonths = e.months.ifEmpty { it.availableMonths },
                    )
                }
            } catch (_: Exception) {
                _ui.update { it.copy(engineOnline = false) }
            }
        }
    }

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
        }
    }

    fun setMonths(n: Int) {
        if (n in _ui.value.availableMonths) {
            _ui.update { it.copy(months = n) }
        }
    }

    fun startGame() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, selectedHand = null) }
            try {
                val g = api.start(
                    KoiStartRequest(mode = "vs_ai", difficulty = _ui.value.difficulty, months = _ui.value.months),
                )
                val legal = runCatching { api.legalMoves().plays }.getOrDefault(emptyList())
                _ui.update {
                    it.copy(
                        loading = false, game = g, plays = legal,
                        months = g.months, inGame = g.status == "playing",
                        engineOnline = g.engine == "aiko",
                    )
                }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = errorMessage(e, "Failed to start Koi-Koi")) }
            }
        }
    }

    /** All legal take-options (each a list of field ids) for a hand card. */
    fun takeOptionsFor(handId: Int): List<List<Int>> =
        _ui.value.plays.firstOrNull { it.hand.id == handId }
            ?.takes?.map { opt -> opt.map { c -> c.id } } ?: emptyList()

    fun isMyTurn(): Boolean {
        val g = _ui.value.game ?: return false
        return g.status == "playing" && g.turn == "you" && g.pending == null
    }

    fun onHandTap(handId: Int) {
        val state = _ui.value
        val game = state.game ?: return
        if (state.loading || game.status != "playing" || game.turn != "you") return
        if (game.pending != null) return
        val options = takeOptionsFor(handId)
        if (options.isEmpty()) return // not playable (shouldn't happen)
        val nonEmpty = options.filter { it.isNotEmpty() }
        when {
            // Single forced take (or forced no-capture): play immediately.
            nonEmpty.size <= 1 && options.size == 1 ->
                sendPlay(handId, options.first())
            // No capture possible: first tap selects, second tap confirms.
            nonEmpty.isEmpty() ->
                if (state.selectedHand == handId) sendPlay(handId, emptyList())
                else _ui.update { it.copy(selectedHand = handId, error = null) }
            // Ambiguous: select, then tap a glowing field card.
            state.selectedHand == handId ->
                _ui.update { it.copy(selectedHand = null) }
            else ->
                _ui.update { it.copy(selectedHand = handId, error = null) }
        }
    }

    fun onFieldTap(fieldId: Int) {
        val state = _ui.value
        val handId = state.selectedHand ?: return
        val options = takeOptionsFor(handId).filter { it.isNotEmpty() }
        val match = options.firstOrNull { fieldId in it } ?: return
        sendPlay(handId, match)
    }

    fun answerFlip(fieldIds: List<Int>) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, selectedHand = null) }
            try {
                val g = api.move(KoiMoveRequest(flip_take = fieldIds))
                val legal = runCatching { api.legalMoves().plays }.getOrDefault(emptyList())
                _ui.update {
                    it.copy(loading = false, game = g, plays = legal, inGame = true)
                }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = errorMessage(e, "Flip answer failed")) }
            }
        }
    }

    fun decide(stop: Boolean) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, selectedHand = null) }
            try {
                val g = api.move(KoiMoveRequest(decision = if (stop) "stop" else "koi"))
                val legal = runCatching { api.legalMoves().plays }.getOrDefault(emptyList())
                _ui.update {
                    it.copy(loading = false, game = g, plays = legal, inGame = true)
                }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = errorMessage(e, "Decision failed")) }
            }
        }
    }

    private fun sendPlay(handId: Int, take: List<Int>) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, selectedHand = null) }
            try {
                val g = api.move(KoiMoveRequest(hand = handId, take = take))
                val legal = runCatching { api.legalMoves().plays }.getOrDefault(emptyList())
                _ui.update {
                    it.copy(loading = false, game = g, plays = legal, inGame = true)
                }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = errorMessage(e, "Move failed")) }
            }
        }
    }

    fun resign() {
        viewModelScope.launch {
            runCatching { api.resign() }
            _ui.update {
                it.copy(loading = false, game = null, plays = emptyList(), selectedHand = null, error = null, inGame = false)
            }
            refreshEngine()
        }
    }

    fun clearError() {
        _ui.update { it.copy(error = null) }
    }
}
