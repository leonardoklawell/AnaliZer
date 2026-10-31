package com.example.analizer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    containerColor = Color(0xFF0D1525),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color(0xFFEBA036)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0D1525)
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0D1525),
                    selectedTextColor = Color(0xFFEBA036),
                    indicatorColor = Color(0xFFEBA036),
                    unselectedIconColor = Color(0xFF8A99B5),
                    unselectedTextColor = Color(0xFF8A99B5)
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Import,
                    onClick = { onScreenSelected(Screen.Import) },
                    icon = { Icon(Icons.Rounded.FileUpload, contentDescription = "Importar") },
                    label = { Text("Importar") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Data,
                    onClick = { onScreenSelected(Screen.Data) },
                    icon = { Icon(Icons.Rounded.TableChart, contentDescription = "Dados") },
                    label = { Text("Dados") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Config,
                    onClick = { onScreenSelected(Screen.Config) },
                    icon = { Icon(Icons.Rounded.Tune, contentDescription = "Configurar") },
                    label = { Text("Configurar") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Analytics,
                    onClick = { onScreenSelected(Screen.Analytics) },
                    icon = { Icon(Icons.Rounded.Analytics, contentDescription = "Análise") },
                    label = { Text("Análise") },
                    colors = navItemColors
                )
            }
        },
        containerColor = Color(0xFFF5E8C7)
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
                    containerColor = Color(0xFFF5E8C7),
                    contentColor = Color(0xFF0D1525),
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color(0xFFEBA036)
                            )
                        }
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                activeTooltipIndex = null
                            },
                            text = { Text(title, fontWeight = FontWeight.SemiBold) },
                            selectedContentColor = Color(0xFF0D1525),
                            unselectedContentColor = Color(0xFF788497)
                        )
                    }
                }

                if (selectedTab == 0) {
                    StatisticsTab(columnStats = columnStats, config = config, spreadsheetData = spreadsheetData)
                } else if (columnStats.isEmpty()) {
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
                        1 -> BarChartTab(columnStats, config, activeTooltipIndex, onTooltipChanged = { activeTooltipIndex = it })
                        2 -> LineChartTab(columnStats, config, activeTooltipIndex, onTooltipChanged = { activeTooltipIndex = it })
                        3 -> DistributionTab(columnStats)
                        4 -> RawValuesTab(columnStats)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

val DEFAULT_CARD_DETAIL_TITLES = listOf("Média", "Mediana", "Desv. Padrão", "Mín / Máx", "Soma", "Contagem")

fun resolveGroupName(
    group: com.example.analizer.model.DisplayGroup?,
    spreadsheetData: SpreadsheetData?,
    currentSheet: String = "",
    defaultIndex: Int = 0
): String {
    if (group == null) return "Card ${defaultIndex + 1}"
    val effectiveSheet = group.selectedSheets.firstOrNull() ?: currentSheet.ifBlank { spreadsheetData?.sheetNames?.firstOrNull() ?: "" }
    val sheetRows = if (!group.selectedFolder.isNullOrBlank()) {
        spreadsheetData?.getRows(group.selectedFolder, effectiveSheet) ?: (spreadsheetData?.sheets?.get(effectiveSheet) ?: emptyList())
    } else {
        spreadsheetData?.sheets?.get(effectiveSheet) ?: emptyList()
    }
    return when (group.nameSourceType) {
        "CELL" -> {
            val cellText = com.example.analizer.domain.StatisticsEngine().resolveCellText(sheetRows, group.cellReference)
            if (!cellText.isNullOrBlank()) cellText else group.name.ifBlank { if (group.selectedFolder.isNotBlank()) group.selectedFolder else "Célula ${group.cellReference}" }
        }
        "SHEET", "SHEET_NAME", "ABANOME" -> effectiveSheet.ifBlank { group.name.ifBlank { if (group.selectedFolder.isNotBlank()) group.selectedFolder else "Card ${defaultIndex + 1}" } }
        else -> group.name.ifBlank { if (group.selectedFolder.isNotBlank()) group.selectedFolder else "Card ${defaultIndex + 1}" }
    }
}

fun getFieldValueForCardConfigField(
    fieldIndex: Int,
    card: com.example.analizer.model.CardConfig,
    allStats: List<ColumnStats>,
    displayGroups: List<com.example.analizer.model.DisplayGroup>,
    cardTitle: String = ""
): Pair<String, String> {
    val defaultTitle = DEFAULT_CARD_DETAIL_TITLES.getOrElse(fieldIndex) { "Campo ${fieldIndex + 1}" }
    val linkedGroup = displayGroups.find { it.id == card.groupId || it.name == card.groupId }
    val mapping = card.fieldMappings.find { it.fieldIndex == fieldIndex }

    val titleRef = mapping?.titleItemReference?.ifBlank { defaultTitle } ?: defaultTitle
    val matchedTitleItem = linkedGroup?.items?.find { it.id == titleRef || it.customLabel == titleRef }
    val title = matchedTitleItem?.let { it.customLabel.ifBlank { defaultTitle } } ?: titleRef.ifBlank { defaultTitle }

    // If no group is linked or no stats are loaded from the spreadsheet, return "--"
    if (linkedGroup == null || allStats.isEmpty()) {
        return Pair(title, "--")
    }

    // Strictly filter stats belonging ONLY to this card's linked group
    val groupStats = allStats.filter { stat ->
        (cardTitle.isNotBlank() && stat.groupName == cardTitle) ||
        stat.groupName == linkedGroup.name ||
        stat.groupName == linkedGroup.id
    }

    if (groupStats.isEmpty()) {
        return Pair(title, "--")
    }

    val valueItemRef = mapping?.valueItemReference.orEmpty().trim()
    if (valueItemRef == "none") {
        return Pair(title, "--")
    }
    val targetItem = if (valueItemRef.isNotBlank()) {
        linkedGroup.items.find { it.id == valueItemRef || it.customLabel == valueItemRef }
    } else {
        // Only map to group item if explicitly configured at this index
        linkedGroup.items.getOrNull(fieldIndex)
    }

    if (targetItem == null) {
        return Pair(title, "--")
    }

    val targetStat = groupStats.find { stat ->
        stat.columnIndex in targetItem.startColumn..targetItem.endColumn &&
        (targetItem.customLabel.isBlank() || stat.columnName.contains(targetItem.customLabel))
    } ?: groupStats.find { stat ->
        stat.columnIndex in targetItem.startColumn..targetItem.endColumn
    }

    if (targetStat == null || targetStat.count <= 0) {
        return Pair(title, "--")
    }

    val metricToUse = if (targetItem.metric.isNotBlank()) targetItem.metric else targetStat.metric
    val value = when (metricToUse) {
        "Soma" -> String.format(Locale.getDefault(), "%.1f", targetStat.sum)
        "Mediana" -> String.format(Locale.getDefault(), "%.2f", targetStat.median)
        "Mínimo" -> String.format(Locale.getDefault(), "%.1f", targetStat.min)
        "Máximo" -> String.format(Locale.getDefault(), "%.1f", targetStat.max)
        "Mín / Máx" -> "${String.format(Locale.getDefault(), "%.1f", targetStat.min)} / ${String.format(Locale.getDefault(), "%.1f", targetStat.max)}"
        "Desvio Padrão" -> if (targetStat.count > 1) String.format(Locale.getDefault(), "%.2f", targetStat.stdDev) else "--"
        "Contagem" -> "${targetStat.count}"
        "Valor da Célula" -> targetStat.rawStringValues.firstOrNull()?.takeIf { it.isNotBlank() } ?: "--"
        "Variância" -> String.format(Locale.getDefault(), "%.2f", targetStat.stdDev * targetStat.stdDev)
        else -> String.format(Locale.getDefault(), "%.2f", targetStat.mean)
    }
    return Pair(title, value)
}

@Composable
fun AnalysisStatCard(
    cardConfig: com.example.analizer.model.CardConfig,
    columnStats: List<ColumnStats>,
    displayGroups: List<com.example.analizer.model.DisplayGroup>,
    spreadsheetData: SpreadsheetData?,
    currentSheetName: String = "",
    cardIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val linkedGroup = displayGroups.find { it.id == cardConfig.groupId || it.name == cardConfig.groupId }
    val cardTitle = resolveGroupName(linkedGroup, spreadsheetData, currentSheetName, cardIndex)

    val targetStat = columnStats.find {
        (cardTitle.isNotBlank() && it.groupName == cardTitle) ||
        it.groupName == linkedGroup?.name ||
        it.groupName == cardConfig.groupId
    }
    val highlightSum = if (targetStat != null && targetStat.count > 0) {
        String.format(Locale.getDefault(), "%.2f", targetStat.sum)
    } else {
        "--"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)),
        border = BorderStroke(1.dp, Color(0xFF1E283E))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cardTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF222F49)
                ) {
                    Text(
                        text = if (highlightSum == "--") "Soma: --" else "Soma: $highlightSum",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEBA036)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF1E283E), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            val visibleFieldIndices = mutableListOf(0, 1, 2, 3, 4, 5)
            for (idx in 6 until 14) {
                val m = cardConfig.fieldMappings.find { it.fieldIndex == idx }
                if (m != null && m.valueItemReference.isNotBlank()) {
                    visibleFieldIndices.add(idx)
                }
            }

            val chunked = visibleFieldIndices.chunked(3)
            chunked.forEachIndexed { rowIndex, rowChunk ->
                if (rowIndex > 0) {
                    Spacer(modifier = Modifier.height(18.dp))
                }
                val f1 = getFieldValueForCardConfigField(rowChunk[0], cardConfig, columnStats, displayGroups, cardTitle)
                val f2 = if (rowChunk.size > 1) getFieldValueForCardConfigField(rowChunk[1], cardConfig, columnStats, displayGroups, cardTitle) else null
                val f3 = if (rowChunk.size > 2) getFieldValueForCardConfigField(rowChunk[2], cardConfig, columnStats, displayGroups, cardTitle) else null

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        StatDetailItem(f1.first, f1.second)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (f2 != null) {
                            StatDetailItem(f2.first, f2.second)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (f3 != null) {
                            StatDetailItem(f3.first, f3.second)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricSummaryCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5E8C7)),
        border = BorderStroke(1.dp, Color(0xFFEDE6DA))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = Color(0xFF788497),
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
        }
    }
}

