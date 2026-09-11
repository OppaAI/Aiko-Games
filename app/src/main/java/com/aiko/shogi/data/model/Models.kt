package com.aiko.shogi.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StartRequest(
    val mode: String = "vs_ai",
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

@Serializable
data class ResignResponse(
    val status: String = "",
    val message: String = "",
)
