package com.aiko.shogi.data

/** Shared HTTP error shaping: prefer server {"detail": "..."} over Retrofit's generic message. */
object ApiErrors {
    fun message(e: Exception, fallback: String): String {
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
}
