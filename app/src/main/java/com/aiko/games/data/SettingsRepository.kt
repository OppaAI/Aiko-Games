package com.aiko.games.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aiko_games_prefs", Context.MODE_PRIVATE)

    fun isDarkTheme(): Boolean = prefs.getBoolean("dark_theme", false)
    fun setDarkTheme(enabled: Boolean) = prefs.edit().putBoolean("dark_theme", enabled).apply()

    fun getShogiDifficulty(): String = prefs.getString("shogi_difficulty", "medium") ?: "medium"
    fun setShogiDifficulty(level: String) = prefs.edit().putString("shogi_difficulty", level).apply()

    fun getShogiSide(): String = prefs.getString("shogi_side", "black") ?: "black"
    fun setShogiSide(side: String) = prefs.edit().putString("shogi_side", side).apply()

    fun isShogiEngineEnabled(): Boolean = prefs.getBoolean("shogi_engine_enabled", true)
    fun setShogiEngineEnabled(enabled: Boolean) = prefs.edit().putBoolean("shogi_engine_enabled", enabled).apply()

    fun getGoDifficulty(): String = prefs.getString("go_difficulty", "medium") ?: "medium"
    fun setGoDifficulty(level: String) = prefs.edit().putString("go_difficulty", level).apply()

    fun getGoSide(): String = prefs.getString("go_side", "black") ?: "black"
    fun setGoSide(side: String) = prefs.edit().putString("go_side", side).apply()

    fun getGoBoardSize(): Int = prefs.getInt("go_board_size", 9)
    fun setGoBoardSize(size: Int) = prefs.edit().putInt("go_board_size", size).apply()

    fun isGoEngineEnabled(): Boolean = prefs.getBoolean("go_engine_enabled", true)
    fun setGoEngineEnabled(enabled: Boolean) = prefs.edit().putBoolean("go_engine_enabled", enabled).apply()

    fun getKoiMonths(): Int = prefs.getInt("koi_months", 6)
    fun setKoiMonths(n: Int) = prefs.edit().putInt("koi_months", n).apply()

    fun getKoiDifficulty(): String = prefs.getString("koi_difficulty", "medium") ?: "medium"
    fun setKoiDifficulty(level: String) = prefs.edit().putString("koi_difficulty", level).apply()
}
