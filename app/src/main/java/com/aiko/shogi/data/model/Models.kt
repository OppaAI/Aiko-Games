package com.aiko.shogi.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StartRequest(
    val mode: String = "vs_ai",
    val difficulty: String? = null,
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
)

@Serializable
data class WarmupResponse(
    val warmed: Boolean = false,
    val reason: String? = null,
    val error: String? = null,
)
