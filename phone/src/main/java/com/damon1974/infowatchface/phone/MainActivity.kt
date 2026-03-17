package com.damon1974.infowatchface.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.damon1974.infowatchface.phone.databinding.ActivityMainBinding
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Handler(Looper.getMainLooper()).postDelayed({
            updateStatus()
            if (SecurePrefs.isLoggedIn(this)) {
                schedulePoll()
                refreshNow()
            }
        }, 500)
    }


    private val usageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val session = intent.getIntExtra(PollWorker.EXTRA_SESSION, -1)
            val weekly = intent.getIntExtra(PollWorker.EXTRA_WEEKLY, -1)
            binding.tvSession.text = "Session: $session%"
            binding.tvWeekly.text = "Weekly: $weekly%"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            loginLauncher.launch(Intent(this, LoginActivity::class.java))
        }
        binding.btnRefresh.setOnClickListener { refreshNow() }
        binding.btnLogout.setOnClickListener {
            SecurePrefs.clear(this)
            WorkManager.getInstance(this).cancelUniqueWork(PollWorker.WORK_NAME)
            updateStatus()
            binding.tvSession.text = "Session: --"
            binding.tvWeekly.text = "Weekly: --"
        }

        updateStatus()
        if (SecurePrefs.isLoggedIn(this)) {
            schedulePoll()
            refreshNow()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(usageReceiver, IntentFilter(PollWorker.ACTION_USAGE_UPDATE),
            RECEIVER_NOT_EXPORTED)
        updateStatus()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                withContext(Dispatchers.Main) {
                    val info = if (nodes.isEmpty()) "No watch" else nodes.joinToString { it.displayName }
                    Toast.makeText(this@MainActivity, "Watch: $info", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Node check failed: ${e.message}")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(usageReceiver)
    }


    private fun updateStatus() {
        val sessionKey = SecurePrefs.getSessionKey(this)
        val orgId = SecurePrefs.getOrgId(this)
        binding.tvStatus.text = when {
            sessionKey == null -> "Status: Not logged in"
            orgId.isNullOrEmpty() -> "Status: Logged in (org ID pending)"
            else -> "Status: Logged in ✓ | Org: ${orgId.take(8)}..."
        }
    }

    private fun schedulePoll() {
        val request = PeriodicWorkRequestBuilder<PollWorker>(10, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PollWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun refreshNow() {
        val sessionKey = SecurePrefs.getSessionKey(this) ?: run {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val orgId = SecurePrefs.getOrgId(this) ?: run {
            Toast.makeText(this, "Org ID not set", Toast.LENGTH_SHORT).show()
            return
        }
        binding.tvSession.text = "Session: fetching..."
        binding.tvWeekly.text = "Weekly: fetching..."
        CoroutineScope(Dispatchers.IO).launch {
            val usage = try { ClaudePoller.fetchUsage(sessionKey, orgId) } catch (e: Exception) { null }
            val mcStatus = try { MinecraftPoller.fetchStatus("mcj.thehackpig.com", 25565) } catch (e: Exception) { null }
            val mcOnline = mcStatus?.online ?: -1
            val mcMax = mcStatus?.max ?: -1
            val rustStatus = try { RustPoller.fetchStatus("rust.thehackpig.com", 28017) } catch (e: Exception) { null }
            val rustOnline = rustStatus?.online ?: -1
            val rustMax = rustStatus?.max ?: -1
            withContext(Dispatchers.Main) {
                if (usage != null) {
                    binding.tvSession.text = "Session: ${usage.sessionPct}%"
                    binding.tvWeekly.text = "Weekly: ${usage.weeklyPct}%"
                    pushToWatch(usage.sessionPct, usage.weeklyPct, mcOnline, mcMax, rustOnline, rustMax)
                } else {
                    binding.tvSession.text = "Session: fetch failed"
                    binding.tvWeekly.text = "Weekly: fetch failed"
                    Toast.makeText(this@MainActivity, "Fetch failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun pushToWatch(sessionPct: Int, weeklyPct: Int, mcOnline: Int = -1, mcMax: Int = -1, rustOnline: Int = -1, rustMax: Int = -1) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                // DataClient
                val req = PutDataMapRequest.create("/usage").apply {
                    dataMap.putInt("session_pct", sessionPct)
                    dataMap.putInt("weekly_pct", weeklyPct)
                    dataMap.putInt("mc_online", mcOnline)
                    dataMap.putInt("mc_max", mcMax)
                    dataMap.putInt("rust_online", rustOnline)
                    dataMap.putInt("rust_max", rustMax)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()
                Wearable.getDataClient(this@MainActivity).putDataItem(req).await()
                // MessageClient for immediate delivery
                val payload = "$sessionPct,$weeklyPct,$mcOnline,$mcMax,$rustOnline,$rustMax".toByteArray()
                nodes.forEach { node ->
                    Wearable.getMessageClient(this@MainActivity)
                        .sendMessage(node.id, "/usage", payload).await()
                    Log.d("MainActivity", "Message sent to ${node.displayName}")
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity,
                        "Watch updated: S=$sessionPct% W=$weeklyPct%", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Push failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Watch push failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
