package com.aiko.shogi.data

import android.content.Context
import java.net.URL

/**
 * Which Aiko-chan server the app talks to.
 * Same pattern as Aiko-Lingo: in-app editable, no hardcoded network hosts in call sites.
 */
object ServerConfig {

    const val DEFAULT_URL = "https://aiko.ide-chroma.ts.net/"

    private const val PREFS = "aiko_shogi_server"
    private const val KEY_URL = "server_url"

    fun get(context: Context): String {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        return normalize(raw)
    }

    fun set(context: Context, raw: String): String {
        val normalized = normalize(raw)
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URL, normalized)
            .apply()
        return normalized
    }

    fun normalize(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return DEFAULT_URL
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "https://$s"
        }
        return s.removeSuffix("/") + "/"
    }

    fun isValid(raw: String): Boolean {
        return try {
            val url = URL(normalize(raw))
            (url.protocol == "http" || url.protocol == "https") && url.host.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }

    fun displayHost(url: String): String {
        return try {
            URL(url).host.ifBlank { url }
        } catch (_: Exception) {
            url
        }
    }
}
