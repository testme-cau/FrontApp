@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.testme.ui.screens.subject

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.testme.data.api.ApiService
import com.example.testme.data.model.PdfDownloadResponse
import com.example.testme.ui.components.SoftBlobBackground
import com.example.testme.ui.components.TestMeTopAppBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder

data class PdfPreviewUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val originalFilename: String = "",
    val downloadUrl: String? = null
)

class PdfPreviewViewModel(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String,
    private val fileId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfPreviewUiState())
    val uiState: StateFlow<PdfPreviewUiState> = _uiState

    init {
        loadPdf()
    }

    private fun loadPdf() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val download: PdfDownloadResponse =
                    apiService.getPdfDownloadUrl("Bearer $token", subjectId, fileId)

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    originalFilename = "",
                    downloadUrl = download.downloadUrl,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "PDF를 불러오는 중 오류가 발생했습니다."
                )
            }
        }
    }

    fun retry() {
        loadPdf()
    }
}

class PdfPreviewViewModelFactory(
    private val apiService: ApiService,
    private val token: String,
    private val subjectId: String,
    private val fileId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfPreviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PdfPreviewViewModel(apiService, token, subjectId, fileId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun PdfPreviewScreen(
    navController: NavController,
    apiService: ApiService,
    token: String,
    subjectId: String,
    fileId: String,
    viewModel: PdfPreviewViewModel = viewModel(
        factory = PdfPreviewViewModelFactory(apiService, token, subjectId, fileId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val brandPrimary = Color(0xFF5BA27F)
    val brandSecondaryText = Color(0xFF4C6070)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TestMeTopAppBar(
                title = if (uiState.originalFilename.isNotBlank())
                    uiState.originalFilename
                else
                    "PDF 미리보기",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 배경
            SoftBlobBackground()

            when {
                uiState.loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "PDF를 불러오는 중입니다...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = brandSecondaryText
                            )
                        }
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.96f)
                            ),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "PDF를 불러오는 데 실패했습니다.",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    uiState.error ?: "알 수 없는 오류",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = brandSecondaryText
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.retry() }) {
                                    Text("다시 시도")
                                }
                            }
                        }
                    }
                }

                uiState.downloadUrl != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // 미리보기 카드
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.98f)
                            ),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        webViewClient = object : WebViewClient() {}
                                        val encodedUrl = URLEncoder.encode(
                                            uiState.downloadUrl,
                                            "UTF-8"
                                        )
                                        loadUrl(
                                            "https://docs.google.com/gview?embedded=1&url=$encodedUrl"
                                        )
                                    }
                                },
                                update = { webView ->
                                    val encodedUrl = URLEncoder.encode(
                                        uiState.downloadUrl,
                                        "UTF-8"
                                    )
                                    webView.loadUrl(
                                        "https://docs.google.com/gview?embedded=1&url=$encodedUrl"
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 하단 버튼들
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    uiState.downloadUrl?.let { url ->
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("다른 앱으로 열기")
                            }

                            Button(
                                onClick = {
                                    uiState.downloadUrl?.let { url ->
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brandPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("PDF 다운로드")
                            }
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("표시할 PDF 정보가 없습니다.")
                    }
                }
            }
        }
    }
}