@Composable
fun StatisticsTab(
    columnStats: List<ColumnStats>,
    config: AnalysisConfig,
    spreadsheetData: SpreadsheetData? = null
) {
    val cards = config.effectiveCards
    val displayGroups = if (config.displayGroups.isNotEmpty()) config.displayGroups else config.effectiveDisplayGroups
    val totalRows = spreadsheetData?.sheets?.values?.maxOfOrNull { it.size } ?: 0
    val totalCols = columnStats.size
    val activeGroups = displayGroups.size
    val totalCards = cards.size

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 4 Metric Strip (Matching Fintech Management Dashboard Print)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricSummaryCard(
                label = "Linhas",
                value = if (totalRows > 0) "$totalRows" else "--",
                accentColor = Color(0xFFEBA036),
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                label = "Colunas",
                value = if (totalCols > 0) "$totalCols" else "--",
                accentColor = Color(0xFFE07A5F),
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                label = "Grupos",
                value = if (activeGroups > 0) "$activeGroups" else "--",
                accentColor = Color(0xFF0D1525),
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                label = "Cards",
                value = if (totalCards > 0) "$totalCards" else "--",
                accentColor = Color(0xFFD4A373),
                modifier = Modifier.weight(1f)
            )
        }

        if (cards.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)),
                border = BorderStroke(1.dp, Color(0xFF1E283E))
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhum card configurado.", color = Color(0xFF94A3B8))
                }
            }
        } else {
            cards.forEachIndexed { cardIdx, cardConfig ->
                AnalysisStatCard(
                    cardConfig = cardConfig,
                    columnStats = columnStats,
                    displayGroups = displayGroups,
                    spreadsheetData = spreadsheetData,
                    currentSheetName = config.sheetName,
                    cardIndex = cardIdx
                )
            }
        }
    }
}

