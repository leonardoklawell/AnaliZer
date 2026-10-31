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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.analizer.ui.SpreadsheetTableScreen
import com.example.analizer.ui.screens.AnalyticsScreen
import com.example.analizer.ui.screens.ConfigScreen
import com.example.analizer.ui.screens.ImportScreen
import com.example.analizer.ui.theme.AnaliZerTheme
import com.example.analizer.ui.viewmodel.AnalyzerViewModel

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
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.initSession(context)
    }

    val spreadsheetData by viewModel.spreadsheetData.collectAsStateWithLifecycle()
    val analysisConfig by viewModel.analysisConfig.collectAsStateWithLifecycle()
    val columnStats by viewModel.columnStats.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val inputUrl by viewModel.inputUrl.collectAsStateWithLifecycle()
    val targetSpreadsheetName by viewModel.targetSpreadsheetName.collectAsStateWithLifecycle()
    val userSession by viewModel.userSession.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Import) }

    LaunchedEffect(spreadsheetData) {
        if (spreadsheetData != null && currentScreen is Screen.Import) {
            currentScreen = Screen.Data
        }
    }

    if (showSettingsDialog) {
        com.example.analizer.ui.screens.SettingsDialog(
            userSession = userSession,
            onLogin = { email, name -> viewModel.login(context, email, name) },
            onLogout = { viewModel.logout(context) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    when (currentScreen) {
        is Screen.Import -> {
            ImportScreen(
                inputUrl = inputUrl,
                onUrlChange = { viewModel.setInputUrl(it) },
                targetSpreadsheetName = targetSpreadsheetName,
                onTargetSpreadsheetNameChange = { viewModel.setTargetSpreadsheetName(it) },
                onFetchGoogleSheet = { viewModel.loadGoogleSheet(it, targetSpreadsheetName) },
                onFileSelected = { ctx, uri -> viewModel.loadUri(ctx, uri) },
                onLoadSampleData = { type -> viewModel.loadSampleData(type) },
                onLoadSampleFolderData = { viewModel.loadSampleFolderData() },
                onOpenSettings = { showSettingsDialog = true },
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
                onOpenSettings = { showSettingsDialog = true },
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
                onOpenSettings = { showSettingsDialog = true },
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
        is Screen.Analytics -> {
            AnalyticsScreen(
                columnStats = columnStats,
                spreadsheetData = spreadsheetData,
                config = analysisConfig,
                onOpenSettings = { showSettingsDialog = true },
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
    }
}
