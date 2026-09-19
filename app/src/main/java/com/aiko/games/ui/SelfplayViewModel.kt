package com.aiko.games.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiko.games.data.ApiErrors
import com.aiko.games.data.model.RoundResult
import com.aiko.games.data.model.SelfplayStartRequest
import com.aiko.games.data.model.GoStone
import com.aiko.games.data.model.SelfplayState
import com.aiko.games.data.remote.GoApi
import com.aiko.games.data.remote.KoiKoiApi
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
    val aikoSide: String = "",
    val aikoColor: String = "",
    val aikoWins: Int = 0,
    val engineWins: Int = 0,
    val draws: Int = 0,
    val matches: Int = 0,
    val gameType: String = "shogi",
    val boardSize: Int = 9,
    val blackPoints: Float = 0f,
    val whitePoints: Float = 0f,
    val aikoPts: Int = 0,
    val enginePts: Int = 0,
    val rounds: List<RoundResult> = emptyList(),
    val stones: List<GoStone> = emptyList(),
)

interface SelfplayApi {
    suspend fun start(body: SelfplayStartRequest): SelfplayState
    suspend fun state(): SelfplayState
    suspend fun stop(): SelfplayState
}

class SelfplayViewModel(
    private val api: SelfplayApi,
    private val gameType: String,
) : ViewModel() {

    private val _ui = MutableStateFlow(SelfplayUiState(gameType = gameType))
    val ui: StateFlow<SelfplayUiState> = _ui.asStateFlow()

    private var pollJob: Job? = null
    private var pollActive = false
    // Stale-response guard: polls + manual refreshes can return out of order.
    // A snapshot applies only if it advances the game, grows the move list,
    // or ends the run — otherwise an old response would visibly rewind moves.
    private var lastAppliedGame = -1
    private var lastAppliedMoves = -1

    private fun errorMessage(e: Exception, fallback: String): String =
        ApiErrors.message(e, fallback)

    private fun applyApi(s: SelfplayState) {
        val fresh = s.game_index > lastAppliedGame ||
            (s.game_index == lastAppliedGame && s.moves.size >= lastAppliedMoves) ||
            !s.running
        if (!fresh) return
        lastAppliedGame = s.game_index
        lastAppliedMoves = s.moves.size
        _ui.update {
            it.copy(
                running = s.running,
                gameIndex = s.game_index,
                gamesTotal = s.games_total,
                sfen = s.sfen,
                moves = s.moves,
                status = s.status,
                lastWinner = s.last_winner,
                lastEnd = s.last_end,
                aikoSide = s.aiko_side,
                aikoColor = s.aiko_color,
                aikoWins = s.aiko_wins,
                engineWins = s.engine_wins,
                draws = s.draws,
                matches = s.matches,
                boardSize = s.board_size,
                blackPoints = s.black_points,
                whitePoints = s.white_points,
                aikoPts = s.aiko_pts,
                enginePts = s.engine_pts,
                rounds = s.rounds,
                stones = s.stones,
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                applyApi(api.state())
                _ui.update { it.copy(error = null) }
                if (_ui.value.running) startPolling()
            } catch (e: Exception) {
                _ui.update { it.copy(error = errorMessage(e, "Could not reach training session")) }
            }
        }
    }

    fun start(games: Int = 1, boardSize: Int? = null) {
        if (_ui.value.running || _ui.value.loading) return
        lastAppliedGame = -1
        lastAppliedMoves = -1
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                applyApi(api.start(SelfplayStartRequest(games.coerceIn(1, 20), boardSize)))
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
                applyApi(api.stop())
            } catch (e: Exception) {
                _ui.update { it.copy(error = errorMessage(e, "Could not stop training")) }
            }
        }
    }

    private fun startPolling() {
        if (pollActive) return
        pollActive = true
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var failures = 0
            try {
                while (true) {
                    delay(1000L)
                    val s = try {
                        api.state()
                    } catch (e: Exception) {
                        // Transient error: keep the last good board and retry.
                        // A single failed poll must never freeze the game view.
                        failures += 1
                        if (failures >= 10) {
                            _ui.update { it.copy(error = errorMessage(e, "Lost connection to training")) }
                            break
                        }
                        continue
                    }
                    failures = 0
                    applyApi(s)
                    if (!s.running) break
                }
            } finally {
                pollActive = false
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}

fun createShogiSelfplayViewModel(api: ShogiApi): SelfplayViewModel = SelfplayViewModel(
    api = object : SelfplayApi {
        override suspend fun start(body: SelfplayStartRequest): SelfplayState = api.selfplayStart(body)
        override suspend fun state(): SelfplayState = api.selfplayState()
        override suspend fun stop(): SelfplayState = api.selfplayStop()
    },
    gameType = "shogi",
)

fun createGoSelfplayViewModel(api: GoApi): SelfplayViewModel = SelfplayViewModel(
    api = object : SelfplayApi {
        override suspend fun start(body: SelfplayStartRequest) = api.selfplayStart(body)
        override suspend fun state() = api.selfplayState()
        override suspend fun stop() = api.selfplayStop()
    },
    gameType = "go",
)

fun createKoiKoiSelfplayViewModel(api: KoiKoiApi): SelfplayViewModel = SelfplayViewModel(
    api = object : SelfplayApi {
        override suspend fun start(body: SelfplayStartRequest) = api.selfplayStart(body)
        override suspend fun state() = api.selfplayState()
        override suspend fun stop() = api.selfplayStop()
    },
    gameType = "koikoi",
)
