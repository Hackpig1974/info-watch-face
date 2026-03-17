package com.damon1974.infowatchface.phone

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PollWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val sessionKey = SecurePrefs.getSessionKey(ctx) ?: return Result.failure()
        val orgId = SecurePrefs.getOrgId(ctx)?.takeIf { it.isNotEmpty() } ?: run {
            val fetched = try { ClaudePoller.fetchOrgId(sessionKey) } catch (e: Exception) {
                Log.e("PollWorker", "fetchOrgId failed: ${e.message}")
                return Result.retry()
            } ?: return Result.failure()
            SecurePrefs.saveSession(ctx, sessionKey, fetched)
            fetched
        }

        val usage = try {
            ClaudePoller.fetchUsage(sessionKey, orgId)
        } catch (e: Exception) {
            Log.e("PollWorker", "fetchUsage failed: ${e.message}")
            return Result.retry()
        } ?: return Result.retry()

        UsageCache.save(ctx, usage.sessionPct, usage.weeklyPct)

        // Poll Minecraft server status
        val mcStatus = withContext(Dispatchers.IO) {
            MinecraftPoller.fetchStatus("mcj.thehackpig.com", 25565)
        }
        val mcOnline = mcStatus?.online ?: -1
        val mcMax = mcStatus?.max ?: -1
        Log.d("PollWorker", "Minecraft: online=$mcOnline max=$mcMax")

        // Poll Rust server status
        val rustStatus = withContext(Dispatchers.IO) {
            RustPoller.fetchStatus("rust.thehackpig.com", 28017)
        }
        val rustOnline = rustStatus?.online ?: -1
        val rustMax = rustStatus?.max ?: -1
        Log.d("PollWorker", "Rust: online=$rustOnline max=$rustMax")

        // Push to watch via Data Layer
        try {
            val putDataReq = PutDataMapRequest.create("/usage").apply {
                dataMap.putInt("session_pct", usage.sessionPct)
                dataMap.putInt("weekly_pct", usage.weeklyPct)
                dataMap.putInt("mc_online", mcOnline)
                dataMap.putInt("mc_max", mcMax)
                dataMap.putInt("rust_online", rustOnline)
                dataMap.putInt("rust_max", rustMax)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(ctx).putDataItem(putDataReq).await()
            Log.d("PollWorker", "Pushed to watch: session=${usage.sessionPct}% weekly=${usage.weeklyPct}% mc=$mcOnline/$mcMax rust=$rustOnline/$rustMax")
        } catch (e: Exception) {
            Log.e("PollWorker", "Data Layer push failed: ${e.message}")
        }

        // Local broadcast for MainActivity UI
        val intent = android.content.Intent(ACTION_USAGE_UPDATE).apply {
            putExtra(EXTRA_SESSION, usage.sessionPct)
            putExtra(EXTRA_WEEKLY, usage.weeklyPct)
        }
        ctx.sendBroadcast(intent)

        return Result.success()
    }

    companion object {
        const val ACTION_USAGE_UPDATE = "com.damon1974.infowatchface.USAGE_UPDATE"
        const val EXTRA_SESSION = "session_pct"
        const val EXTRA_WEEKLY = "weekly_pct"
        const val WORK_NAME = "claude_poll"
    }
}
