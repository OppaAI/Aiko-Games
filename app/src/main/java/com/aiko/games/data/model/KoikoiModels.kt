package com.aiko.games.data.model

import kotlinx.serialization.Serializable

@Serializable
data class KoiCard(
    val id: Int = -1,
    val month: Int = 0, // 0=Jan pine … 11=Dec paulownia
    val kind: String = "", // hikari | tane | tan-poetry | tan-red | tan-blue | kasu
    val special: String = "", // crane, moon, boar … (flavor for UI)
)

@Serializable
data class KoiYaku(
    val id: String = "",
    val name: String = "",
    val jp: String = "",
    val points: Int = 0,
)

@Serializable
data class KoiPending(
    val kind: String = "", // flip | decision
    val flip: KoiCard? = null,
    val options: List<List<KoiCard>> = emptyList(),
    val new_yaku: List<KoiYaku> = emptyList(),
    val would_score: Int = 0,
    val multiplier: Int = 1,
)

@Serializable
data class KoiRoundResult(
    val winner: String = "",
    val points: Int = 0,
    val base: Int = 0,
    val multiplier: Int = 1,
    val reason: String = "stop", // stop | exhausted | resigned
)

@Serializable
data class KoiState(
    val hand_you: List<KoiCard> = emptyList(),
    val hand_aiko_count: Int = 0,
    val stock_count: Int = 0,
    val field: List<KoiCard> = emptyList(),
    val cap_you: List<KoiCard> = emptyList(),
    val cap_aiko: List<KoiCard> = emptyList(),
    val yaku_you: List<KoiYaku> = emptyList(),
    val yaku_aiko: List<KoiYaku> = emptyList(),
    val totals: Map<String, Int> = emptyMap(),
    val month: Int = 1,
    val months: Int = 6,
    val oya: String = "you",
    val turn: String = "you",
    val status: String = "playing", // playing | finished
    val mode: String = "vs_ai",
    val pending: KoiPending? = null,
    val last_flip: KoiCard? = null,
    val round_result: KoiRoundResult? = null,
    val winner: String? = null,
    val ai_comment: String? = null,
    val engine: String? = null,
)

@Serializable
data class KoiStartRequest(
    val mode: String = "vs_ai",
    val difficulty: String? = null,
    val months: Int = 6,
)

@Serializable
data class KoiMoveRequest(
    val hand: Int? = null,
    val take: List<Int> = emptyList(),
    val flip_take: List<Int> = emptyList(),
    val decision: String? = null,
)

@Serializable
data class KoiPlay(
    val hand: KoiCard,
    val takes: List<List<KoiCard>> = emptyList(),
)

@Serializable
data class KoiLegalMoves(
    val plays: List<KoiPlay> = emptyList(),
    val pending: KoiPending? = null,
    val turn: String = "you",
    val status: String = "playing",
)

@Serializable
data class KoiEngineStatus(
    val aiko: Boolean = false,
    val kind: String? = null,
    val fallback: String? = null,
    val difficulty: String? = null,
    val difficulties: List<String> = emptyList(),
    val months: List<Int> = emptyList(),
)
