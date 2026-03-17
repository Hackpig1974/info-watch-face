package com.damon1974.infowatchface.phone

import android.content.Context

object UsageCache {
    private const val PREFS = "usage_cache"
    private const val KEY_SESSION = "session_pct"
    private const val KEY_WEEKLY = "weekly_pct"

    fun save(context: Context, sessionPct: Int, weeklyPct: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_SESSION, sessionPct)
            .putInt(KEY_WEEKLY, weeklyPct)
            .apply()
    }

    fun sessionPct(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_SESSION, 0)

    fun weeklyPct(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_WEEKLY, 0)
}
