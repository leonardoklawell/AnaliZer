package com.example.analizer.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.analizer.data.AppPersistenceManager
import com.example.analizer.data.SpreadsheetRepository
import com.example.analizer.domain.StatisticsEngine
import com.example.analizer.model.AnalysisConfig
import com.example.analizer.model.ColumnStats
import com.example.analizer.model.SpreadsheetData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ImportProgressState(
    val isImporting: Boolean = false,
    val title: String = "Importando Planilha",
    val stageMessage: String = "",
    val progress: Float = 0f,
    val stepText: String = ""
)

sealed class ImportSource {
    data class GoogleSheet(val url: String, val targetName: String) : ImportSource()
    data class LocalUri(val uriString: String) : ImportSource()
    data class SampleData(val type: String) : ImportSource()
    data object SampleFolders : ImportSource()
}

class AnalyzerViewModel(
    private val repository: SpreadsheetRepository = SpreadsheetRepository(),
    private val statisticsEngine: StatisticsEngine = StatisticsEngine()
) : ViewModel() {

    private var persistenceManager: AppPersistenceManager? = null
    private var lastImportSource: ImportSource? = null

    private val _spreadsheetData = MutableStateFlow<SpreadsheetData?>(null)
    val spreadsheetData: StateFlow<SpreadsheetData?> = _spreadsheetData.asStateFlow()

    private val _analysisConfig = MutableStateFlow(AnalysisConfig())
    val analysisConfig: StateFlow<AnalysisConfig> = _analysisConfig.asStateFlow()

    private val _columnStats = MutableStateFlow<List<ColumnStats>>(emptyList())
    val columnStats: StateFlow<List<ColumnStats>> = _columnStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _importProgress = MutableStateFlow(ImportProgressState())
    val importProgress: StateFlow<ImportProgressState> = _importProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _inputUrl = MutableStateFlow("")
    val inputUrl: StateFlow<String> = _inputUrl.asStateFlow()

    private val _targetSpreadsheetName = MutableStateFlow("")
    val targetSpreadsheetName: StateFlow<String> = _targetSpreadsheetName.asStateFlow()

    fun initPersistence(context: Context) {
        if (persistenceManager == null) {
            val manager = AppPersistenceManager(context.applicationContext)
            persistenceManager = manager
            viewModelScope.launch {
                val (savedData, savedConfig) = manager.loadSavedData()
                if (savedData != null) {
                    _spreadsheetData.value = savedData
                    val configToUse = savedConfig ?: AnalysisConfig(
                        sheetName = savedData.sheetNames.firstOrNull() ?: ""
                    )
                    _analysisConfig.value = configToUse
                    runAnalysisForData(savedData, configToUse)
                }
            }
        }
    }

    private fun saveCurrentDataAndConfig() {
        val manager = persistenceManager ?: return
        val data = _spreadsheetData.value ?: return
        val config = _analysisConfig.value
        viewModelScope.launch {
            manager.saveSpreadsheetData(data)
            manager.saveAnalysisConfig(config)
        }
    }

    fun setInputUrl(url: String) {
        _inputUrl.value = url
    }

    fun setTargetSpreadsheetName(name: String) {
        _targetSpreadsheetName.value = name
    }

    fun loadGoogleSheet(url: String, targetSpreadsheetName: String = _targetSpreadsheetName.value) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) {
            _errorMessage.value = "Por favor, insira um link válido do Google Sheets ou Pasta do Google Drive."
            return
        }
        lastImportSource = ImportSource.GoogleSheet(trimmedUrl, targetSpreadsheetName)
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _importProgress.value = ImportProgressState(
                isImporting = true,
                title = "Importando Google Sheets / Drive",
                stageMessage = "Estabelecendo conexão e acessando dados...",
                progress = 0.20f,
                stepText = "Etapa 1 de 4"
            )
            try {
                val result = repository.fetchGoogleSheet(trimmedUrl, targetSpreadsheetName)
                _isLoading.value = false
                result.onSuccess { data ->
                    try {
                        _importProgress.value = ImportProgressState(
                            isImporting = true,
                            title = "Importando Google Sheets / Drive",
                            stageMessage = "Lendo pastas, tabelas e abas da planilha...",
                            progress = 0.50f,
                            stepText = "Etapa 2 de 4"
                        )
                        _spreadsheetData.value = data
                        val defaultSheet = data.sheetNames.firstOrNull() ?: ""
                        val firstSheetRows = data.sheets[defaultSheet] ?: emptyList()
                        val colCount = firstSheetRows.firstOrNull()?.size ?: 0
                        val allCols = (0 until colCount).toList()

                        // DO NOT auto-create groups, intervals or items!
                        val newConfig = AnalysisConfig(
                            sheetName = defaultSheet,
                            headerRowIndex = 0,
                            selectedColumnIndices = allCols,
                            startRow = null,
                            endRow = null,
                            intervals = emptyList(),
                            displayGroups = emptyList()
                        )
                        _analysisConfig.value = newConfig
                        runAnalysisForData(data, newConfig)

                        _importProgress.value = ImportProgressState(
                            isImporting = true,
                            title = "Salvando Localmente",
                            stageMessage = "Armazenando dados e configurações para próximos acessos...",
                            progress = 0.85f,
                            stepText = "Etapa 3 de 4"
                        )
                        saveCurrentDataAndConfig()

                        _importProgress.value = ImportProgressState(
                            isImporting = true,
                            title = "Importação Concluída!",
                            stageMessage = "Tudo pronto! Seus dados foram carregados com sucesso.",
                            progress = 1.0f,
                            stepText = "Etapa 4 de 4"
                        )
                        delay(400)
                    } catch (t: Throwable) {
                        _errorMessage.value = "Erro ao processar dados da planilha: ${t.localizedMessage ?: t.message}"
                    } finally {
                        _importProgress.value = ImportProgressState(isImporting = false)
                    }
                }.onFailure { e ->
                    _errorMessage.value = "Falha ao carregar Google Sheets: ${e.localizedMessage ?: e.message}"
                    _importProgress.value = ImportProgressState(isImporting = false)
                }
            } catch (t: Throwable) {
                _isLoading.value = false
                _importProgress.value = ImportProgressState(isImporting = false)
                _errorMessage.value = "Erro na requisição: ${t.localizedMessage ?: t.message}"
            }
        }
    }

    fun loadSampleFolderData() {
        lastImportSource = ImportSource.SampleFolders
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _importProgress.value = ImportProgressState(
                isImporting = true,
                title = "Importando Dados de Exemplo",
                stageMessage = "Gerando estrutura de pastas e planilhas de exemplo...",
                progress = 0.30f,
                stepText = "Etapa 1 de 3"
            )
            try {
                val sampleData = repository.createSampleMultiFolderData()
                _spreadsheetData.value = sampleData
                val firstFolder = sampleData.folders.firstOrNull()
                val defaultSheet = firstFolder?.sheetNames?.firstOrNull() ?: sampleData.sheetNames.firstOrNull() ?: ""
                val rows = firstFolder?.sheets?.get(defaultSheet) ?: sampleData.sheets[defaultSheet] ?: emptyList()
                val colCount = rows.firstOrNull()?.size ?: 0

                // DO NOT auto-create groups, intervals or items!
                val config = AnalysisConfig(
                    sheetName = defaultSheet,
                    headerRowIndex = 0,
                    selectedColumnIndices = (0 until colCount).toList(),
                    startRow = null,
                    endRow = null,
                    displayGroups = emptyList()
                )
                _analysisConfig.value = config
                runAnalysisForData(sampleData, config)

                _importProgress.value = ImportProgressState(
                    isImporting = true,
                    title = "Salvando Localmente",
                    stageMessage = "Salvando dados localmente...",
                    progress = 0.80f,
                    stepText = "Etapa 2 de 3"
                )
                saveCurrentDataAndConfig()

                _importProgress.value = ImportProgressState(
                    isImporting = true,
                    title = "Importação Concluída!",
                    stageMessage = "Exemplo carregado com sucesso!",
                    progress = 1.0f,
                    stepText = "Etapa 3 de 3"
                )
                delay(300)
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar exemplo de pastas: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _importProgress.value = ImportProgressState(isImporting = false)
            }
        }
    }

    fun loadUri(context: Context, uri: Uri) {
        lastImportSource = ImportSource.LocalUri(uri.toString())
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _importProgress.value = ImportProgressState(
                isImporting = true,
                title = "Importando Arquivo Local",
                stageMessage = "Lendo e analisando o arquivo selecionado...",
                progress = 0.25f,
                stepText = "Etapa 1 de 3"
            )
            try {
                val result = repository.loadSpreadsheetFromUri(context, uri)
                _isLoading.value = false
                result.onSuccess { data ->
                    try {
                        _spreadsheetData.value = data
                        val defaultSheet = data.sheetNames.firstOrNull() ?: ""
                        val firstSheetRows = data.sheets[defaultSheet] ?: emptyList()
                        val colCount = firstSheetRows.firstOrNull()?.size ?: 0
                        val allCols = (0 until colCount).toList()

                        // DO NOT auto-create groups, intervals or items!
                        val newConfig = AnalysisConfig(
                            sheetName = defaultSheet,
                            headerRowIndex = 0,
                            selectedColumnIndices = allCols,
                            startRow = null,
                            endRow = null,
                            intervals = emptyList(),
                            displayGroups = emptyList()
                        )
                        _analysisConfig.value = newConfig
                        runAnalysisForData(data, newConfig)

                        _importProgress.value = ImportProgressState(
                            isImporting = true,
                            title = "Salvando no Dispositivo",
                            stageMessage = "Gravando arquivo e estruturas de dados...",
                            progress = 0.85f,
                            stepText = "Etapa 2 de 3"
                        )
                        saveCurrentDataAndConfig()

                        _importProgress.value = ImportProgressState(
                            isImporting = true,
                            title = "Importação Concluída!",
                            stageMessage = "Arquivo importado e salvo com sucesso!",
                            progress = 1.0f,
                            stepText = "Etapa 3 de 3"
                        )
                        delay(300)
                    } catch (t: Throwable) {
                        _errorMessage.value = "Erro ao processar arquivo: ${t.localizedMessage ?: t.message}"
                    } finally {
                        _importProgress.value = ImportProgressState(isImporting = false)
                    }
                }.onFailure { e ->
                    _errorMessage.value = "Falha ao ler arquivo: ${e.localizedMessage ?: e.message}"
                    _importProgress.value = ImportProgressState(isImporting = false)
                }
            } catch (t: Throwable) {
                _isLoading.value = false
                _importProgress.value = ImportProgressState(isImporting = false)
                _errorMessage.value = "Erro ao abrir arquivo: ${t.localizedMessage ?: t.message}"
            }
        }
    }

    fun loadSampleData(type: String = "sales") {
        lastImportSource = ImportSource.SampleData(type)
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _importProgress.value = ImportProgressState(
                isImporting = true,
                title = "Carregando Dados de Exemplo",
                stageMessage = "Gerando dados do modelo...",
                progress = 0.40f,
                stepText = "Etapa 1 de 2"
            )
            try {
                val sampleRows = if (type == "sales") {
                    listOf(
                        listOf("Product", "Price", "Quantity", "Revenue", "Rating"),
                        listOf("Laptop Pro", "1200.50", "15", "18007.50", "4.8"),
                        listOf("Wireless Mouse", "25.00", "150", "3750.00", "4.5"),
                        listOf("Mechanical Keyboard", "75.50", "80", "6040.00", "4.7"),
                        listOf("4K Monitor", "300.00", "25", "7500.00", "4.6"),
                        listOf("Noise-Cancelling Headset", "50.00", "60", "3000.00", "4.2"),
                        listOf("Ergonomic Standing Desk", "250.00", "10", "2500.00", "4.4"),
                        listOf("USB-C Hub", "35.00", "120", "4200.00", "4.3"),
                        listOf("Webcam HD", "60.00", "45", "2700.00", "4.1")
                    )
                } else {
                    listOf(
                        listOf("Month", "Users", "Active", "Revenue ($)", "Churn (%)"),
                        listOf("Jan", "1200", "950", "12500", "2.1"),
                        listOf("Feb", "1450", "1100", "15200", "1.9"),
                        listOf("Mar", "1800", "1350", "19000", "1.8"),
                        listOf("Apr", "2100", "1600", "22500", "1.5"),
                        listOf("May", "2500", "1950", "27000", "1.4"),
                        listOf("Jun", "3000", "2300", "32000", "1.2")
                    )
                }

                val sheetName = if (type == "sales") "SalesData" else "MetricsData"
                val spreadsheetData = SpreadsheetData(
                    sheetNames = listOf(sheetName),
                    sheets = mapOf(sheetName to sampleRows)
                )

                _spreadsheetData.value = spreadsheetData
                val colCount = sampleRows.firstOrNull()?.size ?: 0
                val allCols = (0 until colCount).toList()

                // DO NOT auto-create groups, intervals or items!
                val config = AnalysisConfig(
                    sheetName = sheetName,
                    headerRowIndex = 0,
                    selectedColumnIndices = allCols,
                    startRow = null,
                    endRow = null,
                    intervals = emptyList(),
                    displayGroups = emptyList()
                )
                _analysisConfig.value = config
                runAnalysisForData(spreadsheetData, config)

                _importProgress.value = ImportProgressState(
                    isImporting = true,
                    title = "Salvando Localmente",
                    stageMessage = "Dados de exemplo salvos com sucesso!",
                    progress = 1.0f,
                    stepText = "Etapa 2 de 2"
                )
                saveCurrentDataAndConfig()
                delay(300)
            } catch (e: Exception) {
                _errorMessage.value = "Error loading sample data: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _importProgress.value = ImportProgressState(isImporting = false)
            }
        }
    }

    fun reloadData(context: Context) {
        val currentConfig = _analysisConfig.value
        val source = lastImportSource
        if (source == null) {
            _spreadsheetData.value?.let { data ->
                runAnalysisForData(data, currentConfig)
            }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _importProgress.value = ImportProgressState(
                isImporting = true,
                title = "Recarregando Dados",
                stageMessage = "Atualizando planilha a partir da fonte...",
                progress = 0.30f,
                stepText = "Etapa 1 de 2"
            )
            try {
                when (source) {
                    is ImportSource.GoogleSheet -> {
                        val res = repository.fetchGoogleSheet(source.url, source.targetName)
                        res.onSuccess { data ->
                            _spreadsheetData.value = data
                            runAnalysisForData(data, currentConfig)
                            saveCurrentDataAndConfig()
                        }.onFailure { e ->
                            _errorMessage.value = "Erro ao recarregar Google Sheets: ${e.localizedMessage}"
                        }
                    }
                    is ImportSource.LocalUri -> {
                        val uri = Uri.parse(source.uriString)
                        val res = repository.loadSpreadsheetFromUri(context, uri)
                        res.onSuccess { data ->
                            _spreadsheetData.value = data
                            runAnalysisForData(data, currentConfig)
                            saveCurrentDataAndConfig()
                        }.onFailure { e ->
                            _errorMessage.value = "Erro ao recarregar arquivo local: ${e.localizedMessage}"
                        }
                    }
                    is ImportSource.SampleFolders -> {
                        val sampleData = repository.createSampleMultiFolderData()
                        _spreadsheetData.value = sampleData
                        runAnalysisForData(sampleData, currentConfig)
                        saveCurrentDataAndConfig()
                    }
                    is ImportSource.SampleData -> {
                        loadSampleData(source.type)
                    }
                }
                _importProgress.value = ImportProgressState(
                    isImporting = true,
                    title = "Recarregamento Concluído!",
                    stageMessage = "Dados atualizados com sucesso mantendo suas configurações!",
                    progress = 1.0f,
                    stepText = "Etapa 2 de 2"
                )
                delay(300)
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao recarregar dados: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _importProgress.value = ImportProgressState(isImporting = false)
            }
        }
    }

    fun exportConfigJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val manager = persistenceManager ?: AppPersistenceManager(context)
                manager.exportConfigToUri(context, uri, _analysisConfig.value)
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao exportar configurações: ${e.localizedMessage}"
            }
        }
    }

    fun importConfigJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val manager = persistenceManager ?: AppPersistenceManager(context)
                val importedConfig = manager.importConfigFromUri(context, uri)
                if (importedConfig != null) {
                    _analysisConfig.value = importedConfig
                    _spreadsheetData.value?.let { data ->
                        runAnalysisForData(data, importedConfig)
                    }
                    manager.saveAnalysisConfig(importedConfig)
                } else {
                    _errorMessage.value = "Arquivo de configuração inválido."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao importar configurações: ${e.localizedMessage}"
            }
        }
    }

    fun updateConfig(config: AnalysisConfig) {
        _analysisConfig.value = config
        _spreadsheetData.value?.let { data ->
            runAnalysisForData(data, config)
        }
        persistenceManager?.let { manager ->
            viewModelScope.launch {
                manager.saveAnalysisConfig(config)
            }
        }
    }

    fun runAnalysis() {
        _spreadsheetData.value?.let { data ->
            runAnalysisForData(data, _analysisConfig.value)
        }
        saveCurrentDataAndConfig()
    }

    private fun runAnalysisForData(data: SpreadsheetData, config: AnalysisConfig) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val stats = statisticsEngine.calculateStats(data, config)
                _columnStats.value = stats
            } catch (e: Exception) {
                _errorMessage.value = "Analysis error: ${e.localizedMessage}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
