package com.example.analizer.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.analizer.data.SpreadsheetRepository
import com.example.analizer.domain.StatisticsEngine
import com.example.analizer.model.AnalysisConfig
import com.example.analizer.model.ColumnRangeInterval
import com.example.analizer.model.ColumnStats
import com.example.analizer.model.DisplayGroup
import com.example.analizer.model.DisplayGroupItem
import com.example.analizer.model.SpreadsheetData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalyzerViewModel(
    private val repository: SpreadsheetRepository = SpreadsheetRepository(),
    private val statisticsEngine: StatisticsEngine = StatisticsEngine()
) : ViewModel() {

    private val _spreadsheetData = MutableStateFlow<SpreadsheetData?>(null)
    val spreadsheetData: StateFlow<SpreadsheetData?> = _spreadsheetData.asStateFlow()

    private val _analysisConfig = MutableStateFlow(AnalysisConfig())
    val analysisConfig: StateFlow<AnalysisConfig> = _analysisConfig.asStateFlow()

    private val _columnStats = MutableStateFlow<List<ColumnStats>>(emptyList())
    val columnStats: StateFlow<List<ColumnStats>> = _columnStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _inputUrl = MutableStateFlow("")
    val inputUrl: StateFlow<String> = _inputUrl.asStateFlow()

    private val _targetSpreadsheetName = MutableStateFlow("")
    val targetSpreadsheetName: StateFlow<String> = _targetSpreadsheetName.asStateFlow()

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
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.fetchGoogleSheet(trimmedUrl, targetSpreadsheetName)
                _isLoading.value = false
                result.onSuccess { data ->
                    try {
                        _spreadsheetData.value = data
                        val defaultSheet = data.sheetNames.firstOrNull() ?: ""
                        val firstSheetRows = data.sheets[defaultSheet] ?: emptyList()
                        val colCount = firstSheetRows.firstOrNull()?.size ?: 0
                        val allCols = (0 until colCount).toList()
                        val defaultEndRow = if (firstSheetRows.isNotEmpty()) firstSheetRows.size - 1 else 0

                        val defaultIntervals = if (colCount > 0) {
                            listOf(
                                ColumnRangeInterval(
                                    startColumn = 0,
                                    endColumn = colCount - 1,
                                    startRow = null,
                                    endRow = null,
                                    metric = "Média"
                                )
                            )
                        } else emptyList()

                        val defaultDisplayGroups = if (data.folders.isNotEmpty()) {
                            data.folders.mapIndexed { idx, folder ->
                                val fSheet = folder.sheetNames.firstOrNull() ?: ""
                                val fRows = folder.sheets[fSheet] ?: emptyList()
                                val cCount = fRows.firstOrNull()?.size ?: 0
                                DisplayGroup(
                                    name = folder.folderName,
                                    selectedFolder = folder.folderName,
                                    selectedSheets = if (fSheet.isNotBlank()) listOf(fSheet) else emptyList(),
                                    items = listOf(
                                        DisplayGroupItem(
                                            startColumn = 0,
                                            endColumn = (cCount - 1).coerceAtLeast(0),
                                            startRow = null,
                                            endRow = null,
                                            metric = "Média"
                                        )
                                    ),
                                    displayOrder = idx
                                )
                            }
                        } else if (colCount > 0) {
                            listOf(
                                DisplayGroup(
                                    name = "Grupo 1",
                                    selectedSheets = if (defaultSheet.isNotBlank()) listOf(defaultSheet) else emptyList(),
                                    items = listOf(
                                        DisplayGroupItem(
                                            startColumn = 0,
                                            endColumn = colCount - 1,
                                            startRow = null,
                                            endRow = null,
                                            metric = "Média"
                                        )
                                    )
                                )
                            )
                        } else emptyList()

                        val newConfig = AnalysisConfig(
                            sheetName = defaultSheet,
                            headerRowIndex = 0,
                            selectedColumnIndices = allCols,
                            startRow = if (firstSheetRows.size > 1) 1 else 0,
                            endRow = defaultEndRow,
                            intervals = defaultIntervals,
                            displayGroups = defaultDisplayGroups
                        )
                        _analysisConfig.value = newConfig
                        runAnalysisForData(data, newConfig)
                    } catch (t: Throwable) {
                        _errorMessage.value = "Erro ao processar dados da planilha: ${t.localizedMessage ?: t.message}"
                    }
                }.onFailure { e ->
                    _errorMessage.value = "Falha ao carregar Google Sheets: ${e.localizedMessage ?: e.message}"
                }
            } catch (t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = "Erro na requisição: ${t.localizedMessage ?: t.message}"
            }
        }
    }

    fun loadSampleFolderData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val sampleData = repository.createSampleMultiFolderData()
                _spreadsheetData.value = sampleData
                val firstFolder = sampleData.folders.firstOrNull()
                val defaultSheet = firstFolder?.sheetNames?.firstOrNull() ?: sampleData.sheetNames.firstOrNull() ?: ""
                val rows = firstFolder?.sheets?.get(defaultSheet) ?: sampleData.sheets[defaultSheet] ?: emptyList()
                val colCount = rows.firstOrNull()?.size ?: 0

                val defaultDisplayGroups = sampleData.folders.mapIndexed { idx, folder ->
                    val fSheet = folder.sheetNames.firstOrNull() ?: ""
                    val fRows = folder.sheets[fSheet] ?: emptyList()
                    val cCount = fRows.firstOrNull()?.size ?: 0
                    DisplayGroup(
                        name = folder.folderName,
                        selectedFolder = folder.folderName,
                        selectedSheets = if (fSheet.isNotBlank()) listOf(fSheet) else emptyList(),
                        items = listOf(
                            DisplayGroupItem(
                                startColumn = 0,
                                endColumn = (cCount - 1).coerceAtLeast(0),
                                startRow = 1,
                                endRow = fRows.size - 1,
                                metric = "Média"
                            )
                        ),
                        displayOrder = idx
                    )
                }

                val config = AnalysisConfig(
                    sheetName = defaultSheet,
                    headerRowIndex = 0,
                    selectedColumnIndices = (0 until colCount).toList(),
                    startRow = 1,
                    endRow = if (rows.size > 1) rows.size - 1 else 0,
                    displayGroups = defaultDisplayGroups
                )
                _analysisConfig.value = config
                runAnalysisForData(sampleData, config)
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar exemplo de pastas: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
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
                        val defaultEndRow = if (firstSheetRows.isNotEmpty()) firstSheetRows.size - 1 else 0

                        val defaultIntervals = if (colCount > 0) {
                            listOf(
                                ColumnRangeInterval(
                                    startColumn = 0,
                                    endColumn = colCount - 1,
                                    startRow = null,
                                    endRow = null,
                                    metric = "Média"
                                )
                            )
                        } else emptyList()

                        val defaultDisplayGroups = if (colCount > 0) {
                            listOf(
                                DisplayGroup(
                                    name = "Grupo 1",
                                    selectedSheets = if (defaultSheet.isNotBlank()) listOf(defaultSheet) else emptyList(),
                                    items = listOf(
                                        DisplayGroupItem(
                                            startColumn = 0,
                                            endColumn = colCount - 1,
                                            startRow = null,
                                            endRow = null,
                                            metric = "Média"
                                        )
                                    )
                                )
                            )
                        } else emptyList()

                        val newConfig = AnalysisConfig(
                            sheetName = defaultSheet,
                            headerRowIndex = 0,
                            selectedColumnIndices = allCols,
                            startRow = if (firstSheetRows.size > 1) 1 else 0,
                            endRow = defaultEndRow,
                            intervals = defaultIntervals,
                            displayGroups = defaultDisplayGroups
                        )
                        _analysisConfig.value = newConfig
                        runAnalysisForData(data, newConfig)
                    } catch (t: Throwable) {
                        _errorMessage.value = "Erro ao processar arquivo: ${t.localizedMessage ?: t.message}"
                    }
                }.onFailure { e ->
                    _errorMessage.value = "Falha ao ler arquivo: ${e.localizedMessage ?: e.message}"
                }
            } catch (t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = "Erro ao abrir arquivo: ${t.localizedMessage ?: t.message}"
            }
        }
    }

    fun loadSampleData(type: String = "sales") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
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
                val defaultIntervals = if (colCount > 0) {
                    listOf(
                        ColumnRangeInterval(
                            startColumn = 0,
                            endColumn = colCount - 1,
                            startRow = null,
                            endRow = null,
                            metric = "Média"
                        )
                    )
                } else emptyList()

                val defaultDisplayGroups = if (colCount > 0) {
                    listOf(
                        DisplayGroup(
                            name = "Grupo 1",
                            selectedSheets = listOf(sheetName),
                            items = listOf(
                                DisplayGroupItem(
                                    startColumn = 0,
                                    endColumn = colCount - 1,
                                    startRow = null,
                                    endRow = null,
                                    metric = "Média"
                                )
                            )
                        )
                    )
                } else emptyList()

                val config = AnalysisConfig(
                    sheetName = sheetName,
                    headerRowIndex = 0,
                    selectedColumnIndices = allCols,
                    startRow = 1,
                    endRow = sampleRows.size - 1,
                    intervals = defaultIntervals,
                    displayGroups = defaultDisplayGroups
                )
                _analysisConfig.value = config
                runAnalysisForData(spreadsheetData, config)
            } catch (e: Exception) {
                _errorMessage.value = "Error loading sample data: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateConfig(config: AnalysisConfig) {
        _analysisConfig.value = config
        _spreadsheetData.value?.let { data ->
            runAnalysisForData(data, config)
        }
    }

    fun runAnalysis() {
        _spreadsheetData.value?.let { data ->
            runAnalysisForData(data, _analysisConfig.value)
        }
    }

    private fun runAnalysisForData(data: SpreadsheetData, config: AnalysisConfig) {
        try {
            val stats = statisticsEngine.calculateStats(data, config)
            _columnStats.value = stats
        } catch (e: Exception) {
            _errorMessage.value = "Analysis error: ${e.localizedMessage}"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
