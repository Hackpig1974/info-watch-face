package com.damon1974.infowatchface.phone

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.damon1974.infowatchface.phone.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val webView = binding.webView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        // Manual button at bottom of screen
        binding.btnDone.setOnClickListener { captureSession() }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                Toast.makeText(this@LoginActivity, "URL: $url", Toast.LENGTH_SHORT).show()
                if (url.contains("claude.ai") && !url.contains("login") && !url.contains("onboarding")) {
                    captureSession()
                }
            }
        }

        webView.loadUrl("https://claude.ai/login")
    }

    private fun captureSession() {
        val cookies = CookieManager.getInstance().getCookie("https://claude.ai")
        if (cookies == null) {
            Toast.makeText(this, "No cookies found - please log in first", Toast.LENGTH_LONG).show()
            return
        }
        val sessionKey = cookies.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("sessionKey=") }
            ?.removePrefix("sessionKey=")

        if (sessionKey == null) {
            Toast.makeText(this, "No session key found in cookies", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "Session captured! Fetching org...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            SecurePrefs.saveSession(applicationContext, sessionKey, "")
            val orgId = try { ClaudePoller.fetchOrgId(sessionKey) } catch (e: Exception) {
                null
            }
            if (orgId != null) {
                SecurePrefs.saveSession(applicationContext, sessionKey, orgId)
            }
            withContext(Dispatchers.Main) {
                val msg = if (orgId != null) "Logged in! Org: $orgId" else "Session saved, org fetch failed"
                Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_LONG).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }
}
