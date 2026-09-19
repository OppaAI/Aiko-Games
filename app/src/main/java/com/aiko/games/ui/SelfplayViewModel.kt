package com.aiko.games.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiko.games.data.ApiErrors
import com.aiko.games.data.model.SelfplayStartRequest
import com.aiko.games.data.remote.ShogiApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SelfplayUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val running: Boolean = false,
    val gameIndex: Int = 0,
    val gamesTotal: Int = 0,
    val sfen: String = "",
    val moves: List<String> = emptyList(),
    val status: String = "idle",
    val lastWinner: String = "",
    val lastEnd: String = "",
    val aikoWins: Int = 0,
    val engineWins: Int = 0,
    val draws: Int = 0,
    val matches: Int = 0,
)

/** Aiko-vs-YaneuraOu training sessions. Polls while a session runs. */
class SelfplayViewModel(
    private val api: ShogiApi,
) : ViewModel() {

    private val _ui = MutableStateFlow(SelfplayUiState())
    val ui: StateFlow<SelfplayUiState> = _ui.asStateFlow()

    private var pollJob: Job? = null

    private fun errorMessage(e: Exception, fallback: String): String =
        ApiErrors.message(e, fallback)

    private fun applyState(
        running: Boolean,
        gameIndex: Int,
        gamesTotal: Int,
        sfen: String,
        moves: List<String>,
        status: String,
        lastWinner: String,
        lastEnd: String,
        aikoWins: Int,
        engineWins: Int,
        draws: Int,
        matches: Int,
    ) {
        _ui.update {
            it.copy(
                running = running, gameIndex = gameIndex, gamesTotal = gamesTotal,
                sfen = sfen, moves = moves, status = status,
                lastWinner = lastWinner, lastEnd = lastEnd,
                aikoWins = aikoWins, engineWins = engineWins,
                draws = draws, matches = matches,
            )
        }
    }

    private fun applyApi(s: com.aiko.games.data.model.SelfplayState) {
        applyState(
            running = s.running, gameIndex = s.game_index, gamesTotal = s.games_total,
            sfen = s.sfen, moves = s.moves, status = s.status,
            lastWinner = s.last_winner, lastEnd = s.last_end,
            aikoWins = s.aiko_wins, engineWins = s.engine_wins,
            draws = s.draws, matches = s.matches,
        )
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                applyApi(api.selfplayState())
                _ui.update { it.copy(error = null) }
            } catch (e: Exception) {
                _ui.update { it.copy(error = errorMessage(e, "Could not reach training session")) }
            }
        }
    }

    fun start(games: Int = 1) {
        if (_ui.value.running || _ui.value.loading) return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                applyApi(api.selfplayStart(SelfplayStartRequest(games.coerceIn(1, 20))))
                _ui.update { it.copy(loading = false) }
                startPolling()
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = errorMessage(e, "Could not start training")) }
            }
        }
    }

    fun stop() {
        viewModelScope.launch {
            try {
                applyApi(api.selfplayStop())
            } catch (e: Exception) {
                _ui.update { it.copy(error = errorMessage(e, "Could not stop training")) }
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(2000L)
                try {
                    val s = api.selfplayState()
                    applyApi(s)
                    if (!s.running) break
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
