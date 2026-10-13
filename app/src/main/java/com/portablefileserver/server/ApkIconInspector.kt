package com.portablefileserver.server

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.LinkedHashMap

/**
 * Reads package metadata and the embedded launcher icon from APK files.
 * It never installs or executes the uploaded package.
 */
class ApkIconInspector(context: Context) {
    data class Result(
        val packageName: String,
        val label: String,
        val iconDataUrl: String?
    )

    private val packageManager = context.applicationContext.packageManager
    private val cache = object : LinkedHashMap<String, Result?>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Result?>?): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    @Synchronized
    fun inspect(file: File): Result? {
        if (!file.isFile || !file.name.endsWith(".apk", ignoreCase = true)) return null
        // Package parsing is metadata-only, but bound the work done on a network request.
        if (file.length() !in 1..MAX_APK_BYTES) return null

        val key = "${file.absolutePath}:${file.length()}:${file.lastModified()}"
        if (cache.containsKey(key)) return cache[key]

        val result = try {
            val packageInfo = readPackageInfo(file) ?: return remember(key, null)
            val appInfo = packageInfo.applicationInfo ?: return remember(key, null)
            appInfo.sourceDir = file.absolutePath
            appInfo.publicSourceDir = file.absolutePath
            val label = packageManager.getApplicationLabel(appInfo).toString()
            val iconDataUrl = try {
                val drawable = packageManager.getApplicationIcon(appInfo)
                val bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).also { canvas ->
                    drawable.setBounds(0, 0, ICON_SIZE, ICON_SIZE)
                    drawable.draw(canvas)
                }
                val png = ByteArrayOutputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    bitmap.recycle()
                    output.toByteArray()
                }
                "data:image/png;base64," + Base64.encodeToString(png, Base64.NO_WRAP)
            } catch (_: Exception) {
                null
            }
            Result(
                packageName = appInfo.packageName,
                label = label,
                iconDataUrl = iconDataUrl
            )
        } catch (_: Exception) {
            null
        }
        return remember(key, result)
    }

    @Suppress("DEPRECATION")
    private fun readPackageInfo(file: File): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                file.absolutePath,
                PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        }

    private fun remember(key: String, value: Result?): Result? {
        cache[key] = value
        return value
    }

    private companion object {
        const val ICON_SIZE = 96
        const val MAX_APK_BYTES = 256L * 1024L * 1024L
        const val MAX_CACHE_ENTRIES = 128
    }
}
