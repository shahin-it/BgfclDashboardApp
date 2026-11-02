package com.bgfcl.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    url: String,
    isOnline: Boolean,
    onNavigationStateChanged: (canGoBack: Boolean) -> Unit,
    onDownloadRequested: (url: String, userAgent: String?, contentDisposition: String?, mimetype: String?) -> Unit,
    onShowFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true

                allowFileAccess = true
                allowContentAccess = true

                useWideViewPort = true
                loadWithOverviewMode = true

                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false

                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                // Offline caching for better performance
                cacheMode = if (isOnline) {
                    WebSettings.LOAD_DEFAULT
                } else {
                    WebSettings.LOAD_CACHE_ELSE_NETWORK
                }
            }

            // Cookie configuration
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            // File download listener
            setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, _ ->
                onDownloadRequested(downloadUrl, userAgent, contentDisposition, mimetype)
            }

            // WebChromeClient for file uploads (<input type="file">)
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    return filePathCallback != null && fileChooserParams != null &&
                        onShowFileChooser(filePathCallback, fileChooserParams)
                }
            }

            // WebViewClient for navigation and offline fallback
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val uri = request?.url ?: return false
                    return when (uri.scheme?.lowercase()) {
                        "http", "https" -> false
                        else -> {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                true
                            } catch (_: Exception) {
                                false
                            }
                        }
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    view?.let { onNavigationStateChanged(it.canGoBack()) }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.let { onNavigationStateChanged(it.canGoBack()) }
                }
            }
        }.also {
            onWebViewCreated(it)
            it.loadUrl(url)
        }
    }

    // Dynamically adjust cache mode when network connectivity changes
    LaunchedEffect(isOnline) {
        webView.settings.cacheMode = if (isOnline) {
            WebSettings.LOAD_DEFAULT
        } else {
            WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webView.stopLoading()
                webView.destroy()
            } catch (_: Exception) {}
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_webview")
    )
}
