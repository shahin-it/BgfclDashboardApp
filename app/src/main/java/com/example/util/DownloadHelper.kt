package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import java.io.File
import java.net.URLDecoder

object DownloadHelper {

    data class DownloadResult(
        val success: Boolean,
        val downloadId: Long = -1L,
        val fileName: String = "",
        val errorMessage: String? = null
    )

    fun startDownload(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?
    ): DownloadResult {
        return try {
            val uri = Uri.parse(url)
            val detectedFileName = resolveFileName(url, contentDisposition, mimetype)
            val resolvedMimeType = resolveMimeType(detectedFileName, mimetype)

            val request = DownloadManager.Request(uri).apply {
                setTitle(detectedFileName)
                setDescription("Downloading file from BGFCL Dashboard")
                setMimeType(resolvedMimeType)

                // Attach cookies for authenticated sessions
                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrBlank()) {
                    addRequestHeader("Cookie", cookies)
                }

                if (!userAgent.isNullOrBlank()) {
                    addRequestHeader("User-Agent", userAgent)
                }

                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, detectedFileName)
            }

            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            DownloadResult(
                success = true,
                downloadId = downloadId,
                fileName = detectedFileName
            )
        } catch (e: Exception) {
            DownloadResult(
                success = false,
                errorMessage = e.localizedMessage ?: "Failed to start download"
            )
        }
    }

    private fun resolveFileName(
        url: String,
        contentDisposition: String?,
        mimetype: String?
    ): String {
        // Try Content-Disposition header first (e.g. filename="report.pdf" or filename*=UTF-8''report.pdf)
        if (!contentDisposition.isNullOrBlank()) {
            val cd = contentDisposition.trim()
            val filenameUtf8Regex = Regex("filename\\*\\s*=\\s*UTF-8''([^;]+)", RegexOption.IGNORE_CASE)
            val matchUtf8 = filenameUtf8Regex.find(cd)
            if (matchUtf8 != null) {
                try {
                    return URLDecoder.decode(matchUtf8.groupValues[1].trim('"', '\''), "UTF-8")
                } catch (_: Exception) {}
            }

            val filenameRegex = Regex("filename\\s*=\\s*\"?([^\";]+)\"?", RegexOption.IGNORE_CASE)
            val match = filenameRegex.find(cd)
            if (match != null) {
                val candidate = match.groupValues[1].trim('"', '\'')
                if (candidate.isNotBlank()) {
                    return candidate
                }
            }
        }

        // Fallback to URLUtil
        val guessed = URLUtil.guessFileName(url, contentDisposition, mimetype)
        if (!guessed.isNullOrBlank() && guessed != "downloadfile.bin") {
            return guessed
        }

        // Try extracting from URL path
        try {
            val uri = Uri.parse(url)
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrBlank()) {
                val decoded = URLDecoder.decode(lastSegment, "UTF-8")
                if (decoded.contains(".")) {
                    return decoded
                }
            }
        } catch (_: Exception) {}

        return guessed ?: "bgfcl_download.bin"
    }

    private fun resolveMimeType(fileName: String, fallbackMimeType: String?): String {
        if (!fallbackMimeType.isNullOrBlank() && fallbackMimeType != "application/octet-stream") {
            return fallbackMimeType
        }
        val extension = File(fileName).extension.lowercase()
        if (extension.isNotBlank()) {
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (!mime.isNullOrBlank()) {
                return mime
            }
        }
        return fallbackMimeType ?: "application/octet-stream"
    }

    fun openDownloadsFolder(context: Context) {
        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }
}
