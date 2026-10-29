package com.example.analizer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.analizer.Screen
import com.example.analizer.model.AnalysisConfig
import com.example.analizer.model.ColumnStats
import com.example.analizer.model.SpreadsheetData
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    columnStats: List<ColumnStats>,
    spreadsheetData: SpreadsheetData?,
    config: AnalysisConfig = AnalysisConfig(),
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Estatísticas", "Gráfico de Barras", "Gráfico de Linhas", "Distribuição", "Valores Brutos")

    var activeTooltipIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análise e Gráficos Interativos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onScreenSelected(Screen.Config) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar para Configuração")
                    }
                },
                actions = {
                    IconButton(onClick = { onScreenSelected(Screen.Import) }) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = "Importar Planilha")
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
        if (spreadsheetData == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { onScreenSelected(Screen.Import) },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Carregar Arquivo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Tabs for Analytics views
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                activeTooltipIndex = null
                            },
                            text = { Text(title, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                if (columnStats.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Analytics,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Nenhuma análise executada.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Configure as colunas e clique em 'Executar Análise' ou verifique os dados.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(onClick = { onScreenSelected(Screen.Config) }) {
                                    Text("Configurar")
                                }
                                Button(onClick = { onScreenSelected(Screen.Data) }) {
                                    Text("Ver Dados")
                                }
                            }
                        }
                    }
                } else {
                    when (selectedTab) {
                        0 -> StatisticsTab(columnStats = columnStats, config = config)
                        1 -> BarChartTab(columnStats, activeTooltipIndex, onTooltipChanged = { activeTooltipIndex = it })
                        2 -> LineChartTab(columnStats, activeTooltipIndex, onTooltipChanged = { activeTooltipIndex = it })
                        3 -> DistributionTab(columnStats)
                        4 -> RawValuesTab(columnStats)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatisticsTab(
    columnStats: List<ColumnStats>,
    config: AnalysisConfig
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Cards per Column
        columnStats.forEachIndexed { i, stat ->
            val colIndex = stat.columnIndex.takeIf { it >= 0 } ?: config.selectedColumnIndices.getOrNull(i) ?: i
            val selectedMetric = config.getMetricForColumn(colIndex)

            val highlightValue = when (selectedMetric) {
                "Soma" -> String.format(Locale.getDefault(), "%.2f", stat.sum)
                "Máximo" -> String.format(Locale.getDefault(), "%.2f", stat.max)
                "Mínimo" -> String.format(Locale.getDefault(), "%.2f", stat.min)
                "Mediana" -> String.format(Locale.getDefault(), "%.2f", stat.median)
                "Desvio Padrão" -> String.format(Locale.getDefault(), "%.2f", stat.stdDev)
                "Contagem" -> "${stat.count}"
                else -> String.format(Locale.getDefault(), "%.2f", stat.mean) // "Média"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stat.columnName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$selectedMetric: $highlightValue",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Detailed metrics grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatDetailItem("Média", String.format(Locale.getDefault(), "%.2f", stat.mean))
                        StatDetailItem("Mediana", String.format(Locale.getDefault(), "%.2f", stat.median))
                        StatDetailItem("Desv. Padrão", String.format(Locale.getDefault(), "%.2f", stat.stdDev))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatDetailItem("Mín / Máx", "${String.format(Locale.getDefault(), "%.1f", stat.min)} / ${String.format(Locale.getDefault(), "%.1f", stat.max)}")
                        StatDetailItem("Soma", String.format(Locale.getDefault(), "%.1f", stat.sum))
                        StatDetailItem("Contagem", "${stat.count}")
                    }
                }
            }
        }
    }
}

