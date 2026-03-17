package com.damon1974.infowatchface.phone

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UsageData(
    val sessionPct: Int,
    val weeklyPct: Int
)

object ClaudePoller {

    private val client = OkHttpClient()

    private const val UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    fun fetchOrgId(sessionKey: String): String? {
        val request = Request.Builder()
            .url("https://claude.ai/api/organizations")
            .header("Cookie", "sessionKey=$sessionKey")
            .header("User-Agent", UA)
            .header("Accept", "application/json")
            .build()
        val body = client.newCall(request).execute().use { it.body?.string() } ?: return null
        val arr = org.json.JSONArray(body)
        return if (arr.length() > 0) arr.getJSONObject(0).getString("uuid") else null
    }

    fun fetchUsage(sessionKey: String, orgId: String): UsageData? {
        val request = Request.Builder()
            .url("https://claude.ai/api/organizations/$orgId/usage")
            .header("Cookie", "sessionKey=$sessionKey")
            .header("User-Agent", UA)
            .header("Accept", "application/json")
            .build()
        val body = client.newCall(request).execute().use { it.body?.string() } ?: return null
        val json = JSONObject(body)
        val session = json.optJSONObject("five_hour")?.optDouble("utilization") ?: 0.0
        val weekly = json.optJSONObject("seven_day")?.optDouble("utilization") ?: 0.0
        return UsageData(session.toInt(), weekly.toInt())
    }
}
