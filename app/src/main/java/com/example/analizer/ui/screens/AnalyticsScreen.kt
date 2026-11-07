package com.example.analizer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    columnStats: List<ColumnStats>,
    spreadsheetData: SpreadsheetData?,
    config: AnalysisConfig = AnalysisConfig(),
    onMonthSelected: (String) -> Unit = {},
    onConfigChanged: (AnalysisConfig) -> Unit = {},
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    val tabs = listOf("Estatísticas", "Gráfico de Barras", "Gráfico de Linhas", "Distribuição", "Valores Brutos")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

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
            ) {
                // Tabs for Analytics views
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 0.dp,
                    containerColor = Color(0xFFF5E8C7),
                    contentColor = Color(0xFF0D1525),
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = Color(0xFFEBA036)
                            )
                        }
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                                activeTooltipIndex = null
                            },
                            text = { Text(title, fontWeight = FontWeight.SemiBold) },
                            selectedContentColor = Color(0xFF0D1525),
                            unselectedContentColor = Color(0xFF788497)
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (page == 0) {
                            StatisticsTab(
                                columnStats = columnStats,
                                config = config,
                                spreadsheetData = spreadsheetData,
                                onMonthSelected = onMonthSelected,
                                onConfigChanged = onConfigChanged
                            )
                        } else if (page in 1..2) {
                            when (page) {
                                1 -> BarChartTab(
                                    columnStats = columnStats,
                                    config = config,
                                    spreadsheetData = spreadsheetData,
                                    onConfigChanged = onConfigChanged,
                                    activeTooltipIndex = activeTooltipIndex,
                                    onTooltipChanged = { activeTooltipIndex = it }
                                )
                                2 -> LineChartTab(
                                    columnStats = columnStats,
                                    config = config,
                                    spreadsheetData = spreadsheetData,
                                    onConfigChanged = onConfigChanged,
                                    activeTooltipIndex = activeTooltipIndex,
                                    onTooltipChanged = { activeTooltipIndex = it }
                                )
                            }
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
                            when (page) {
                                3 -> DistributionTab(columnStats)
                                4 -> RawValuesTab(columnStats)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
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
    val cleanFolder = group.selectedFolder.substringBefore(" - ").substringAfterLast("/").trim()
    return when (group.nameSourceType) {
        "CELL" -> {
            val cellText = com.example.analizer.domain.StatisticsEngine().resolveCellText(sheetRows, group.cellReference)
            if (!cellText.isNullOrBlank()) cellText else group.name.ifBlank { if (cleanFolder.isNotBlank()) cleanFolder else "Célula ${group.cellReference}" }
        }
        "SHEET", "SHEET_NAME", "ABANOME" -> effectiveSheet.ifBlank { group.name.ifBlank { if (cleanFolder.isNotBlank()) cleanFolder else "Card ${defaultIndex + 1}" } }
        else -> group.name.ifBlank { if (cleanFolder.isNotBlank()) cleanFolder else "Card ${defaultIndex + 1}" }
    }
}

data class ResolvedCardItem(
    val label: String,
    val value: String,
    val enabled: Boolean
)

fun resolveCardFieldValue(
    fieldIndex: Int,
    cardConfig: com.example.analizer.model.CardConfig,
    columnStats: List<ColumnStats>,
    displayGroups: List<com.example.analizer.model.DisplayGroup>,
    spreadsheetData: SpreadsheetData?,
    selectedMonth: String = "TODOS",
    monthOptions: List<com.example.analizer.model.MonthSelectorOption> = emptyList()
): ResolvedCardItem {
    val defaultLabel = "ITEM ${fieldIndex + 1}"
    val mapping = cardConfig.fieldMappings.find { it.fieldIndex == fieldIndex }
    val isEnabled = mapping?.enabled ?: true
    if (!isEnabled) {
        return ResolvedCardItem(label = "", value = "", enabled = false)
    }

    var titleRef = mapping?.titleItemReference.orEmpty().trim()
    var cellRef = mapping?.valueItemReference.orEmpty().trim()

    val linkedGroup = displayGroups.find { it.id == cardConfig.groupId }
    val folderName = linkedGroup?.selectedFolder?.ifBlank { linkedGroup?.name ?: "" } ?: ""
    val sheetName = cardConfig.selectedSheet.ifBlank { linkedGroup?.selectedSheets?.firstOrNull() ?: "" }

    val sheetRows = if (folderName.isNotBlank()) {
        spreadsheetData?.getRows(folderName, sheetName) ?: (spreadsheetData?.sheets?.get(sheetName) ?: emptyList())
    } else {
        spreadsheetData?.sheets?.get(sheetName) ?: emptyList()
    }

    // Shift cell references according to month/period selector if checked for this card and item
    val isMappingObey = mapping?.obeyMonthSelector ?: true
    if (cardConfig.obeyMonthSelector && isMappingObey && !selectedMonth.equals("TODOS", ignoreCase = true)) {
        val selectedOpt = monthOptions.find {
            it.name.equals(selectedMonth, ignoreCase = true) ||
            com.example.analizer.model.resolveMonthOptionDisplayName(it, spreadsheetData, folderName, sheetName, displayGroups).equals(selectedMonth, ignoreCase = true)
        }
        val offset = selectedOpt?.columnOffset ?: 0
        if (offset != 0) {
            if (cellRef.isNotBlank()) {
                cellRef = com.example.analizer.domain.StatisticsEngine().shiftCellReference(cellRef, offset)
            }
            if (titleRef.isNotBlank() && com.example.analizer.domain.StatisticsEngine().parseCellReference(titleRef) != null) {
                titleRef = com.example.analizer.domain.StatisticsEngine().shiftCellReference(titleRef, offset)
            }
        }
    }

    // Resolve cell reference for title reference if it matches cell format (e.g. A1, S3)
    val resolvedTitleText = if (titleRef.isNotBlank()) {
        com.example.analizer.domain.StatisticsEngine().resolveCellText(sheetRows, titleRef)
    } else null
    val rawLabel = resolvedTitleText ?: titleRef.ifBlank { defaultLabel }
    val label = com.example.analizer.model.limitDecimalDigits(rawLabel, 3)

    // Resolve cell reference for value reference
    val rawValue = if (cellRef.isNotBlank()) {
        com.example.analizer.domain.StatisticsEngine().resolveCellText(sheetRows, cellRef) ?: cellRef
    } else if (resolvedTitleText != null) {
        resolvedTitleText
    } else {
        "--"
    }

    val formattedValue = com.example.analizer.model.formatFieldValue(
        rawValue,
        mapping?.numberFormat ?: "AUTO",
        mapping?.thousandsSeparator ?: true
    )

    return ResolvedCardItem(label = label, value = formattedValue, enabled = true)
}

@Composable
fun AnalysisStatCard(
    cardConfig: com.example.analizer.model.CardConfig,
    columnStats: List<ColumnStats>,
    displayGroups: List<com.example.analizer.model.DisplayGroup>,
    spreadsheetData: SpreadsheetData?,
    currentSheetName: String = "",
    cardIndex: Int = 0,
    selectedMonth: String = "TODOS",
    monthOptions: List<com.example.analizer.model.MonthSelectorOption> = emptyList(),
    modifier: Modifier = Modifier
) {
    val resolvedValues = remember(cardConfig, columnStats, displayGroups, spreadsheetData, selectedMonth, monthOptions) {
        (0 until 19).map { idx ->
            resolveCardFieldValue(idx, cardConfig, columnStats, displayGroups, spreadsheetData, selectedMonth, monthOptions)
        }
    }

    val item1 = resolvedValues[0]
    val item2 = resolvedValues[1]
    val item3 = resolvedValues[2]

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1825)),
        border = BorderStroke(1.dp, Color(0xFF1E283E))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row: ITEM 1 & ITEM 2 on Top Left, ITEM 3 on Top Right Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (item1.enabled) {
                        val text1 = if (item1.value != "--" && item1.value.isNotBlank()) item1.value else item1.label
                        Text(
                            text = text1,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    if (item2.enabled) {
                        val text2 = if (item2.value != "--" && item2.value.isNotBlank()) item2.value else item2.label
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = text2,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEBA036)
                        )
                    }
                }

                if (item3.enabled) {
                    val text3 = if (item3.value != "--" && item3.value.isNotBlank()) item3.value else item3.label
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFEBA036)
                    ) {
                        Text(
                            text = text3,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D1525)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF2C3E55), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            data class ColumnPair(val labelItem: ResolvedCardItem, val valueItem: ResolvedCardItem)

            // Body Row 1: Top 2 lines (ITEM 4/5, ITEM 8/9, ITEM 12/13, ITEM 16/17)
            val row1Candidates = listOf(
                ColumnPair(resolvedValues[3], resolvedValues[4]),
                ColumnPair(resolvedValues[7], resolvedValues[8]),
                ColumnPair(resolvedValues[11], resolvedValues[12]),
                ColumnPair(resolvedValues[15], resolvedValues[16])
            )
            val activeRow1 = row1Candidates.filter { it.labelItem.enabled || it.valueItem.enabled }

            // Body Row 2: Bottom 2 lines (ITEM 6/7, ITEM 10/11, ITEM 14/15, ITEM 18/19)
            val row2Candidates = listOf(
                ColumnPair(resolvedValues[5], resolvedValues[6]),
                ColumnPair(resolvedValues[9], resolvedValues[10]),
                ColumnPair(resolvedValues[13], resolvedValues[14]),
                ColumnPair(resolvedValues[17], resolvedValues[18])
            )
            val activeRow2 = row2Candidates.filter { it.labelItem.enabled || it.valueItem.enabled }

            fun getLabel(item: ResolvedCardItem): String = if (item.enabled) (if (item.value != "--" && item.value.isNotBlank()) item.value else item.label) else ""
            fun getValue(item: ResolvedCardItem): String = if (item.enabled) (if (item.value != "--" && item.value.isNotBlank()) item.value else item.label) else ""

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1 of Body: Top 2 lines with independent dynamic column distribution
                if (activeRow1.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeRow1.forEach { pair ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                StatDetailItem(getLabel(pair.labelItem), getValue(pair.valueItem))
                            }
                        }
                    }
                }

                // Row 2 of Body: Bottom 2 lines with independent dynamic column distribution
                if (activeRow2.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        activeRow2.forEach { pair ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                StatDetailItem(getLabel(pair.labelItem), getValue(pair.valueItem))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthSelectorBar(
    selectedMonth: String,
    onMonthSelected: (String) -> Unit,
    monthOptions: List<com.example.analizer.model.MonthSelectorOption> = emptyList(),
    spreadsheetData: SpreadsheetData? = null,
    config: com.example.analizer.model.AnalysisConfig? = null,
    displayGroups: List<com.example.analizer.model.DisplayGroup> = emptyList(),
    modifier: Modifier = Modifier
) {
    val displayOptions = if (monthOptions.isNotEmpty()) monthOptions else com.example.analizer.model.DEFAULT_MONTH_OPTIONS
    val folder = config?.selectedFolder.orEmpty()
    val sheet = config?.sheetName.orEmpty()

    val selectedOpt = displayOptions.find {
        it.name.equals(selectedMonth, ignoreCase = true) ||
        com.example.analizer.model.resolveMonthOptionDisplayName(it, spreadsheetData, folder, sheet, displayGroups).equals(selectedMonth, ignoreCase = true)
    }
    val selectedDisplayLabel = selectedOpt?.let {
        com.example.analizer.model.resolveMonthOptionDisplayName(it, spreadsheetData, folder, sheet, displayGroups)
    } ?: selectedMonth

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1525))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mês em Análise",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEBA036)
                )
                Text(
                    text = if (selectedMonth.equals("TODOS", ignoreCase = true) || selectedDisplayLabel.equals("TODOS", ignoreCase = true)) "Exibindo Acumulado (Todos)" else "Mês: $selectedDisplayLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }

            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                displayOptions.forEach { opt ->
                    val rawMonthName = opt.name
                    val displayName = com.example.analizer.model.resolveMonthOptionDisplayName(opt, spreadsheetData, folder, sheet, displayGroups)
                    val isSelected = rawMonthName.equals(selectedMonth, ignoreCase = true) || displayName.equals(selectedMonth, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMonthSelected(rawMonthName) },
                        label = { Text(displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEBA036),
                            selectedLabelColor = Color(0xFF0D1525),
                            containerColor = Color(0xFF1B273B),
                            labelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) Color(0xFFEBA036) else Color(0xFF2C3E55),
                            selectedBorderColor = Color(0xFFEBA036),
                            enabled = true,
                            selected = isSelected
                        )
                    )
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = Color(0xFF64748B),
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
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsTab(
    columnStats: List<ColumnStats>,
    config: AnalysisConfig,
    spreadsheetData: SpreadsheetData? = null,
    onMonthSelected: (String) -> Unit = {},
    onConfigChanged: (AnalysisConfig) -> Unit = {}
) {
    val cards = config.effectiveCards
    val displayGroups = if (config.displayGroups.isNotEmpty()) config.displayGroups else config.effectiveDisplayGroups
    val totalRows = spreadsheetData?.sheets?.values?.maxOfOrNull { it.size } ?: 0
    val totalCols = columnStats.size
    val activeGroups = displayGroups.size
    val totalCards = cards.size

    val enabledMetrics = if (config.summaryMetrics.isEmpty()) {
        com.example.analizer.model.DEFAULT_SUMMARY_METRICS
    } else {
        config.summaryMetrics.filter { it.enabled }
    }

    var sortFieldIndex by remember(config.sortFieldIndex) { mutableStateOf(config.sortFieldIndex) }
    var sortAscending by remember(config.sortAscending) { mutableStateOf(config.sortAscending) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    var selectedGroupIdsFilter by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedCardIndicesFilter by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var groupFilterExpanded by remember { mutableStateOf(false) }
    var itemFilterExpanded by remember { mutableStateOf(false) }

    val sortTitleToValueIndexMap = remember {
        mapOf(
            3 to 4,    // ITEM 4 -> ITEM 5
            7 to 8,    // ITEM 8 -> ITEM 9
            11 to 12,  // ITEM 12 -> ITEM 13
            15 to 16,  // ITEM 16 -> ITEM 17
            5 to 6,    // ITEM 6 -> ITEM 7
            9 to 10,   // ITEM 10 -> ITEM 11
            13 to 14,  // ITEM 14 -> ITEM 15
            17 to 18   // ITEM 18 -> ITEM 19
        )
    }
    val allowedSortTitleIndices = remember { listOf(3, 7, 11, 15, 5, 9, 13, 17) }

    val sortedCards = remember(cards, sortFieldIndex, sortAscending, config.selectedMonth, config.monthOptions) {
        if (sortFieldIndex == null) cards
        else {
            val titleIdx = sortFieldIndex!!
            val valueIdx = sortTitleToValueIndexMap[titleIdx] ?: titleIdx
            cards.sortedWith(Comparator { c1, c2 ->
                val itemRes1 = resolveCardFieldValue(valueIdx, c1, columnStats, displayGroups, spreadsheetData, config.selectedMonth, config.monthOptions)
                val itemRes2 = resolveCardFieldValue(valueIdx, c2, columnStats, displayGroups, spreadsheetData, config.selectedMonth, config.monthOptions)
                val str1 = if (itemRes1.value != "--" && itemRes1.value.isNotBlank()) itemRes1.value else itemRes1.label
                val str2 = if (itemRes2.value != "--" && itemRes2.value.isNotBlank()) itemRes2.value else itemRes2.label

                val num1 = com.example.analizer.model.parseToDouble(str1)
                val num2 = com.example.analizer.model.parseToDouble(str2)

                val cmp = if (num1 != null && num2 != null) {
                    num1.compareTo(num2)
                } else {
                    str1.compareTo(str2, ignoreCase = true)
                }
                if (sortAscending) cmp else -cmp
            })
        }
    }

    val filteredCards = remember(sortedCards, selectedGroupIdsFilter, selectedCardIndicesFilter) {
        sortedCards.filter { card ->
            val cardIdx = cards.indexOf(card)
            val matchGroup = selectedGroupIdsFilter.isEmpty() || selectedGroupIdsFilter.contains(card.groupId)
            val matchCard = selectedCardIndicesFilter.isEmpty() || selectedCardIndicesFilter.contains(cardIdx)
            matchGroup && matchCard
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter Bar (Grupo / Pasta & Cards) com Checkbox para análise independente
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1825))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown 1: Filtrar por Grupo (com Checkbox por Item)
                Box(modifier = Modifier.weight(1f)) {
                    val currentGroupLabel = when {
                        selectedGroupIdsFilter.isEmpty() -> "Todos os Grupos"
                        selectedGroupIdsFilter.size == 1 -> {
                            val grp = displayGroups.find { it.id == selectedGroupIdsFilter.first() }
                            grp?.name?.takeIf { it.isNotBlank() }
                                ?: grp?.selectedFolder?.substringBefore(" - ")?.substringAfterLast("/")?.trim()
                                ?: "1 Grupo"
                        }
                        else -> "${selectedGroupIdsFilter.size} Grupos"
                    }
                    ExposedDropdownMenuBox(
                        expanded = groupFilterExpanded,
                        onExpandedChange = { groupFilterExpanded = !groupFilterExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentGroupLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Filtrar Grupo (Pasta)", fontSize = 10.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupFilterExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFEBA036), unfocusedBorderColor = Color(0xFF4A5D78),
                                focusedContainerColor = Color(0xFF02121C), unfocusedContainerColor = Color(0xFF02121C),
                                focusedLabelColor = Color(0xFFEBA036), unfocusedLabelColor = Color(0xFFB0BEC5)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = groupFilterExpanded,
                            onDismissRequest = { groupFilterExpanded = false },
                            modifier = Modifier.background(Color(0xFF02121C))
                        ) {
                            val allChecked = selectedGroupIdsFilter.isEmpty()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = allChecked,
                                            onCheckedChange = {
                                                selectedGroupIdsFilter = emptySet()
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFFEBA036),
                                                uncheckedColor = Color(0xFF8A99B5),
                                                checkmarkColor = Color(0xFF0D1525)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Todos os Grupos", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = {
                                    selectedGroupIdsFilter = emptySet()
                                }
                            )
                            HorizontalDivider(color = Color(0xFF1E283E))
                            displayGroups.forEach { grp ->
                                val label = grp.name.takeIf { it.isNotBlank() }
                                    ?: grp.selectedFolder.substringBefore(" - ").substringAfterLast("/").trim().ifBlank { "Grupo" }
                                val isChecked = selectedGroupIdsFilter.contains(grp.id)
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    selectedGroupIdsFilter = if (checked) {
                                                        selectedGroupIdsFilter + grp.id
                                                    } else {
                                                        selectedGroupIdsFilter - grp.id
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFFEBA036),
                                                    uncheckedColor = Color(0xFF8A99B5),
                                                    checkmarkColor = Color(0xFF0D1525)
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(label, color = Color.White)
                                        }
                                    },
                                    onClick = {
                                        selectedGroupIdsFilter = if (isChecked) {
                                            selectedGroupIdsFilter - grp.id
                                        } else {
                                            selectedGroupIdsFilter + grp.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Dropdown 2: Filtrar por Card (com Checkbox por Item)
                Box(modifier = Modifier.weight(1f)) {
                    val currentCardLabel = when {
                        selectedCardIndicesFilter.isEmpty() -> "Todos os Cards"
                        selectedCardIndicesFilter.size == 1 -> {
                            val idx = selectedCardIndicesFilter.first()
                            val card = cards.getOrNull(idx)
                            val group = displayGroups.find { it.id == card?.groupId }
                            val title = if (card != null) resolveGroupName(group, spreadsheetData, config.sheetName, idx) else "Card ${idx + 1}"
                            title
                        }
                        else -> "${selectedCardIndicesFilter.size} Cards"
                    }
                    ExposedDropdownMenuBox(
                        expanded = itemFilterExpanded,
                        onExpandedChange = { itemFilterExpanded = !itemFilterExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentCardLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Filtrar Card", fontSize = 10.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemFilterExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFEBA036), unfocusedBorderColor = Color(0xFF4A5D78),
                                focusedContainerColor = Color(0xFF02121C), unfocusedContainerColor = Color(0xFF02121C),
                                focusedLabelColor = Color(0xFFEBA036), unfocusedLabelColor = Color(0xFFB0BEC5)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = itemFilterExpanded,
                            onDismissRequest = { itemFilterExpanded = false },
                            modifier = Modifier.background(Color(0xFF02121C))
                        ) {
                            val allChecked = selectedCardIndicesFilter.isEmpty()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = allChecked,
                                            onCheckedChange = {
                                                selectedCardIndicesFilter = emptySet()
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFFEBA036),
                                                uncheckedColor = Color(0xFF8A99B5),
                                                checkmarkColor = Color(0xFF0D1525)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Todos os Cards", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = {
                                    selectedCardIndicesFilter = emptySet()
                                }
                            )
                            HorizontalDivider(color = Color(0xFF1E283E))
                            cards.forEachIndexed { idx, card ->
                                val group = displayGroups.find { it.id == card.groupId }
                                val title = resolveGroupName(group, spreadsheetData, config.sheetName, idx)
                                val isChecked = selectedCardIndicesFilter.contains(idx)
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    selectedCardIndicesFilter = if (checked) {
                                                        selectedCardIndicesFilter + idx
                                                    } else {
                                                        selectedCardIndicesFilter - idx
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFFEBA036),
                                                    uncheckedColor = Color(0xFF8A99B5),
                                                    checkmarkColor = Color(0xFF0D1525)
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Card ${idx + 1} - $title", color = Color.White)
                                        }
                                    },
                                    onClick = {
                                        selectedCardIndicesFilter = if (isChecked) {
                                            selectedCardIndicesFilter - idx
                                        } else {
                                            selectedCardIndicesFilter + idx
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
                // Month Selector Bar
        MonthSelectorBar(
            selectedMonth = config.selectedMonth,
            onMonthSelected = onMonthSelected,
            monthOptions = config.monthOptions
        )

        // Summary Metrics Row with White Background Cards
        if (enabledMetrics.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                enabledMetrics.forEachIndexed { index, metric ->
                    val valText = if (metric.valueSourceType == "CELL") {
                        val sName = metric.sheetName.ifBlank { config.sheetName }
                        val sheetRows = if (config.selectedFolder.isNotBlank()) {
                            spreadsheetData?.getRows(config.selectedFolder, sName) ?: (spreadsheetData?.sheets?.get(sName) ?: emptyList())
                        } else {
                            spreadsheetData?.sheets?.get(sName) ?: emptyList()
                        }
                        val rawText = if (metric.cellReference.isNotBlank()) {
                            com.example.analizer.domain.StatisticsEngine().resolveCellText(sheetRows, metric.cellReference) ?: metric.cellReference
                        } else "--"
                        com.example.analizer.model.formatFieldValue(rawText, metric.numberFormat, metric.thousandsSeparator)
                    } else {
                        when (metric.metricType.uppercase()) {
                            "LINHAS" -> if (totalRows > 0) "$totalRows" else "--"
                            "COLUNAS" -> if (totalCols > 0) "$totalCols" else "--"
                            "GRUPOS" -> if (activeGroups > 0) "$activeGroups" else "--"
                            "CARDS" -> if (totalCards > 0) "$totalCards" else "--"
                            else -> "--"
                        }
                    }
                    val colors = listOf(Color(0xFFEBA036), Color(0xFFE07A5F), Color(0xFF0D1525), Color(0xFFD4A373))
                    val accentColor = colors[index % colors.size]

                    MetricSummaryCard(
                        label = metric.label.ifBlank { if (metric.valueSourceType == "CELL") "Célula ${metric.cellReference}" else metric.metricType.lowercase().replaceFirstChar { it.uppercase() } },
                        value = valText,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Sort Control Bar ("Ordenar")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cards de Análise (${filteredCards.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D1525)
            )

            Box {
                Button(
                    onClick = { sortMenuExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D1525),
                        contentColor = Color(0xFFEBA036)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = "Ordenar",
                        tint = Color(0xFFEBA036),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val currentSortLabel = if (sortFieldIndex != null) {
                        val idx = sortFieldIndex!!
                        val titleItemNum = idx + 1
                        val resolvedTitle = cards.firstNotNullOfOrNull { card ->
                            val res = resolveCardFieldValue(idx, card, columnStats, displayGroups, spreadsheetData, config.selectedMonth, config.monthOptions)
                            res.label.takeIf { it.isNotBlank() && !it.equals("ITEM $titleItemNum", ignoreCase = true) }
                        }
                        resolvedTitle ?: "ITEM $titleItemNum"
                    } else "Ordenar"

                    Text(
                        text = if (sortFieldIndex == null) "Ordenar" else "$currentSortLabel (${if (sortAscending) "↑" else "↓"})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier.background(Color(0xFF0D1525))
                ) {
                    DropdownMenuItem(
                        text = { Text("Padrão (Sem ordenação)", color = if (sortFieldIndex == null) Color(0xFFEBA036) else Color.White, fontWeight = FontWeight.Bold) },
                        onClick = {
                            sortFieldIndex = null
                            sortMenuExpanded = false
                            onConfigChanged(config.copy(sortFieldIndex = null))
                        }
                    )
                    HorizontalDivider(color = Color(0xFF1E283E))

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (sortAscending) "Direção: Crescente (A-Z / 0-9) ✓" else "Direção: Decrescente (Z-A / 9-0)",
                                color = Color(0xFFEBA036),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {
                            sortAscending = !sortAscending
                            onConfigChanged(config.copy(sortAscending = sortAscending))
                        }
                    )
                    HorizontalDivider(color = Color(0xFF1E283E))

                    allowedSortTitleIndices.forEach { fieldIdx ->
                        val titleItemNum = fieldIdx + 1
                        val resolvedTitle = cards.firstNotNullOfOrNull { card ->
                            val res = resolveCardFieldValue(fieldIdx, card, columnStats, displayGroups, spreadsheetData, config.selectedMonth, config.monthOptions)
                            res.label.takeIf { it.isNotBlank() && !it.equals("ITEM $titleItemNum", ignoreCase = true) }
                        }
                        val displayName = resolvedTitle ?: "ITEM $titleItemNum"
                        val isSelected = sortFieldIndex == fieldIdx

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = displayName,
                                    color = if (isSelected) Color(0xFFEBA036) else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                sortFieldIndex = fieldIdx
                                sortMenuExpanded = false
                                onConfigChanged(config.copy(sortFieldIndex = fieldIdx, sortAscending = sortAscending))
                            }
                        )
                    }
                }
            }
        }

        if (filteredCards.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)),
                border = BorderStroke(1.dp, Color(0xFF1E283E))
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhum card configurado ou visível para os filtros selecionados.", color = Color(0xFF94A3B8))
                }
            }
        } else {
            // Container branco para cada grupo de cards, separando os grupos visualmente
            val groupedCards = remember(filteredCards, displayGroups) {
                val map = LinkedHashMap<com.example.analizer.model.DisplayGroup?, MutableList<com.example.analizer.model.CardConfig>>()
                displayGroups.forEach { grp ->
                    val cardsInGroup = filteredCards.filter { it.groupId == grp.id }
                    if (cardsInGroup.isNotEmpty()) {
                        map[grp] = cardsInGroup.toMutableList()
                    }
                }
                val ungrouped = filteredCards.filter { card -> displayGroups.none { it.id == card.groupId } }
                if (ungrouped.isNotEmpty()) {
                    map[null] = ungrouped.toMutableList()
                }
                map
            }

            groupedCards.forEach { (grp, groupCards) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val grpTitle = if (grp != null) {
                            resolveGroupName(grp, spreadsheetData, config.sheetName)
                        } else {
                            "Cards Gerais"
                        }
                        val cleanFolder = grp?.selectedFolder?.substringBefore(" - ")?.substringAfterLast("/")?.trim().orEmpty()
                        val sheetName = grp?.selectedSheets?.firstOrNull().orEmpty()
                        val subtitle = listOf(cleanFolder, sheetName).filter { it.isNotBlank() }.joinToString(" • ")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFFFFF7ED), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (grp != null) Icons.Rounded.Folder else Icons.Rounded.FolderOpen,
                                        contentDescription = null,
                                        tint = Color(0xFFEBA036),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = grpTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (subtitle.isNotBlank()) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${groupCards.size} ${if (groupCards.size == 1) "Card" else "Cards"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        groupCards.forEach { cardConfig ->
                            val cardIdx = cards.indexOf(cardConfig).coerceAtLeast(0)
                            AnalysisStatCard(
                                cardConfig = cardConfig,
                                columnStats = columnStats,
                                displayGroups = displayGroups,
                                spreadsheetData = spreadsheetData,
                                currentSheetName = config.sheetName,
                                cardIndex = cardIdx,
                                selectedMonth = config.selectedMonth,
                                monthOptions = config.monthOptions
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatDetailItem(label: String, value: String) {
    val displayValue = if (value.isBlank() || value == "-") "--" else value
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = displayValue,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

data class ChartPointData(
    val label: String,
    val value: Double,
    val formattedValue: String,
    val subtitle: String = ""
)

fun parseCellRange(rangeStr: String): List<String> {
    val clean = rangeStr.trim().uppercase()
    if (!clean.contains(":")) {
        return if (clean.isNotBlank()) listOf(clean) else emptyList()
    }
    val parts = clean.split(":")
    if (parts.size != 2) return emptyList()
    val startRef = parts[0].trim()
    val endRef = parts[1].trim()
    val engine = com.example.analizer.domain.StatisticsEngine()
    val startCell = engine.parseCellReference(startRef) ?: return emptyList()
    val endCell = engine.parseCellReference(endRef) ?: return emptyList()

    val minCol = minOf(startCell.first, endCell.first)
    val maxCol = maxOf(startCell.first, endCell.first)
    val minRow = minOf(startCell.second, endCell.second)
    val maxRow = maxOf(startCell.second, endCell.second)

    val cells = mutableListOf<String>()
    for (r in minRow..maxRow) {
        for (c in minCol..maxCol) {
            var n = c + 1
            var colName = ""
            while (n > 0) {
                val rem = (n - 1) % 26
                colName = ('A' + rem) + colName
                n = (n - 1) / 26
            }
            val rowNum = r + 1
            cells.add("$colName$rowNum")
            if (cells.size >= 50) break
        }
        if (cells.size >= 50) break
    }
    return cells
}

fun resolveItemTitle(
    valueIdx: Int,
    targetCard: com.example.analizer.model.CardConfig?,
    columnStats: List<ColumnStats>,
    displayGroups: List<com.example.analizer.model.DisplayGroup>,
    spreadsheetData: SpreadsheetData?,
    config: AnalysisConfig
): String {
    val titleIdx = when (valueIdx) {
        4 -> 3
        8 -> 7
        12 -> 11
        16 -> 15
        6 -> 5
        10 -> 9
        14 -> 13
        18 -> 17
        else -> (valueIdx - 1).coerceAtLeast(0)
    }
    val itemNum = valueIdx + 1
    if (targetCard != null) {
        val res = resolveCardFieldValue(titleIdx, targetCard, columnStats, displayGroups, spreadsheetData, config.selectedMonth, config.monthOptions)
        if (res.label.isNotBlank() && !res.label.equals("ITEM ${titleIdx + 1}", ignoreCase = true)) {
            return res.label
        }
    }
    return "ITEM $itemNum"
}

fun getCardItemValue(
    card: com.example.analizer.model.CardConfig,
    valueIdx: Int,
    columnStats: List<ColumnStats>,
    displayGroups: List<com.example.analizer.model.DisplayGroup>,
    spreadsheetData: SpreadsheetData?,
    config: AnalysisConfig
): Pair<Double?, String> {
    val res = resolveCardFieldValue(valueIdx, card, columnStats, displayGroups, spreadsheetData, config.selectedMonth, config.monthOptions)
    val rawText = if (res.value != "--" && res.value.isNotBlank()) res.value else res.label
    val num = com.example.analizer.model.parseToDouble(rawText) ?: com.example.analizer.domain.StatisticsEngine().parseNumber(rawText)
    return Pair(num, rawText)
}

fun computeChartDataPoints(
    isLineChart: Boolean,
    config: AnalysisConfig,
    spreadsheetData: SpreadsheetData?,
    columnStats: List<ColumnStats>
): List<ChartPointData> {
    val cards = config.effectiveCards
    val displayGroups = if (config.displayGroups.isNotEmpty()) config.displayGroups else config.effectiveDisplayGroups

    val queryMode = if (isLineChart) config.lineChartQueryMode else config.barChartQueryMode
    val selectedCardIndex = if (isLineChart) config.lineChartSelectedCardIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0)) else config.barChartSelectedCardIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0))
    val selectedItemIndex = if (isLineChart) config.lineChartSelectedItemIndex else config.barChartSelectedItemIndex
    val customInterval = if (isLineChart) config.lineChartCustomInterval else config.barChartCustomInterval
    val itemsSubMode = if (isLineChart) config.lineChartItemsSubMode else config.barChartItemsSubMode

    val allowedValueIndices = listOf(4, 8, 12, 16, 6, 10, 14, 18)

    val list = when (queryMode) {
        "ITEMS" -> {
            if (cards.isEmpty()) emptyList()
            else if (itemsSubMode == "CARD_ITEMS") {
                val card = cards.getOrNull(selectedCardIndex) ?: cards.first()
                allowedValueIndices.mapNotNull { vIdx ->
                    val mapping = card.fieldMappings.find { it.fieldIndex == vIdx }
                    val isEnabled = mapping?.enabled ?: true
                    if (!isEnabled) null
                    else {
                        val (num, rawText) = getCardItemValue(card, vIdx, columnStats, displayGroups, spreadsheetData, config)
                        if (num != null) {
                            val title = resolveItemTitle(vIdx, card, columnStats, displayGroups, spreadsheetData, config)
                            ChartPointData(
                                label = title,
                                value = num,
                                formattedValue = rawText,
                                subtitle = "ITEM ${vIdx + 1}"
                            )
                        } else null
                    }
                }
            } else {
                val vIdx = if (selectedItemIndex in allowedValueIndices) selectedItemIndex else allowedValueIndices.first()
                cards.mapIndexedNotNull { cIdx, card ->
                    val (num, rawText) = getCardItemValue(card, vIdx, columnStats, displayGroups, spreadsheetData, config)
                    if (num != null) {
                        val group = displayGroups.find { it.id == card.groupId }
                        val cardName = resolveGroupName(group, spreadsheetData, config.sheetName, cIdx)
                        ChartPointData(
                            label = cardName,
                            value = num,
                            formattedValue = rawText,
                            subtitle = resolveItemTitle(vIdx, card, columnStats, displayGroups, spreadsheetData, config)
                        )
                    } else null
                }
            }
        }
        "GROUPS" -> {
            val vIdx = if (selectedItemIndex in allowedValueIndices) selectedItemIndex else allowedValueIndices.first()
            displayGroups.mapIndexedNotNull { gIdx, grp ->
                val grpCards = cards.filter { it.groupId == grp.id }
                if (grpCards.isEmpty()) null
                else {
                    val nums = grpCards.mapNotNull { getCardItemValue(it, vIdx, columnStats, displayGroups, spreadsheetData, config).first }
                    if (nums.isNotEmpty()) {
                        val avg = nums.average()
                        val grpName = resolveGroupName(grp, spreadsheetData, config.sheetName, gIdx)
                        ChartPointData(
                            label = grpName,
                            value = avg,
                            formattedValue = String.format(Locale.getDefault(), "%.2f", avg),
                            subtitle = "${grpCards.size} cards (Média)"
                        )
                    } else null
                }
            }
        }
        "CARDS" -> {
            val vIdx = if (selectedItemIndex in allowedValueIndices) selectedItemIndex else allowedValueIndices.first()
            cards.mapIndexedNotNull { cIdx, card ->
                val (num, rawText) = getCardItemValue(card, vIdx, columnStats, displayGroups, spreadsheetData, config)
                if (num != null) {
                    val group = displayGroups.find { it.id == card.groupId }
                    val cardName = resolveGroupName(group, spreadsheetData, config.sheetName, cIdx)
                    ChartPointData(
                        label = cardName,
                        value = num,
                        formattedValue = rawText,
                        subtitle = resolveItemTitle(vIdx, card, columnStats, displayGroups, spreadsheetData, config)
                    )
                } else null
            }
        }
        "INTERVAL" -> {
            val effInterval = customInterval.ifBlank { "B2:B12" }
            val cellRefs = parseCellRange(effInterval)
            val sheetRows = if (config.selectedFolder.isNotBlank()) {
                spreadsheetData?.getRows(config.selectedFolder, config.sheetName) ?: (spreadsheetData?.sheets?.get(config.sheetName) ?: emptyList())
            } else {
                spreadsheetData?.sheets?.get(config.sheetName) ?: emptyList()
            }
            val engine = com.example.analizer.domain.StatisticsEngine()
            cellRefs.mapNotNull { cellRef ->
                val text = engine.resolveCellText(sheetRows, cellRef)
                if (!text.isNullOrBlank()) {
                    val num = com.example.analizer.model.parseToDouble(text) ?: engine.parseNumber(text)
                    if (num != null) {
                        ChartPointData(
                            label = cellRef,
                            value = num,
                            formattedValue = text,
                            subtitle = config.sheetName
                        )
                    } else null
                } else null
            }
        }
        else -> emptyList()
    }
    return if (list.isNotEmpty()) list
    else {
        columnStats.map { stat ->
            ChartPointData(
                label = stat.columnName,
                value = if (stat.mean.isNaN()) 0.0 else stat.mean,
                formattedValue = String.format(Locale.getDefault(), "%.2f", stat.mean),
                subtitle = "Média da coluna"
            )
        }
    }
}

@Composable
fun ChartAnalysisCard(
    isLineChart: Boolean,
    columnStats: List<ColumnStats>,
    config: AnalysisConfig,
    spreadsheetData: SpreadsheetData?,
    activeTooltipIndex: Int?,
    onTooltipChanged: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (isLineChart) config.lineChartTitle.ifBlank { "Gráfico de Linhas" } else config.barChartTitle.ifBlank { "Gráfico de Barras" }
    val queryMode = if (isLineChart) config.lineChartQueryMode else config.barChartQueryMode
    val cards = config.effectiveCards
    val displayGroups = if (config.displayGroups.isNotEmpty()) config.displayGroups else config.effectiveDisplayGroups
    val selectedCardIndex = if (isLineChart) config.lineChartSelectedCardIndex else config.barChartSelectedCardIndex
    val selectedItemIndex = if (isLineChart) config.lineChartSelectedItemIndex else config.barChartSelectedItemIndex
    val customInterval = if (isLineChart) config.lineChartCustomInterval else config.barChartCustomInterval
    val itemsSubMode = if (isLineChart) config.lineChartItemsSubMode else config.barChartItemsSubMode

    val dataPoints = remember(isLineChart, config, spreadsheetData, columnStats) {
        computeChartDataPoints(isLineChart, config, spreadsheetData, columnStats)
    }

    val summaryMean = if (dataPoints.isNotEmpty()) dataPoints.map { it.value }.average() else 0.0
    val summaryTotal = if (dataPoints.isNotEmpty()) dataPoints.map { it.value }.sum() else 0.0
    val summaryMax = if (dataPoints.isNotEmpty()) dataPoints.maxOfOrNull { it.value } ?: 0.0 else 0.0
    val summaryMin = if (dataPoints.isNotEmpty()) dataPoints.minOfOrNull { it.value } ?: 0.0 else 0.0

    val fmtMean = String.format(Locale.getDefault(), "%.2f", summaryMean)
    val fmtTotal = String.format(Locale.getDefault(), "%.2f", summaryTotal)
    val fmtMax = String.format(Locale.getDefault(), "%.2f", summaryMax)
    val fmtMin = String.format(Locale.getDefault(), "%.2f", summaryMin)

    val modeBadge = when (queryMode) {
        "ITEMS" -> if (itemsSubMode == "CARD_ITEMS") "Itens do Card" else "Comparação de Cards"
        "GROUPS" -> "Comparação de Grupos"
        "CARDS" -> "Comparação de Cards"
        "INTERVAL" -> "Intervalo (${customInterval.ifBlank { "B2:B12" }})"
        else -> "Consulta"
    }

    val subtitle = when (queryMode) {
        "ITEMS" -> {
            if (itemsSubMode == "CARD_ITEMS") {
                val currentCard = cards.getOrNull(selectedCardIndex)
                val grp = displayGroups.find { it.id == currentCard?.groupId }
                val cardName = resolveGroupName(grp, spreadsheetData, config.sheetName, selectedCardIndex)
                "$cardName • ${dataPoints.size} itens"
            } else {
                val currentTitle = resolveItemTitle(selectedItemIndex, cards.firstOrNull(), columnStats, displayGroups, spreadsheetData, config)
                "Item: $currentTitle • ${dataPoints.size} cards"
            }
        }
        "GROUPS" -> {
            val currentTitle = resolveItemTitle(selectedItemIndex, cards.firstOrNull(), columnStats, displayGroups, spreadsheetData, config)
            "Item: $currentTitle • ${dataPoints.size} grupos"
        }
        "CARDS" -> {
            val currentTitle = resolveItemTitle(selectedItemIndex, cards.firstOrNull(), columnStats, displayGroups, spreadsheetData, config)
            "Item: $currentTitle • ${dataPoints.size} cards"
        }
        "INTERVAL" -> {
            val eff = customInterval.ifBlank { "B2:B12" }
            "Intervalo $eff • ${dataPoints.size} células"
        }
        else -> "${dataPoints.size} pontos de dados"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1825)),
        border = BorderStroke(1.dp, Color(0xFF1E283E))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Card layout identical to Analysis Card (pure visualization)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF152238), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLineChart) Icons.Rounded.ShowChart else Icons.Rounded.BarChart,
                            contentDescription = null,
                            tint = Color(0xFFEBA036),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Surface(
                    color = Color(0xFF152238),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E283E))
                ) {
                    Text(
                        text = modeBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEBA036),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1E283E))

            // Chart Canvas Section
            if (dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color(0xFF08101A), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum dado numérico encontrado para a consulta configurada.\nConfigure o gráfico na aba Configurações.",
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                val maxVal = dataPoints.maxOfOrNull { it.value }?.takeIf { it > 0.0 && !it.isNaN() } ?: 1.0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Color(0xFF08101A), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .pointerInput(dataPoints, isLineChart) {
                            detectTapGestures { offset ->
                                val count = dataPoints.size.coerceAtLeast(1)
                                if (isLineChart) {
                                    val stepX = if (count > 1) size.width / (count - 1) else size.width
                                    if (stepX > 0f) {
                                        val tapped = (offset.x / stepX).roundToInt().coerceIn(0, count - 1)
                                        onTooltipChanged(if (activeTooltipIndex == tapped) null else tapped)
                                    }
                                } else {
                                    val slotWidth = size.width / count
                                    if (slotWidth > 0f) {
                                        val tapped = (offset.x / slotWidth).toInt().coerceIn(0, count - 1)
                                        onTooltipChanged(if (activeTooltipIndex == tapped) null else tapped)
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val count = dataPoints.size.coerceAtLeast(1)

                        // Grid lines
                        val gridCount = 4
                        for (i in 0..gridCount) {
                            val y = size.height - 30f - (i.toFloat() / gridCount) * (size.height - 50f)
                            drawLine(
                                color = Color(0xFF152238),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }

                        if (!isLineChart) {
                            val totalWidth = size.width
                            val slotWidth = totalWidth / count
                            val barWidth = (slotWidth * 0.65f).coerceIn(10f, 44f)

                            dataPoints.forEachIndexed { index, dp ->
                                val safeVal = if (dp.value.isNaN() || dp.value.isInfinite()) 0.0 else dp.value.coerceAtLeast(0.0)
                                val barHeight = ((safeVal / maxVal) * (size.height - 55f)).toFloat().coerceIn(4f, size.height - 55f)
                                val centerX = index * slotWidth + slotWidth / 2f
                                val left = centerX - barWidth / 2f
                                val top = size.height - 30f - barHeight

                                val isSelected = activeTooltipIndex == index
                                val barBrush = if (isSelected) {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFFDE047), Color(0xFFEBA036)),
                                        startY = top,
                                        endY = size.height - 30f
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFEBA036), Color(0xFFE07A5F)),
                                        startY = top,
                                        endY = size.height - 30f
                                    )
                                }

                                val cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                                val path = Path().apply {
                                    addRoundRect(
                                        androidx.compose.ui.geometry.RoundRect(
                                            left = left,
                                            top = top,
                                            right = left + barWidth,
                                            bottom = size.height - 30f,
                                            topLeftCornerRadius = cornerRadius,
                                            topRightCornerRadius = cornerRadius,
                                            bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius.Zero,
                                            bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius.Zero
                                        )
                                    )
                                }
                                drawPath(path, brush = barBrush)
                            }
                        } else {
                            val stepX = if (count > 1) size.width / (count - 1) else size.width / 2f
                            val points = dataPoints.mapIndexed { index, dp ->
                                val safeVal = if (dp.value.isNaN() || dp.value.isInfinite()) 0.0 else dp.value.coerceAtLeast(0.0)
                                val x = if (count > 1) index * stepX else size.width / 2f
                                val y = (size.height - 30f - ((safeVal / maxVal) * (size.height - 55f)).toFloat()).coerceIn(15f, size.height - 30f)
                                Offset(x, y)
                            }

                            if (points.isNotEmpty()) {
                                val fillPath = Path().apply {
                                    moveTo(points.first().x, size.height - 30f)
                                    points.forEach { lineTo(it.x, it.y) }
                                    lineTo(points.last().x, size.height - 30f)
                                    close()
                                }
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFFEBA036).copy(alpha = 0.35f), Color(0xFFEBA036).copy(alpha = 0.02f)),
                                        startY = 15f,
                                        endY = size.height - 30f
                                    )
                                )

                                val strokePath = Path().apply {
                                    moveTo(points.first().x, points.first().y)
                                    for (i in 1 until points.size) {
                                        lineTo(points[i].x, points[i].y)
                                    }
                                }
                                drawPath(
                                    path = strokePath,
                                    color = Color(0xFFEBA036),
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )

                                points.forEachIndexed { index, pt ->
                                    val isSelected = activeTooltipIndex == index
                                    if (isSelected) {
                                        drawCircle(
                                            color = Color(0xFFFDE047).copy(alpha = 0.4f),
                                            radius = 12.dp.toPx(),
                                            center = pt
                                        )
                                    }
                                    drawCircle(
                                        color = if (isSelected) Color(0xFFFDE047) else Color(0xFFEBA036),
                                        radius = 5.dp.toPx(),
                                        center = pt
                                    )
                                    drawCircle(
                                        color = Color(0xFF0B1825),
                                        radius = 2.5.dp.toPx(),
                                        center = pt
                                    )
                                }
                            }
                        }
                    }

                    // Bottom labels under bars/points
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dataPoints.forEachIndexed { idx, dp ->
                            Text(
                                text = dp.label.take(8),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = if (activeTooltipIndex == idx) Color(0xFFEBA036) else Color(0xFF64748B),
                                fontWeight = if (activeTooltipIndex == idx) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Interactive Tooltip on Tap
                if (activeTooltipIndex != null && activeTooltipIndex in dataPoints.indices) {
                    val point = dataPoints[activeTooltipIndex]
                    Surface(
                        color = Color(0xFF152238),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEBA036)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = point.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (point.subtitle.isNotBlank()) {
                                    Text(
                                        text = point.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = point.formattedValue,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEBA036)
                                )
                                val pct = if (summaryTotal > 0.0) (point.value / summaryTotal) * 100 else 0.0
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%% do Total", pct),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = if (isLineChart) "Toque em um ponto para detalhes." else "Toque em uma barra para detalhes.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1E283E))

            // Bottom Metrics Row: Replicating AnalysisStatCard Summary Body
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    StatDetailItem("Média", fmtMean)
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    StatDetailItem("Total", fmtTotal)
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    StatDetailItem("Máximo", fmtMax)
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    StatDetailItem("Mínimo", fmtMin)
                }
            }
        }
    }
}

@Composable
fun BarChartTab(
    columnStats: List<ColumnStats>,
    config: AnalysisConfig,
    spreadsheetData: SpreadsheetData? = null,
    onConfigChanged: (AnalysisConfig) -> Unit = {},
    activeTooltipIndex: Int? = null,
    onTooltipChanged: (Int?) -> Unit = {}
) {
    ChartAnalysisCard(
        isLineChart = false,
        columnStats = columnStats,
        config = config,
        spreadsheetData = spreadsheetData,
        activeTooltipIndex = activeTooltipIndex,
        onTooltipChanged = onTooltipChanged
    )
}

@Composable
fun LineChartTab(
    columnStats: List<ColumnStats>,
    config: AnalysisConfig,
    spreadsheetData: SpreadsheetData? = null,
    onConfigChanged: (AnalysisConfig) -> Unit = {},
    activeTooltipIndex: Int? = null,
    onTooltipChanged: (Int?) -> Unit = {}
) {
    ChartAnalysisCard(
        isLineChart = true,
        columnStats = columnStats,
        config = config,
        spreadsheetData = spreadsheetData,
        activeTooltipIndex = activeTooltipIndex,
        onTooltipChanged = onTooltipChanged
    )
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