@Composable
fun StatDetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun BarChartTab(
    columnStats: List<ColumnStats>,
    activeTooltipIndex: Int?,
    onTooltipChanged: (Int?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Gráfico de Barras (Médias por Coluna)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (columnStats.isEmpty()) {
                Text("Sem dados para exibir.")
            } else {
                val primaryColor = MaterialTheme.colorScheme.primary

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .pointerInput(columnStats) {
                            detectTapGestures { offset ->
                                val barWidth = size.width / (columnStats.size * 2)
                                val tappedIndex = (offset.x / (barWidth * 2)).toInt()
                                if (tappedIndex in columnStats.indices) {
                                    onTooltipChanged(if (activeTooltipIndex == tappedIndex) null else tappedIndex)
                                } else {
                                    onTooltipChanged(null)
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxVal = columnStats.maxOfOrNull { it.mean }?.takeIf { it > 0.0 } ?: 1.0
                        val barWidth = size.width / (columnStats.size * 2f)

                        columnStats.forEachIndexed { index, stat ->
                            val barHeight = ((stat.mean / maxVal) * (size.height - 40f)).toFloat()
                            val left = index * (barWidth * 2f) + barWidth * 0.5f
                            val top = size.height - barHeight - 30f

                            drawRect(
                                color = if (activeTooltipIndex == index) primaryColor else primaryColor.copy(alpha = 0.8f),
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight)
                            )
                        }
                    }
                }

                if (activeTooltipIndex != null && activeTooltipIndex in columnStats.indices) {
                    val stat = columnStats[activeTooltipIndex]
                    Surface(
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${stat.columnName}: Média = ${String.format(Locale.getDefault(), "%.2f", stat.mean)}",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Text(
                        text = "Toque em uma barra para ver detalhes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LineChartTab(
    columnStats: List<ColumnStats>,
    activeTooltipIndex: Int?,
    onTooltipChanged: (Int?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Gráfico de Linhas (Tendência de Médias)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (columnStats.size < 2) {
                Text("São necessárias pelo menos 2 colunas para exibir o gráfico de linhas.")
            } else {
                val primaryColor = MaterialTheme.colorScheme.primary

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .pointerInput(columnStats) {
                            detectTapGestures { offset ->
                                val stepX = size.width / (columnStats.size - 1).coerceAtLeast(1)
                                val tappedIndex = (offset.x / stepX).roundToInt()
                                if (tappedIndex in columnStats.indices) {
                                    onTooltipChanged(if (activeTooltipIndex == tappedIndex) null else tappedIndex)
                                } else {
                                    onTooltipChanged(null)
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxVal = columnStats.maxOfOrNull { it.mean }?.takeIf { it > 0.0 } ?: 1.0
                        val stepX = size.width / (columnStats.size - 1).coerceAtLeast(1)

                        val path = Path()
                        columnStats.forEachIndexed { index, stat ->
                            val x = index * stepX
                            val y = size.height - (((stat.mean / maxVal) * (size.height - 40f))).toFloat() - 30f
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                            drawCircle(
                                color = primaryColor,
                                radius = if (activeTooltipIndex == index) 8f else 5f,
                                center = Offset(x, y)
                            )
                        }

                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                if (activeTooltipIndex != null && activeTooltipIndex in columnStats.indices) {
                    val stat = columnStats[activeTooltipIndex]
                    Surface(
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${stat.columnName}: Média = ${String.format(Locale.getDefault(), "%.2f", stat.mean)}",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Text(
                        text = "Toque em um ponto da linha para ver detalhes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DistributionTab(columnStats: List<ColumnStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Distribuição e Dispersão",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            columnStats.forEach { stat ->
                val range = (stat.max - stat.min).takeIf { it > 0.0 } ?: 1.0
                val progress = if (range > 0.0) (stat.stdDev / range).toFloat().coerceIn(0f, 1f) else 0f

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stat.columnName, fontWeight = FontWeight.SemiBold)
                        Text("Desv. Padrão: ${String.format(Locale.getDefault(), "%.2f", stat.stdDev)}")
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RawValuesTab(columnStats: List<ColumnStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Valores Analisados Brutos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            columnStats.forEach { stat ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stat.columnName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Valores (${stat.rawValues.size}): ${stat.rawValues.take(15).joinToString(", ")}${if (stat.rawValues.size > 15) "..." else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
