package com.futura.audio.outside

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.futura.audio.service.FuturaOverlayService

/**
 * Test screen. Shows the boot log, saves one YouTube link and opens it in YouTube's own player
 * (only on your tap, autoplay blocked), and turns the floating button on, off, or asks for its
 * permission. Nothing is downloaded, extracted, or relayed.
 */
class OutsideTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = createDeviceProtectedStorageContext()
            .getSharedPreferences(BootTestReceiver.PREFS, MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val log = TextView(this).apply {
            text = prefs.getString(BootTestReceiver.KEY_LOG, "").takeUnless { it.isNullOrEmpty() }
                ?: "No boot log yet. Restart the phone, wait 2 minutes, then open this screen."
            setTextIsSelectable(true)
        }
        val input = EditText(this).apply {
            hint = "YouTube link"
            setText(prefs.getString(BootTestReceiver.KEY_LINK, ""))
        }
        val save = Button(this).apply {
            text = "Save link"
            setOnClickListener {
                prefs.edit().putString(BootTestReceiver.KEY_LINK, input.text.toString().trim()).apply()
            }
        }
        val web = WebView(this).apply { settings.javaScriptEnabled = true }
        val open = Button(this).apply {
            text = "Open saved link"
            setOnClickListener {
                val link = prefs.getString(BootTestReceiver.KEY_LINK, "").orEmpty()
                if (link.isNotEmpty()) web.loadUrl(link)
            }
        }
        val allow = Button(this).apply {
            text = "Allow floating button"
            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                )
            }
        }
        val show = Button(this).apply {
            text = "Show floating button"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@OutsideTestActivity)) {
                    startForegroundService(Intent(this@OutsideTestActivity, FuturaOverlayService::class.java))
                }
            }
        }
        val hide = Button(this).apply {
            text = "Hide floating button"
            setOnClickListener {
                stopService(Intent(this@OutsideTestActivity, FuturaOverlayService::class.java))
            }
        }

        root.addView(log)
        root.addView(input)
        root.addView(save)
        root.addView(open)
        root.addView(allow)
        root.addView(show)
        root.addView(hide)
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 700))
        setContentView(root)
    }
}
