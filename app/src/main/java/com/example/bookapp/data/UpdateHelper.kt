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
    private const val RELEASES_API = "https://api.github.com/repos/$REPO/releases?per_page=20"

    data class UpdateInfo(
        val buildNumber: Int,
        val tagName: String,
        val downloadUrl: String,
        val isReleaseApk: Boolean
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
                val viewer = com.example.bookapp.BuildConfig.PUBLIC_VIEWER
                val releaseAsset = if (viewer) "app-viewer-release.apk" else "app-admin-release.apk"
                val debugAsset = if (viewer) "app-viewer-debug.apk" else "app-admin-debug.apk"

                var best: UpdateInfo? = null
                for (i in 0 until releases.length()) {
                    val release = releases.getJSONObject(i)
                    if (release.optBoolean("draft", false)) continue

                    val tag = release.optString("tag_name")
                    val match = Regex("^apk-build-(\\d+)$").find(tag) ?: continue
                    val buildNumber = match.groupValues[1].toIntOrNull() ?: continue
                    if (buildNumber <= currentVersionCode) continue

                    val assets = release.optJSONArray("assets") ?: continue
                    var apkUrl: String? = null
                    var isReleaseApk = false

                    // برای انتشار رسمی همیشه Release APK اولویت دارد؛
                    // در buildهای آزمایشی اگر Release APK نبود، Debug APK استفاده می‌شود.
                    for (assetIndex in 0 until assets.length()) {
                        val asset = assets.getJSONObject(assetIndex)
                        if (asset.optString("name") == releaseAsset) {
                            apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                            isReleaseApk = apkUrl != null
                            if (isReleaseApk) break
                        }
                    }
                    if (apkUrl == null) {
                        for (assetIndex in 0 until assets.length()) {
                            val asset = assets.getJSONObject(assetIndex)
                            if (asset.optString("name") == debugAsset) {
                                apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                                break
                            }
                        }
                    }

                    if (apkUrl != null && (best == null || buildNumber > best!!.buildNumber ||
                                (buildNumber == best!!.buildNumber && isReleaseApk && !best!!.isReleaseApk))) {
                        best = UpdateInfo(buildNumber, tag, apkUrl, isReleaseApk)
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