@Composable
fun StatDetailItem(label: String, value: String) {
    val displayValue = if (value.isBlank() || value == "-") "--" else value
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = Color(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = displayValue,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
fun BarChartTab(
    columnStats: List<ColumnStats>,
    config: AnalysisConfig,
    activeTooltipIndex: Int?,
    onTooltipChanged: (Int?) -> Unit
) {
    val activeStats = if (config.chartSelectedGroupIds.isNotEmpty()) {
        columnStats.filter { config.chartSelectedGroupIds.contains(it.groupName) }
    } else {
        columnStats
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5E8C7)),
        border = BorderStroke(1.dp, Color(0xFFEDE6DA))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Gráfico de Barras (Médias por Coluna)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            if (activeStats.isEmpty()) {
                Text("Sem dados para exibir para os grupos selecionados.", color = Color(0xFF788497))
            } else {
                val barColor = Color(0xFFE8A25D)
                val activeBarColor = Color(0xFF0D1525)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .pointerInput(activeStats) {
                            detectTapGestures { offset ->
                                val count = activeStats.size.coerceAtLeast(1)
                                val barWidth = size.width / (count * 2f)
                                if (barWidth > 0f) {
                                    val tappedIndex = (offset.x / (barWidth * 2f)).toInt()
                                    if (tappedIndex in activeStats.indices) {
                                        onTooltipChanged(if (activeTooltipIndex == tappedIndex) null else tappedIndex)
                                    } else {
                                        onTooltipChanged(null)
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxVal = activeStats.maxOfOrNull { it.mean }?.takeIf { it > 0.0 && !it.isNaN() } ?: 1.0
                        val count = activeStats.size.coerceAtLeast(1)
                        val barWidth = (size.width / (count * 2f)).coerceAtLeast(0f)

                        activeStats.forEachIndexed { index, stat ->
                            val safeMean = if (stat.mean.isNaN() || stat.mean.isInfinite()) 0.0 else stat.mean
                            val barHeight = ((safeMean.coerceAtLeast(0.0) / maxVal) * (size.height - 40f)).toFloat().coerceIn(0f, size.height)
                            val left = index * (barWidth * 2f) + barWidth * 0.5f
                            val top = (size.height - barHeight - 30f).coerceIn(0f, size.height)

                            if (barWidth > 0f && barHeight > 0f) {
                                drawRect(
                                    color = if (activeTooltipIndex == index) activeBarColor else barColor,
                                    topLeft = Offset(left, top),
                                    size = Size(barWidth, barHeight)
                                )
                            }
                        }
                    }
                }

                if (activeTooltipIndex != null && activeTooltipIndex in activeStats.indices) {
                    val stat = activeStats[activeTooltipIndex]
                    Surface(
                        color = Color(0xFF0D1525),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${stat.columnName}: Média = ${String.format(Locale.getDefault(), "%.2f", stat.mean)}",
                            color = Color(0xFFEBA036),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = "Toque em uma barra para ver detalhes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF788497)
                    )
                }
            }
        }
    }
}

@Composable
fun LineChartTab(
    columnStats: List<ColumnStats>,
    config: AnalysisConfig,
    activeTooltipIndex: Int?,
    onTooltipChanged: (Int?) -> Unit
) {
    val activeStats = if (config.chartSelectedGroupIds.isNotEmpty()) {
        columnStats.filter { config.chartSelectedGroupIds.contains(it.groupName) }
    } else {
        columnStats
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5E8C7)),
        border = BorderStroke(1.dp, Color(0xFFEDE6DA))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Gráfico de Linhas (Tendência de Médias)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            if (activeStats.size < 2) {
                Text("São necessárias pelo menos 2 colunas para exibir o gráfico de linhas.", color = Color(0xFF788497))
            } else {
                val lineColor = Color(0xFF0D1525)
                val dotColor = Color(0xFFE07A5F)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .pointerInput(activeStats) {
                            detectTapGestures { offset ->
                                val stepX = (size.width / (activeStats.size - 1).coerceAtLeast(1))
                                if (stepX > 0f) {
                                    val tappedIndex = (offset.x / stepX).roundToInt()
                                    if (tappedIndex in activeStats.indices) {
                                        onTooltipChanged(if (activeTooltipIndex == tappedIndex) null else tappedIndex)
                                    } else {
                                        onTooltipChanged(null)
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxVal = activeStats.maxOfOrNull { it.mean }?.takeIf { it > 0.0 && !it.isNaN() } ?: 1.0
                        val stepX = (size.width / (activeStats.size - 1).coerceAtLeast(1))

                        val path = Path()
                        activeStats.forEachIndexed { index, stat ->
                            val safeMean = if (stat.mean.isNaN() || stat.mean.isInfinite()) 0.0 else stat.mean
                            val x = index * stepX
                            val y = (size.height - (((safeMean.coerceAtLeast(0.0) / maxVal) * (size.height - 40f))).toFloat() - 30f).coerceIn(0f, size.height)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                            drawCircle(
                                color = if (activeTooltipIndex == index) Color(0xFFEBA036) else dotColor,
                                radius = if (activeTooltipIndex == index) 9f else 6f,
                                center = Offset(x, y)
                            )
                        }

                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.5.dp.toPx())
                        )
                    }
                }

                if (activeTooltipIndex != null && activeTooltipIndex in activeStats.indices) {
                    val stat = activeStats[activeTooltipIndex]
                    Surface(
                        color = Color(0xFF0D1525),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${stat.columnName}: Média = ${String.format(Locale.getDefault(), "%.2f", stat.mean)}",
                            color = Color(0xFFEBA036),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = "Toque em um ponto da linha para ver detalhes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF788497)
                    )
                }
            }
        }
    }
}

