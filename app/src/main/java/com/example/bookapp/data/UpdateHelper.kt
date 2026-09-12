package com.example.bookapp.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** بررسی دستی بروزرسانی؛ استفاده عادی برنامه کاملاً آفلاین باقی می‌ماند. */
object UpdateHelper {
    private const val REPO = "lavialireza/taziehappv3"
    private const val RELEASES_API = "https://api.github.com/repos/$REPO/releases?per_page=10"

    data class UpdateInfo(
        val buildNumber: Int,
        val tagName: String,
        val downloadUrl: String
    )

    suspend fun checkForUpdate(currentVersionCode: Int): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Tazieh-Android-Updater")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException("خطای سرور: ${connection.responseCode}")
                }
                val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val releases = JSONArray(json)
                var best: UpdateInfo? = null
                for (i in 0 until releases.length()) {
                    val release = releases.getJSONObject(i)
                    val tag = release.optString("tag_name")
                    val match = Regex("^apk-build-(\\d+)$").find(tag) ?: continue
                    val buildNumber = match.groupValues[1].toIntOrNull() ?: continue
                    val assets = release.optJSONArray("assets") ?: continue
                    var apkUrl: String? = null
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val name = asset.optString("name")
                        if (name == if (currentVersionCode >= 0) "app-viewer-debug.apk" else "app-viewer-debug.apk") {
                            apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                            break
                        }
                    }
                    if (apkUrl != null && buildNumber > currentVersionCode && (best == null || buildNumber > best!!.buildNumber)) {
                        best = UpdateInfo(buildNumber, tag, apkUrl)
                    }
                }
                best
            } finally {
                connection.disconnect()
            }
        }
    }

    fun openDownloadPage(context: Context, url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
