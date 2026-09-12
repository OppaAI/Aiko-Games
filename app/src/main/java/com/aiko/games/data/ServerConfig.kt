package com.aiko.games.data

import android.content.Context
import java.net.URL

/**
 * Which Aiko-chan server the app talks to.
 * Same pattern as Aiko-Lingo: in-app editable, no hardcoded network hosts in call sites.
 */
object ServerConfig {

    const val DEFAULT_URL = "https://aiko.ide-chroma.ts.net/"

    private const val PREFS_NEW = "aiko_games_server"
    private const val PREFS_OLD = "aiko_shogi_server"
    private const val KEY_URL = "server_url"
    private const val KEY_SECRET = "app_secret"

    fun get(context: Context): String {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NEW, Context.MODE_PRIVATE)
        // Migrate from pre-rename prefs once.
        if (!prefs.contains(KEY_URL)) {
            val old = context.applicationContext
                .getSharedPreferences(PREFS_OLD, Context.MODE_PRIVATE)
                .getString(KEY_URL, null)
            if (!old.isNullOrBlank()) {
                prefs.edit().putString(KEY_URL, normalize(old)).apply()
            }
        }
        val raw = prefs.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        return normalize(raw)
    }

    fun set(context: Context, raw: String): String {
        val normalized = normalize(raw)
        context.applicationContext
            .getSharedPreferences(PREFS_NEW, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URL, normalized)
            .apply()
        return normalized
    }

    fun getSecret(context: Context): String {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NEW, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_SECRET)) {
            val old = context.applicationContext
                .getSharedPreferences(PREFS_OLD, Context.MODE_PRIVATE)
                .getString(KEY_SECRET, "")
            if (!old.isNullOrBlank()) {
                prefs.edit().putString(KEY_SECRET, old.trim()).apply()
            }
        }
        return prefs.getString(KEY_SECRET, "").orEmpty().trim()
    }

    fun setSecret(context: Context, raw: String): String {
        val cleaned = raw.trim()
        context.applicationContext
            .getSharedPreferences(PREFS_NEW, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SECRET, cleaned)
            .apply()
        return cleaned
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
