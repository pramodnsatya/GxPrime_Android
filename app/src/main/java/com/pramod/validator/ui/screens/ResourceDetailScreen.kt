package com.pramod.validator.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.pramod.validator.data.models.TrainingResource
import com.pramod.validator.data.models.TrainingResourceType
import com.pramod.validator.viewmodel.TrainingResourcesViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceDetailScreen(
    resourceId: String,
    onNavigateBack: () -> Unit,
    onNavigateToResources: () -> Unit,
    viewModel: TrainingResourcesViewModel
) {
    val resources by viewModel.resources.collectAsState()
    val resource = resources.firstOrNull { it.id == resourceId }

    LaunchedEffect(resourceId) {
        if (resource == null) {
            viewModel.refreshResources()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = Color.White
            ) {
            TopAppBar(
                    title = { 
                        Text(
                            resource?.title ?: "Resource",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        ) 
                    },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A) // slate-900
                            )
                    }
                },
                    actions = {
                        if (resource != null) {
                            val context = LocalContext.current
                            val externalUrl = when {
                                resource.type == TrainingResourceType.VIDEO && resource.videoUrl.isNotBlank() -> resource.videoUrl
                                resource.resourceUrl.isNotBlank() -> resource.resourceUrl
                                else -> null
                            }
                            if (externalUrl != null) {
                                IconButton(onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, externalUrl.toUri())
                                    context.startActivity(intent)
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Open externally",
                                        tint = Color(0xFF1E3A8A) // blue-900
                                    )
                                }
                            }
                        }
                    },
                colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF0F172A), // slate-900
                        navigationIconContentColor = Color(0xFF0F172A), // slate-900
                        actionIconContentColor = Color(0xFF1E3A8A) // blue-900
                    )
                )
            }
        },
        containerColor = Color(0xFFF8FAFC) // slate-50
    ) { padding ->
        if (resource == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading resource…")
            }
        } else {
            ResourceDetailContent(
                resource = resource,
                contentPadding = padding,
                onNavigateToResources = onNavigateToResources
            )
        }
    }
}

@Composable
private fun ResourceDetailContent(
    resource: TrainingResource,
    contentPadding: PaddingValues,
    onNavigateToResources: () -> Unit
) {
    // For articles, PDFs, and other web content, show full screen WebView
    when (resource.type) {
        TrainingResourceType.ARTICLE -> {
            if (resource.resourceUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                ) {
                    ArticleWebView(url = resource.resourceUrl)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    MissingResourceMessage("Article link is unavailable")
                }
            }
        }
        TrainingResourceType.FDA483, TrainingResourceType.PDF, TrainingResourceType.OTHER -> {
            if (resource.resourceUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                ) {
                    PdfWebView(url = resource.resourceUrl)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    MissingResourceMessage("File is unavailable")
                }
            }
        }
        TrainingResourceType.VIDEO -> {
            // For videos, show mini player with back button
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .background(Color(0xFFF8FAFC)) // slate-50
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Mini video player with 16:9 aspect ratio
                if (resource.videoUrl.isNotBlank()) {
                    YoutubePlayerView(videoUrl = resource.videoUrl)
                } else {
                    MissingResourceMessage("Video link is unavailable")
                }
                
                // Back button
                Button(
                    onClick = onNavigateToResources,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E3A8A) // blue-900
                    )
                ) {
                    Text(
                        "Back to Resources",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourceTypeChip(type: TrainingResourceType) {
    val (label, color) = when (type) {
        TrainingResourceType.VIDEO -> "Video" to MaterialTheme.colorScheme.primary
        TrainingResourceType.ARTICLE -> "Article" to MaterialTheme.colorScheme.secondary
        TrainingResourceType.FDA483 -> "FDA 483" to MaterialTheme.colorScheme.tertiary
        TrainingResourceType.PDF -> "PDF" to MaterialTheme.colorScheme.primary
        TrainingResourceType.OTHER -> "Resource" to MaterialTheme.colorScheme.secondary
    }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = color
        )
    )
}

