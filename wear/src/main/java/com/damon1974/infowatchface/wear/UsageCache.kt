package com.damon1974.infowatchface.wear

import android.content.Context

object UsageCache {
    private const val PREFS = "usage_cache"
    const val KEY_SESSION = "session_pct"
    const val KEY_WEEKLY = "weekly_pct"
    const val KEY_MC_ONLINE = "mc_online"
    const val KEY_MC_MAX = "mc_max"
    const val KEY_RUST_ONLINE = "rust_online"
    const val KEY_RUST_MAX = "rust_max"

    fun save(context: Context, sessionPct: Int, weeklyPct: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_SESSION, sessionPct)
            .putInt(KEY_WEEKLY, weeklyPct)
            .apply()
    }

    fun saveMc(context: Context, online: Int, max: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_MC_ONLINE, online)
            .putInt(KEY_MC_MAX, max)
            .apply()
    }

    fun saveRust(context: Context, online: Int, max: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_RUST_ONLINE, online)
            .putInt(KEY_RUST_MAX, max)
            .apply()
    }

    fun sessionPct(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_SESSION, 0)

    fun weeklyPct(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_WEEKLY, 0)

    fun mcOnline(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MC_ONLINE, -1)

    fun mcMax(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MC_MAX, -1)

    fun rustOnline(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_RUST_ONLINE, -1)

    fun rustMax(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_RUST_MAX, -1)
}
