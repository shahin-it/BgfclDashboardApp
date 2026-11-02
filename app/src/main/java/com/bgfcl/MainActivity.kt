package com.bgfcl

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgfcl.ui.components.WebViewContainer
import com.bgfcl.ui.theme.MyApplicationTheme
import com.bgfcl.util.DownloadHelper
import com.bgfcl.util.NetworkObserver
import com.bgfcl.util.NetworkStatus

class MainActivity : ComponentActivity() {

    companion object {
        const val BGFCL_URL = "http://180.211.173.178:3000"
    }

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (filePathCallback == null) return@registerForActivityResult

        val uris: Array<Uri>? = when {
            result.resultCode == Activity.RESULT_OK -> {
                val data = result.data
                val clipData = data?.clipData
                val singleUri = data?.data

                when {
                    clipData != null && clipData.itemCount > 0 -> {
                        Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    }
                    singleUri != null -> arrayOf(singleUri)
                    else -> null
                }
            }
            else -> null
        }

        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val networkObserver = NetworkObserver(this)

        setContent {
            MyApplicationTheme {
                val networkStatus by networkObserver.status.collectAsStateWithLifecycle(
                    initialValue = if (networkObserver.isConnected()) NetworkStatus.CONNECTED else NetworkStatus.DISCONNECTED
                )

                DashboardApp(
                    networkStatus = networkStatus,
                    onLaunchFileChooser = { callback, params ->
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = callback

                        try {
                            val intent = params.createIntent().apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            fileChooserLauncher.launch(intent)
                            true
                        } catch (_: Exception) {
                            try {
                                val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "*/*"
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    putExtra(
                                        Intent.EXTRA_ALLOW_MULTIPLE,
                                        params.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
                                    )
                                }
                                fileChooserLauncher.launch(fallbackIntent)
                                true
                            } catch (_: Exception) {
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = null
                                false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardApp(
    networkStatus: NetworkStatus,
    onLaunchFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    WebViewContainer(
        url = MainActivity.BGFCL_URL,
        isOnline = networkStatus == NetworkStatus.CONNECTED,
        onNavigationStateChanged = { back -> canGoBack = back },
        onDownloadRequested = { url, userAgent, contentDisposition, mimetype ->
            val result = DownloadHelper.startDownload(
                context = context,
                url = url,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimetype = mimetype
            )
            val message = if (result.success) {
                "Downloading: ${result.fileName}"
            } else {
                "Download failed: ${result.errorMessage ?: "Unknown error"}"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
        onShowFileChooser = onLaunchFileChooser,
        onWebViewCreated = { webView ->
            webViewInstance = webView
        },
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    )
}