@Composable
private fun ResourceViewer(resource: TrainingResource) {
    val resourceUrl = resource.resourceUrl
    when (resource.type) {
        TrainingResourceType.VIDEO -> {
            if (resource.videoUrl.isNotBlank()) {
                YoutubePlayerView(videoUrl = resource.videoUrl)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    MissingResourceMessage("Video link is unavailable")
                }
            }
        }
        TrainingResourceType.ARTICLE -> {
            // Articles are handled separately in ResourceDetailContent
            if (resourceUrl.isNotBlank()) {
                ArticleWebView(url = resourceUrl)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    MissingResourceMessage("Article link is unavailable")
                }
            }
        }
        TrainingResourceType.FDA483, TrainingResourceType.PDF, TrainingResourceType.OTHER -> {
            if (resourceUrl.isNotBlank()) {
                PdfWebView(url = resourceUrl)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    MissingResourceMessage("File is unavailable")
                }
            }
        }
    }
}

@Composable
private fun MissingResourceMessage(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PdfWebView(url: String) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.setSupportZoom(true)
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            hasError = false
                        }
                        
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }
                        
                        override fun onReceivedError(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            hasError = true
                            isLoading = false
                        }
                    }
                    
                    // Try multiple PDF viewer approaches
                    when {
                        url.startsWith("http://") || url.startsWith("https://") -> {
                            // Use Google Docs viewer for remote PDFs
                            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                            loadUrl("https://docs.google.com/viewer?url=$encodedUrl&embedded=true")
                        }
                        else -> {
                            // Direct load for local or other URLs
                            loadUrl(url)
                        }
                    }
                }
            },
            update = { webView ->
                if (hasError) {
                    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                    webView.loadUrl("https://docs.google.com/viewer?url=$encodedUrl&embedded=true")
                }
            }
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArticleWebView(url: String) {
    var isLoading by remember { mutableStateOf(true) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = true
                    settings.setSupportZoom(true)
                    settings.textZoom = 100
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                        }
                        
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }
                        
                        override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                            // Keep navigation within the WebView
                            return false
                        }
                    }
                    
                    loadUrl(url)
                }
            },
            update = { webView ->
                if (webView.url != url) {
                    webView.loadUrl(url)
                }
            }
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YoutubePlayerView(videoUrl: String) {
    val embedUrl = remember(videoUrl) { buildYoutubeEmbedUrl(videoUrl) }
    var hasError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // If embed URL is invalid or there's an error, show button to open in browser
    if (embedUrl == null || hasError) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(20.dp)
            ) {
                MissingResourceMessage(
                    if (embedUrl == null) {
                        "Invalid YouTube link. Click to open in browser."
                    } else {
                        "Video cannot be embedded. Click to open in browser."
                    }
                )
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, videoUrl.toUri())
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626) // red-600
                    )
                ) {
                    Text("Open in YouTube", color = Color.White)
                }
            }
        }
        return
    }
    
    // Mini player with 16:9 aspect ratio - user can click for fullscreen
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f), // 16:9 aspect ratio for mini player
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            hasError = true
                        }
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Check if page loaded successfully
                        evaluateJavascript("document.body.innerHTML", null)
                    }
                }
                
                // Use embed URL - user can click fullscreen button in YouTube player
                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            * { margin: 0; padding: 0; box-sizing: border-box; }
                            html, body { width: 100%; height: 100%; }
                            body { display: flex; align-items: center; justify-content: center; background: #000; }
                            iframe { width: 100%; height: 100%; border: 0; }
                        </style>
                    </head>
                    <body>
                        <iframe 
                            src="$embedUrl?autoplay=0&rel=0&modestbranding=1&playsinline=1&enablejsapi=1" 
                            frameborder="0" 
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                            allowfullscreen>
                        </iframe>
                    </body>
                    </html>
                """.trimIndent()
                
                loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            // No need to reload if URL hasn't changed
        }
    )
}

private fun buildYoutubeEmbedUrl(url: String): String? {
    val uri = url.toUri()
    val videoId = when {
        uri.host?.contains("youtu.be") == true -> uri.lastPathSegment
        uri.host?.contains("youtube.com") == true -> uri.getQueryParameter("v")
        uri.path?.contains("embed") == true -> uri.lastPathSegment
        else -> null
    }
    return videoId?.let { "https://www.youtube.com/embed/$it" }
}