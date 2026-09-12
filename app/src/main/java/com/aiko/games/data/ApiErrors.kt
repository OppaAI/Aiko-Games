package com.aiko.games.data

/** Shared HTTP error shaping: friendly text, never raw "HTTP 401". */
object ApiErrors {
    fun message(e: Exception, fallback: String): String {
        if (e is retrofit2.HttpException) {
            val detail = runCatching {
                val body = e.response()?.errorBody()?.string().orEmpty()
                if (body.isBlank()) null
                else {
                    kotlinx.serialization.json.Json.parseToJsonElement(body)
                        .let { it as? kotlinx.serialization.json.JsonObject }
                        ?.get("detail")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        ?.takeIf { it.isNotBlank() }
                }
            }.getOrNull()
            return when (e.code()) {
                401 -> "🔒 Not signed in on the server (${detail ?: "authentication required"}). " +
                    "On the server set AIKO_USER_ID and restart it."
                403 -> "⛔ Forbidden (${detail ?: fallback}). Check server login / accepted terms."
                404 -> detail ?: "No active game on the server — tap Start to begin a new one."
                400 -> detail ?: fallback
                503 -> "🛠 Server engine unavailable (${detail ?: fallback}). Casual mode will be used."
                else -> if (!detail.isNullOrBlank()) detail else "$fallback (server ${e.code()})"
            }
        }
        val msg = e.message.orEmpty()
        return when {
            "Unable to resolve host" in msg || "Failed to connect" in msg || "timeout" in msg.lowercase() ->
                "📡 Cannot reach the server. Check that Tailscale is connected; " +
                    "the address is baked in at build time (AIKO_PUBLIC_BASE_URL)."
            msg.isNotBlank() -> msg
            else -> fallback
        }
    }
}
