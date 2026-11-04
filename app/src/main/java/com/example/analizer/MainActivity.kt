package com.example.analizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.analizer.ui.SpreadsheetTableScreen
import com.example.analizer.ui.components.ImportProgressDialog
import com.example.analizer.ui.screens.AnalyticsScreen
import com.example.analizer.ui.screens.ConfigScreen
import com.example.analizer.ui.screens.ImportScreen
import com.example.analizer.ui.theme.AnaliZerTheme
import com.example.analizer.ui.viewmodel.AnalyzerViewModel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

sealed class Screen {
    data object Import : Screen()
    data object Data : Screen()
    data object Config : Screen()
    data object Analytics : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: AnalyzerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("AnaliZer", "Fatal uncaught exception on thread ${thread.name}", throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
        enableEdgeToEdge()
        setContent {
            AnaliZerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainApp(viewModel: AnalyzerViewModel) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initPersistence(context)
    }

    val spreadsheetData by viewModel.spreadsheetData.collectAsStateWithLifecycle()
    val analysisConfig by viewModel.analysisConfig.collectAsStateWithLifecycle()
    val columnStats by viewModel.columnStats.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val inputUrl by viewModel.inputUrl.collectAsStateWithLifecycle()
    val targetSpreadsheetName by viewModel.targetSpreadsheetName.collectAsStateWithLifecycle()
    val importRangeText by viewModel.importRangeText.collectAsStateWithLifecycle()

    val exportConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportConfigJson(context, uri)
        }
    }

    val importConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importConfigJson(context, uri)
        }
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Import) }

    LaunchedEffect(spreadsheetData) {
        if (spreadsheetData != null && currentScreen is Screen.Import) {
            currentScreen = Screen.Data
        }
    }

    // Modal progress window shown during any import flow
    ImportProgressDialog(progressState = importProgress)

    when (currentScreen) {
        is Screen.Import -> {
            ImportScreen(
                inputUrl = inputUrl,
                onUrlChange = { viewModel.setInputUrl(it) },
                targetSpreadsheetName = targetSpreadsheetName,
                onTargetSpreadsheetNameChange = { viewModel.setTargetSpreadsheetName(it) },
                importRangeText = importRangeText,
                onImportRangeTextChange = { viewModel.setImportRangeText(it) },
                onFetchGoogleSheet = { viewModel.loadGoogleSheet(it, targetSpreadsheetName) },
                onFileSelected = { ctx, uri -> viewModel.loadUri(ctx, uri) },
                onLoadSampleData = { type -> viewModel.loadSampleData(type) },
                onLoadSampleFolderData = { viewModel.loadSampleFolderData() },
                isLoading = isLoading,
                errorMessage = errorMessage,
                onErrorDismiss = { viewModel.clearError() },
                spreadsheetData = spreadsheetData,
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
        is Screen.Data -> {
            SpreadsheetTableScreen(
                spreadsheetData = spreadsheetData,
                config = analysisConfig,
                onConfigChanged = { viewModel.updateConfig(it) },
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
        is Screen.Config -> {
            ConfigScreen(
                spreadsheetData = spreadsheetData,
                config = analysisConfig,
                columnStats = columnStats,
                onConfigChanged = { viewModel.updateConfig(it) },
                onRunAnalysis = { viewModel.runAnalysis() },
                onExportConfig = { exportConfigLauncher.launch("analizer_config.json") },
                onImportConfig = { importConfigLauncher.launch(arrayOf("application/json", "*/*")) },
                onReloadData = { viewModel.reloadData(context) },
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
        is Screen.Analytics -> {
            AnalyticsScreen(
                columnStats = columnStats,
                spreadsheetData = spreadsheetData,
                config = analysisConfig,
                onMonthSelected = { viewModel.setSelectedMonth(it) },
                onConfigChanged = { viewModel.updateConfig(it) },
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
    }
}
