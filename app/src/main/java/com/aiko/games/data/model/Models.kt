package com.aiko.games.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StartRequest(
    val mode: String = "vs_ai",
    val difficulty: String? = null,
    val side: String? = "black",
    val use_engine: Boolean = true,
)

@Serializable
data class MoveRequest(
    val move: String,
)

@Serializable
data class GameState(
    val sfen: String = "",
    val turn: String = "black",
    val last_move: String? = null,
    val status: String = "playing",
    val mode: String = "vs_ai",
    val side: String = "black",
    val ai_comment: String? = null,
    val engine: String? = null,
    val clock_black_ms: Long? = null,
    val clock_white_ms: Long? = null,
    val byoyomi_ms: Long? = null,
)

@Serializable
data class LegalMovesResponse(
    val moves: List<String> = emptyList(),
    val turn: String = "black",
    val status: String = "playing",
)

@Serializable
data class EngineStatus(
    val yaneuraou: Boolean = false,
    val path: String? = null,
    val movetime_ms: Int? = null,
    val fallback: String? = null,
    val difficulty: String? = null,
    val difficulties: List<String> = emptyList(),
)

@Serializable
data class WarmupResponse(
    val warmed: Boolean = false,
    val reason: String? = null,
    val error: String? = null,
)

@Serializable
data class SelfplayStartRequest(
    val games: Int = 1,
)

@Serializable
data class SelfplayState(
    val running: Boolean = false,
    val game_index: Int = 0,
    val games_total: Int = 0,
    val sfen: String = "",
    val moves: List<String> = emptyList(),
    val status: String = "idle",
    val last_winner: String = "",
    val last_end: String = "",
    val aiko_side: String = "",
    val aiko_wins: Int = 0,
    val engine_wins: Int = 0,
    val draws: Int = 0,
    val matches: Int = 0,
)
