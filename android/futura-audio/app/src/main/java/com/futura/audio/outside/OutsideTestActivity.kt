package com.futura.audio.outside

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.futura.audio.service.FuturaOverlayService
import com.futura.audio.ui.FuturaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

/**
 * Test screen. Saved YouTube links appear as a cover flow of YouTube's public thumbnails.
 * Tapping a cover opens that link in YouTube's own player, on your tap only. Autoplay is
 * blocked. Nothing is downloaded, extracted, or relayed.
 */
class OutsideTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FuturaTheme { OutsideTestScreen() }
        }
    }
}

@Composable
private fun OutsideTestScreen() {
    val context = LocalContext.current
    val prefs = remember {
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(BootTestReceiver.PREFS, Context.MODE_PRIVATE)
    }
    val log = remember { prefs.getString(BootTestReceiver.KEY_LOG, "").orEmpty() }
    var links by remember { mutableStateOf(SavedLinks.load(prefs)) }
    var input by remember { mutableStateOf("") }
    var openUrl by remember { mutableStateOf<String?>(null) }
    val pager = rememberPagerState(pageCount = { links.size })

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).verticalScroll(rememberScrollState())) {
            Text(
                text = log.ifEmpty { "No boot log yet. Restart the phone, wait 2 minutes, then open this screen." },
                style = MaterialTheme.typography.bodySmall,
            )
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("YouTube link") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val link = input.trim()
                if (link.isNotEmpty() && link !in links) {
                    links = links + link
                    SavedLinks.save(prefs, links)
                }
                input = ""
            }) { Text("Save") }
            OutlinedButton(onClick = {
                if (links.isNotEmpty()) {
                    val current = pager.currentPage
                    val removed = links[current]
                    links = links.toMutableList().also { it.removeAt(current) }
                    SavedLinks.save(prefs, links)
                    if (openUrl == removed) openUrl = null
                }
            }) { Text("Remove") }
        }

        if (links.isEmpty()) {
            Text("No saved links yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxWidth().height(260.dp),
                contentPadding = PaddingValues(horizontal = 96.dp),
            ) { page ->
                val url = links[page]
                val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
                Cover(
                    url = url,
                    modifier = Modifier
                        .graphicsLayer {
                            val scale = 1f - 0.2f * abs(offset).coerceAtMost(1f)
                            scaleX = scale
                            scaleY = scale
                            rotationY = (-30f * offset).coerceIn(-30f, 30f)
                        }
                        .clickable { openUrl = url },
                )
            }
        }

        openUrl?.let { url ->
            key(url) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = WebViewClient()
                            loadUrl(url)
                        }
                    },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                )
            }) { Text("Allow float") }
            Button(onClick = {
                if (Settings.canDrawOverlays(context)) {
                    context.startForegroundService(Intent(context, FuturaOverlayService::class.java))
                }
            }) { Text("Show float") }
            OutlinedButton(onClick = {
                context.stopService(Intent(context, FuturaOverlayService::class.java))
            }) { Text("Hide float") }
        }
    }
}

@Composable
private fun Cover(url: String, modifier: Modifier) {
    val id = SavedLinks.videoId(url)
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = id) {
        value = if (id == null) null else withContext(Dispatchers.IO) { fetchThumbnail(id) }
    }
    Box(
        modifier = modifier
            .width(200.dp)
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = url,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = if (id == null) "No cover" else "Loading...",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** YouTube's public thumbnail for a video. Returns null if it can't be loaded. */
private fun fetchThumbnail(id: String): ImageBitmap? = try {
    val connection = URL("https://i.ytimg.com/vi/$id/hqdefault.jpg").openConnection() as HttpURLConnection
    connection.connectTimeout = 8000
    connection.readTimeout = 8000
    connection.inputStream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
} catch (e: Exception) {
    null
}