@Composable
fun DistributionTab(columnStats: List<ColumnStats>) {
    val progressColors = listOf(
        Color(0xFF0D1525), // Navy
        Color(0xFFEBA036), // Amber
        Color(0xFFE07A5F), // Terracotta
        Color(0xFF6B8F71)  // Sage
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5E8C7)),
        border = BorderStroke(1.dp, Color(0xFFEDE6DA))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Distribuição e Dispersão",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            columnStats.forEachIndexed { idx, stat ->
                val range = (stat.max - stat.min).takeIf { it > 0.0 && !it.isNaN() } ?: 1.0
                val rawProgress = if (stat.stdDev.isNaN() || range.isNaN() || range <= 0.0) 0f else (stat.stdDev / range).toFloat()
                val progress = rawProgress.takeIf { !it.isNaN() && !it.isInfinite() }?.coerceIn(0f, 1f) ?: 0f
                val color = progressColors[idx % progressColors.size]

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stat.columnName, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        val safeStd = if (stat.stdDev.isNaN() || stat.stdDev.isInfinite()) 0.0 else stat.stdDev
                        Text("Desv. Padrão: ${String.format(Locale.getDefault(), "%.2f", safeStd)}", color = Color(0xFF788497))
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = color,
                        trackColor = Color(0xFFF1ECE1)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5E8C7)),
        border = BorderStroke(1.dp, Color(0xFFEDE6DA))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Valores Analisados Brutos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            columnStats.forEach { stat ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF7F4EB)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEDE6DA)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stat.columnName,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D1525)
                        )
                        Text(
                            text = "Valores (${stat.rawValues.size}): ${stat.rawValues.take(15).joinToString(", ")}${if (stat.rawValues.size > 15) "..." else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF788497)
                        )
                    }
                }
            }
        }
    }
}
