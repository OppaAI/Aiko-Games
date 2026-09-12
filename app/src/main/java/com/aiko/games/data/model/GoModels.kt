package com.aiko.games.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GoStartRequest(
    val mode: String = "vs_ai",
    val size: Int = 9,
    val difficulty: String? = null,
    val side: String? = "black",
)

@Serializable
data class GoMoveRequest(
    val move: String,
)

@Serializable
data class GoStone(
    val color: String = "",
    val row: Int = 0,
    val col: Int = 0,
    val gtp: String = "",
)

@Serializable
data class GoGameState(
    val size: Int = 9,
    val turn: String = "black",
    val last_move: String? = null,
    val status: String = "playing",
    val mode: String = "vs_ai",
    val side: String = "black",
    val stones: List<GoStone> = emptyList(),
    val captured_black: Int = 0,
    val captured_white: Int = 0,
    val ai_comment: String? = null,
    val engine: String? = null,
    val moves: List<String> = emptyList(),
)

@Serializable
data class GoLegalMovesResponse(
    val moves: List<String> = emptyList(),
    val turn: String = "black",
    val status: String = "playing",
    val size: Int = 9,
)

@Serializable
data class GoEngineStatus(
    val katago: Boolean = false,
    val path: String? = null,
    val model: String? = null,
    val fallback: String? = null,
    val difficulty: String? = null,
    val difficulties: List<String> = emptyList(),
    val sizes: List<Int> = emptyList(),
)

@Serializable
data class GoResignResponse(
    val status: String = "",
    val message: String = "",
)
