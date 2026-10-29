package com.example.analizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.analizer.Screen
import com.example.analizer.domain.StatisticsEngine
import com.example.analizer.model.AnalysisConfig
import com.example.analizer.model.ColumnRangeInterval
import com.example.analizer.model.METRIC_OPTIONS
import com.example.analizer.model.SpreadsheetData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    spreadsheetData: SpreadsheetData?,
    config: AnalysisConfig,
    onConfigChanged: (AnalysisConfig) -> Unit,
    onRunAnalysis: () -> Unit,
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    if (spreadsheetData == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Configuração") },
                    navigationIcon = {
                        IconButton(onClick = { onScreenSelected(Screen.Import) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Import,
                        onClick = { onScreenSelected(Screen.Import) },
                        icon = { Icon(Icons.Rounded.FileUpload, contentDescription = "Importar") },
                        label = { Text("Importar") }
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Data,
                        onClick = { onScreenSelected(Screen.Data) },
                        icon = { Icon(Icons.Rounded.TableChart, contentDescription = "Dados") },
                        label = { Text("Dados") }
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Config,
                        onClick = { onScreenSelected(Screen.Config) },
                        icon = { Icon(Icons.Rounded.Tune, contentDescription = "Configurar") },
                        label = { Text("Configurar") }
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Analytics,
                        onClick = { onScreenSelected(Screen.Analytics) },
                        icon = { Icon(Icons.Rounded.Analytics, contentDescription = "Análise") },
                        label = { Text("Análise") }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Nenhuma planilha carregada, por favor, importe um arquivo ou link primeiro.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { onScreenSelected(Screen.Import) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar Arquivo ou Link")
                    }
                }
            }
        }
        return
    }

    val sheetNames = spreadsheetData.sheetNames
    val currentSheetName = if (config.sheetName.isNotBlank() && sheetNames.contains(config.sheetName)) {
        config.sheetName
    } else {
        sheetNames.firstOrNull() ?: ""
    }

    val currentSheetRows = spreadsheetData.sheets[currentSheetName] ?: emptyList()
    val maxRowIndex = if (currentSheetRows.isNotEmpty()) currentSheetRows.size - 1 else 0
    val headerRowIndex = config.headerRowIndex.coerceIn(0, maxOf(0, maxRowIndex))
    val headerRow = currentSheetRows.getOrNull(headerRowIndex) ?: emptyList()
    val totalCols = headerRow.size.takeIf { it > 0 } ?: (currentSheetRows.maxOfOrNull { it.size } ?: 0)

    var sheetExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuração de Intervalos de Análise", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onScreenSelected(Screen.Import) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                NavigationBarItem(
                    selected = currentScreen is Screen.Import,
                    onClick = { onScreenSelected(Screen.Import) },
                    icon = { Icon(Icons.Rounded.FileUpload, contentDescription = "Importar") },
                    label = { Text("Importar") }
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Data,
                    onClick = { onScreenSelected(Screen.Data) },
                    icon = { Icon(Icons.Rounded.TableChart, contentDescription = "Dados") },
                    label = { Text("Dados") }
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Config,
                    onClick = { onScreenSelected(Screen.Config) },
                    icon = { Icon(Icons.Rounded.Tune, contentDescription = "Configurar") },
                    label = { Text("Configurar") }
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Analytics,
                    onClick = { onScreenSelected(Screen.Analytics) },
                    icon = { Icon(Icons.Rounded.Analytics, contentDescription = "Análise") },
                    label = { Text("Análise") }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Worksheet Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Aba da Planilha",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    ExposedDropdownMenuBox(
                        expanded = sheetExpanded,
                        onExpandedChange = { sheetExpanded = !sheetExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentSheetName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sheetExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = sheetExpanded,
                            onDismissRequest = { sheetExpanded = false }
                        ) {
                            sheetNames.forEach { sheet ->
                                DropdownMenuItem(
                                    text = { Text(sheet) },
                                    onClick = {
                                        sheetExpanded = false
                                        val rows = spreadsheetData.sheets[sheet] ?: emptyList()
                                        val colCount = rows.firstOrNull()?.size ?: 0
                                        val cols = (0 until colCount).toList()
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

                                        onConfigChanged(
                                            config.copy(
                                                sheetName = sheet,
                                                selectedColumnIndices = cols,
                                                startRow = if (rows.size > 1) 1 else 0,
                                                endRow = if (rows.isNotEmpty()) rows.size - 1 else 0,
                                                intervals = defaultIntervals
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Global Row Range & Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Intervalo de Linhas e Cabeçalho",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = config.headerRowIndex.toString(),
                            onValueChange = { input ->
                                val parsed = input.toIntOrNull() ?: 0
                                onConfigChanged(config.copy(headerRowIndex = parsed.coerceIn(0, maxRowIndex)))
                            },
                            label = { Text("Linha do Cabeçalho") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = config.startRow.toString(),
                            onValueChange = { input ->
                                val parsed = input.toIntOrNull() ?: 0
                                onConfigChanged(config.copy(startRow = parsed.coerceIn(0, maxRowIndex)))
                            },
                            label = { Text("Linha Inicial Global") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = config.endRow.toString(),
                            onValueChange = { input ->
                                val parsed = input.toIntOrNull() ?: maxRowIndex
                                onConfigChanged(config.copy(endRow = parsed.coerceIn(0, maxRowIndex)))
                            },
                            label = { Text("Linha Final Global") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                    Text(
                        text = "Total de linhas na planilha: ${currentSheetRows.size} (Índices 0 a $maxRowIndex)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Column Range Intervals Container Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Intervalos de Análise (Colunas e Linhas)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Defina os intervalos de colunas (início e fim), personalização de linhas e a métrica para cada intervalo de análise.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (config.intervals.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhum intervalo configurado. Clique abaixo para adicionar.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        config.intervals.forEachIndexed { index, interval ->
                            IntervalItemCard(
                                interval = interval,
                                totalCols = totalCols,
                                headerRow = headerRow,
                                customColumnTitles = config.customColumnTitles,
                                maxRowIndex = maxRowIndex,
                                globalStartRow = config.startRow,
                                globalEndRow = config.endRow,
                                onUpdateInterval = { updatedInterval ->
                                    val updatedList = config.intervals.toMutableList().apply {
                                        set(index, updatedInterval)
                                    }
                                    onConfigChanged(config.copy(intervals = updatedList))
                                },
                                onDeleteInterval = {
                                    val updatedList = config.intervals.toMutableList().apply {
                                        removeAt(index)
                                    }
                                    onConfigChanged(config.copy(intervals = updatedList))
                                }
                            )
                        }
                    }

                    // Add Interval Button
                    OutlinedButton(
                        onClick = {
                            val newInterval = ColumnRangeInterval(
                                startColumn = 0,
                                endColumn = (totalCols - 1).coerceAtLeast(0),
                                startRow = null,
                                endRow = null,
                                metric = "Média"
                            )
                            onConfigChanged(config.copy(intervals = config.intervals + newInterval))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar Intervalo")
                    }
                }
            }

            // Run Analysis Button
            Button(
                onClick = {
                    onRunAnalysis()
                    onScreenSelected(Screen.Analytics)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Rounded.Analytics, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Executar Análise e Ver Resultados", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalItemCard(
    interval: ColumnRangeInterval,
    totalCols: Int,
    headerRow: List<String>,
    customColumnTitles: Map<Int, String>,
    maxRowIndex: Int,
    globalStartRow: Int,
    globalEndRow: Int,
    onUpdateInterval: (ColumnRangeInterval) -> Unit,
    onDeleteInterval: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with icon and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ViewWeek,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (interval.name.isNotBlank()) interval.name else "Intervalo de Colunas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDeleteInterval) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Remover Intervalo",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Interval Name Field (Optional)
            OutlinedTextField(
                value = interval.name,
                onValueChange = { newName ->
                    onUpdateInterval(interval.copy(name = newName))
                },
                label = { Text("Nome do Intervalo (Opcional)") },
                placeholder = { Text("ex: Vendas Trimestrais") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Column Start & Column End Dropdowns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ColumnDropdown(
                    label = "Coluna Início",
                    selectedColumn = interval.startColumn,
                    totalCols = totalCols,
                    headerRow = headerRow,
                    customColumnTitles = customColumnTitles,
                    onColumnSelected = { selected ->
                        val newEnd = if (selected > interval.endColumn) selected else interval.endColumn
                        onUpdateInterval(interval.copy(startColumn = selected, endColumn = newEnd))
                    },
                    modifier = Modifier.weight(1f)
                )

                ColumnDropdown(
                    label = "Coluna Fim",
                    selectedColumn = interval.endColumn,
                    totalCols = totalCols,
                    headerRow = headerRow,
                    customColumnTitles = customColumnTitles,
                    onColumnSelected = { selected ->
                        val newStart = if (selected < interval.startColumn) selected else interval.startColumn
                        onUpdateInterval(interval.copy(startColumn = newStart, endColumn = selected))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Specific Start Row & End Row Overrides
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = interval.startRow?.toString() ?: "",
                    onValueChange = { text ->
                        val parsed = text.toIntOrNull()
                        onUpdateInterval(interval.copy(startRow = parsed?.coerceIn(0, maxRowIndex)))
                    },
                    label = { Text("Linha Início (Op.)") },
                    placeholder = { Text("Global ($globalStartRow)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = interval.endRow?.toString() ?: "",
                    onValueChange = { text ->
                        val parsed = text.toIntOrNull()
                        onUpdateInterval(interval.copy(endRow = parsed?.coerceIn(0, maxRowIndex)))
                    },
                    label = { Text("Linha Fim (Op.)") },
                    placeholder = { Text("Global ($globalEndRow)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Metric Selection
            ColumnMetricDropdown(
                currentMetric = interval.metric,
                onMetricSelected = { newMetric ->
                    onUpdateInterval(interval.copy(metric = newMetric))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnDropdown(
    label: String,
    selectedColumn: Int,
    totalCols: Int,
    headerRow: List<String>,
    customColumnTitles: Map<Int, String>,
    onColumnSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    fun getColumnLabel(index: Int): String {
        val excelName = StatisticsEngine().getExcelColumnName(index)
        val custom = customColumnTitles[index]
        val header = headerRow.getOrNull(index)?.trim()
        val title = if (!custom.isNullOrBlank()) custom else if (!header.isNullOrBlank()) header else null
        return if (title != null) "$excelName - $title" else "Coluna $excelName"
    }

    val safeIndex = selectedColumn.coerceIn(0, maxOf(0, totalCols - 1))
    val selectedText = if (totalCols > 0) getColumnLabel(safeIndex) else "Sem colunas"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            shape = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            for (colIdx in 0 until totalCols) {
                DropdownMenuItem(
                    text = { Text(getColumnLabel(colIdx)) },
                    onClick = {
                        expanded = false
                        onColumnSelected(colIdx)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnMetricDropdown(
    currentMetric: String,
    onMetricSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = currentMetric,
            onValueChange = {},
            readOnly = true,
            label = { Text("Métrica") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            shape = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            METRIC_OPTIONS.forEach { metric ->
                DropdownMenuItem(
                    text = { Text(metric) },
                    onClick = {
                        expanded = false
                        onMetricSelected(metric)
                    }
                )
            }
        }
    }
}
