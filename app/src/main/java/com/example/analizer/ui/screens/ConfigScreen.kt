package com.example.analizer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analizer.Screen
import com.example.analizer.domain.StatisticsEngine
import com.example.analizer.model.AnalysisConfig
import com.example.analizer.model.CardFieldMapping
import com.example.analizer.model.DisplayGroup
import com.example.analizer.model.DisplayGroupItem
import com.example.analizer.model.METRIC_OPTIONS
import com.example.analizer.model.SpreadsheetData
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    spreadsheetData: SpreadsheetData?,
    config: AnalysisConfig,
    columnStats: List<com.example.analizer.model.ColumnStats> = emptyList(),
    onConfigChanged: (AnalysisConfig) -> Unit,
    onRunAnalysis: () -> Unit,
    onExportConfig: () -> Unit = {},
    onImportConfig: () -> Unit = {},
    onReloadData: () -> Unit = {},
    onLoadPreset: () -> Unit = {},
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    if (spreadsheetData == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Configuração", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { onScreenSelected(Screen.Import) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        IconButton(onClick = onLoadPreset) {
                            Icon(Icons.Rounded.DashboardCustomize, contentDescription = "Carregar Modelo Mensal", tint = Color(0xFFEBA036))
                        }
                        IconButton(onClick = onExportConfig) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = "Exportar Configurações", tint = Color(0xFFEBA036))
                        }
                        IconButton(onClick = onImportConfig) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = "Importar Configurações", tint = Color(0xFFEBA036))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF0D1525),
                        titleContentColor = androidx.compose.ui.graphics.Color.White,
                        navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF0D1525)
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = androidx.compose.ui.graphics.Color(0xFF0D1525),
                        selectedTextColor = androidx.compose.ui.graphics.Color(0xFFEBA036),
                        indicatorColor = androidx.compose.ui.graphics.Color(0xFFEBA036),
                        unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF8A99B5),
                        unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF8A99B5)
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
            containerColor = androidx.compose.ui.graphics.Color(0xFFF5E8C7)
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

    val displayGroups = if (config.displayGroups.isNotEmpty()) {
        config.displayGroups
    } else {
        config.effectiveDisplayGroups
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuração de Grupos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onScreenSelected(Screen.Import) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onLoadPreset) {
                        Icon(Icons.Rounded.DashboardCustomize, contentDescription = "Carregar Modelo Mensal", tint = Color(0xFFEBA036))
                    }
                    IconButton(onClick = onExportConfig) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = "Exportar Configurações", tint = Color(0xFFEBA036))
                    }
                    IconButton(onClick = onImportConfig) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = "Importar Configurações", tint = Color(0xFFEBA036))
                    }
                    IconButton(onClick = onReloadData) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Recarregar Dados", tint = Color(0xFFEBA036))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF0D1525),
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White,
                    actionIconContentColor = androidx.compose.ui.graphics.Color(0xFFEBA036)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color(0xFF0D1525)
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = androidx.compose.ui.graphics.Color(0xFF0D1525),
                    selectedTextColor = androidx.compose.ui.graphics.Color(0xFFEBA036),
                    indicatorColor = androidx.compose.ui.graphics.Color(0xFFEBA036),
                    unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF8A99B5),
                    unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF8A99B5)
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
        containerColor = androidx.compose.ui.graphics.Color(0xFFF5E8C7)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
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
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Configuração de Grupos", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = Color(0xFF0D1525),
                    unselectedContentColor = Color(0xFF788497)
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Configuração de Cards", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = Color(0xFF0D1525),
                    unselectedContentColor = Color(0xFF788497)
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        // Tab 0: General settings, display groups, positioning, charts
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Display Groups Container Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF0B1825)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "Grupos de Exibição",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEBA036)
                                    )
                                    Text(
                                        text = "Cada grupo contém a pasta, aba da planilha, nome do grupo e seus itens que compõem os cards.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD0DCE5)
                                    )

                                    if (displayGroups.isEmpty()) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFF02121C)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Nenhum grupo configurado.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color(0xFFD0DCE5)
                                                )
                                            }
                                        }
                                    } else {
                                        displayGroups.forEachIndexed { index, group ->
                                            DisplayGroupCard(
                                                group = group,
                                                groupIndex = index,
                                                spreadsheetData = spreadsheetData,
                                                sheets = spreadsheetData?.sheets ?: emptyMap(),
                                                sheetNames = sheetNames,
                                                currentGlobalSheet = currentSheetName,
                                                headerRowIndex = headerRowIndex,
                                                customColumnTitles = config.customColumnTitles,
                                                globalStartRow = config.startRow,
                                                globalEndRow = config.endRow,
                                                maxRowIndex = maxRowIndex,
                                                onUpdateGroup = { updatedGroup ->
                                                    val updatedList = displayGroups.toMutableList().apply {
                                                        set(index, updatedGroup)
                                                    }
                                                    onConfigChanged(config.copy(displayGroups = updatedList))
                                                },
                                                onDeleteGroup = {
                                                    val updatedList = displayGroups.toMutableList().apply {
                                                        removeAt(index)
                                                    }
                                                    onConfigChanged(config.copy(displayGroups = updatedList))
                                                }
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val newGroup = DisplayGroup(
                                                name = "Grupo ${displayGroups.size + 1}",
                                                selectedSheets = listOf(currentSheetName),
                                                displayOrder = displayGroups.size,
                                                items = listOf(
                                                    DisplayGroupItem(
                                                        startColumn = 0,
                                                        endColumn = (totalCols - 1).coerceAtLeast(0),
                                                        startRow = null,
                                                        endRow = null,
                                                        metric = "Média"
                                                    )
                                                )
                                            )
                                            onConfigChanged(config.copy(displayGroups = displayGroups + newGroup))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF02121C),
                                            contentColor = Color(0xFFEBA036)
                                        )
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFFEBA036))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Adicionar Grupo de Exibição", color = Color(0xFFEBA036), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // Group Ordering & Selection Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF0B1825)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "Posicionamento e Seleção para Análise",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEBA036)
                                    )

                                    displayGroups.forEachIndexed { index, group ->
                                        val groupName = when (group.nameSourceType) {
                                            "CELL" -> "Célula ${group.cellReference}"
                                            "SHEET", "SHEET_NAME", "ABANOME" -> group.selectedSheets.firstOrNull() ?: currentSheetName
                                            else -> group.name.ifBlank { "Grupo ${index + 1}" }
                                        }
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFF02121C)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Checkbox(
                                                        checked = group.enabledForAnalysis,
                                                        onCheckedChange = { checked ->
                                                            val updated = displayGroups.mapIndexed { idx, g ->
                                                                if (idx == index) g.copy(enabledForAnalysis = checked) else g
                                                            }
                                                            onConfigChanged(config.copy(displayGroups = updated))
                                                        },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = Color(0xFFEBA036),
                                                            uncheckedColor = Color(0xFF788497)
                                                        )
                                                    )
                                                    Text(
                                                        text = "${index + 1}. $groupName",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            if (index > 0) {
                                                                val mutable = displayGroups.toMutableList()
                                                                val item = mutable.removeAt(index)
                                                                mutable.add(index - 1, item)
                                                                val reordered = mutable.mapIndexed { newIdx, g -> g.copy(displayOrder = newIdx) }
                                                                onConfigChanged(config.copy(displayGroups = reordered))
                                                            }
                                                        },
                                                        enabled = index > 0
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.ArrowUpward,
                                                            contentDescription = "Subir",
                                                            tint = if (index > 0) Color.White else Color(0xFF556882)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            if (index < displayGroups.size - 1) {
                                                                val mutable = displayGroups.toMutableList()
                                                                val item = mutable.removeAt(index)
                                                                mutable.add(index + 1, item)
                                                                val reordered = mutable.mapIndexed { newIdx, g -> g.copy(displayOrder = newIdx) }
                                                                onConfigChanged(config.copy(displayGroups = reordered))
                                                            }
                                                        },
                                                        enabled = index < displayGroups.size - 1
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.ArrowDownward,
                                                            contentDescription = "Descer",
                                                            tint = if (index < displayGroups.size - 1) Color.White else Color(0xFF556882)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Chart Group Comparison Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF0B1825)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "Seleção de Grupos para Gráficos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEBA036)
                                    )

                                    displayGroups.forEachIndexed { index, group ->
                                        val groupName = when (group.nameSourceType) {
                                            "CELL" -> "Célula ${group.cellReference}"
                                            "SHEET", "SHEET_NAME", "ABANOME" -> group.selectedSheets.firstOrNull() ?: currentSheetName
                                            else -> group.name.ifBlank { "Grupo ${index + 1}" }
                                        }
                                        val isSelected = config.chartSelectedGroupIds.contains(groupName) || config.chartSelectedGroupIds.contains(group.id)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val currentSelected = config.chartSelectedGroupIds
                                                    val newSelected = if (isSelected) {
                                                        currentSelected.filter { it != groupName && it != group.id }
                                                    } else {
                                                        currentSelected + groupName
                                                    }
                                                    onConfigChanged(config.copy(chartSelectedGroupIds = newSelected))
                                                },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    val currentSelected = config.chartSelectedGroupIds
                                                    val newSelected = if (checked) {
                                                        currentSelected + groupName
                                                    } else {
                                                        currentSelected.filter { it != groupName && it != group.id }
                                                    }
                                                    onConfigChanged(config.copy(chartSelectedGroupIds = newSelected))
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFFEBA036),
                                                    uncheckedColor = Color(0xFF788497)
                                                )
                                            )
                                            Text(text = groupName, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                        }
                                    }
                                }
                            }

                            // Run Analysis Button
                            Button(
                                onClick = {
                                    onRunAnalysis()
                                    onScreenSelected(Screen.Analytics)
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0B1825),
                                    contentColor = Color(0xFFEBA036)
                                )
                            ) {
                                Icon(Icons.Rounded.Analytics, contentDescription = null, tint = Color(0xFFEBA036))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Executar Análise e Ver Resultados", fontWeight = FontWeight.Bold, color = Color(0xFFEBA036))
                            }
                        }
                    }
                    1 -> {
                        // Tab 1: Card Configuration with option to create/edit/delete cards, each with its viewer and 14 fields (2 per row)
                        val cards = config.effectiveCards

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Configuração e Visualização de Cards",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0B1825)
                                    )
                                    Text(
                                        text = "Crie, edite ou remova cards. Cada card tem 14 campos (2 por linha) vinculados aos itens do grupo selecionado.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Button(
                                    onClick = {
                                        val newCard = com.example.analizer.model.CardConfig(
                                            groupId = displayGroups.firstOrNull()?.id ?: ""
                                        )
                                        onConfigChanged(config.copy(cards = cards + newCard))
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0B1825),
                                        contentColor = Color(0xFFEBA036)
                                    )
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFFEBA036))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Adicionar Card", color = Color(0xFFEBA036), fontWeight = FontWeight.Bold)
                                }
                            }

                            if (cards.isEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF0B1825)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Nenhum card configurado. Adicione um card acima.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFD0DCE5)
                                        )
                                    }
                                }
                            } else {
                                cards.forEachIndexed { cardIdx, cardConfig ->
                                    val linkedGroup = displayGroups.find { it.id == cardConfig.groupId || it.name == cardConfig.groupId }
                                    val cardTitle = resolveGroupName(linkedGroup, spreadsheetData, currentSheetName, cardIdx)

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF0B1825)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            // Card Header with Duplicate and Delete buttons
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Card ${cardIdx + 1}: $cardTitle",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEBA036),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            val duplicatedCard = cardConfig.copy(
                                                                id = java.util.UUID.randomUUID().toString(),
                                                                fieldMappings = cardConfig.fieldMappings.map {
                                                                    it.copy(id = java.util.UUID.randomUUID().toString())
                                                                }
                                                            )
                                                            val updatedCards = cards.toMutableList()
                                                            updatedCards.add(cardIdx + 1, duplicatedCard)
                                                            onConfigChanged(config.copy(cards = updatedCards))
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF02121C),
                                                            contentColor = Color(0xFFEBA036)
                                                        ),
                                                        modifier = Modifier
                                                            .height(34.dp)
                                                            .testTag("duplicate_card_$cardIdx")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.ContentCopy,
                                                            contentDescription = "Duplicar Card",
                                                            tint = Color(0xFFEBA036),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Duplicar", fontSize = 12.sp, color = Color(0xFFEBA036), fontWeight = FontWeight.SemiBold)
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val updatedCards = cards.toMutableList().apply { removeAt(cardIdx) }
                                                            onConfigChanged(config.copy(cards = updatedCards))
                                                        },
                                                        modifier = Modifier.testTag("delete_card_$cardIdx")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Delete,
                                                            contentDescription = "Excluir Card",
                                                            tint = Color(0xFFFF6B6B)
                                                        )
                                                    }
                                                }
                                            }

                                            // Live Card Preview Viewer
                                            AnalysisStatCard(
                                                cardConfig = cardConfig,
                                                columnStats = columnStats,
                                                displayGroups = displayGroups,
                                                spreadsheetData = spreadsheetData,
                                                currentSheetName = currentSheetName,
                                                cardIndex = cardIdx
                                            )

                                            HorizontalDivider(color = Color(0xFF2C3E55))

                                            // Card Group Selection (Card Title Source)
                                            var cardGroupExpanded by remember { mutableStateOf(false) }
                                            ExposedDropdownMenuBox(
                                                expanded = cardGroupExpanded,
                                                onExpandedChange = { cardGroupExpanded = !cardGroupExpanded },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                OutlinedTextField(
                                                    value = cardTitle,
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Grupo Selecionado (Título do Card)") },
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardGroupExpanded) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        
                                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                                    shape = RoundedCornerShape(8.dp),
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                                                    colors = darkTextFieldColors()
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = cardGroupExpanded,
                                                    onDismissRequest = { cardGroupExpanded = false },
                                                    modifier = Modifier.background(Color(0xFF02121C))
                                                ) {
                                                    displayGroups.forEach { grp ->
                                                        val gName = resolveGroupName(grp, spreadsheetData, currentSheetName)
                                                        DropdownMenuItem(
                                                            text = { Text(gName, color = Color.White) },
                                                            colors = MenuDefaults.itemColors(textColor = Color.White),
                                                            onClick = {
                                                                cardGroupExpanded = false
                                                                val updatedCards = cards.toMutableList()
                                                                updatedCards[cardIdx] = cardConfig.copy(groupId = grp.id)
                                                                onConfigChanged(config.copy(cards = updatedCards))
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            // Edit 14 Fields of this card (2 per row)
                                            Text(
                                                text = "Editar 14 Campos do Card (2 por linha)",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEBA036)
                                            )

                                            for (i in 0 until 14 step 2) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        CardFieldConfigCard(
                                                            fieldIndex = i,
                                                            cardConfig = cardConfig,
                                                            cardIdx = cardIdx,
                                                            cards = cards,
                                                            displayGroups = displayGroups,
                                                            linkedGroup = linkedGroup,
                                                            config = config,
                                                            onConfigChanged = onConfigChanged
                                                        )
                                                    }
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        if (i + 1 < 14) {
                                                            CardFieldConfigCard(
                                                                fieldIndex = i + 1,
                                                                cardConfig = cardConfig,
                                                                cardIdx = cardIdx,
                                                                cards = cards,
                                                                displayGroups = displayGroups,
                                                                linkedGroup = linkedGroup,
                                                                config = config,
                                                                onConfigChanged = onConfigChanged
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF02121C),
    unfocusedContainerColor = Color(0xFF02121C),
    focusedLabelColor = Color(0xFFEBA036),
    unfocusedLabelColor = Color(0xFFB0BEC5),
    focusedBorderColor = Color(0xFFEBA036),
    unfocusedBorderColor = Color(0xFF4A5D78),
    focusedTrailingIconColor = Color(0xFFEBA036),
    unfocusedTrailingIconColor = Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayGroupCard(
    group: DisplayGroup,
    groupIndex: Int,
    spreadsheetData: SpreadsheetData?,
    sheets: Map<String, List<List<String>>>,
    sheetNames: List<String>,
    currentGlobalSheet: String,
    headerRowIndex: Int,
    customColumnTitles: Map<Int, String>,
    globalStartRow: Int?,
    globalEndRow: Int?,
    maxRowIndex: Int,
    onUpdateGroup: (DisplayGroup) -> Unit,
    onDeleteGroup: () -> Unit
) {
    val folderNames = spreadsheetData?.folderNames ?: emptyList()
    val currentFolder = group.selectedFolder.ifBlank { folderNames.firstOrNull() ?: "" }
    val folderSheetNames = if (currentFolder.isNotBlank()) spreadsheetData?.getSheetNamesForFolder(currentFolder) ?: sheetNames else sheetNames
    val effectiveSheet = group.selectedSheets.firstOrNull() ?: folderSheetNames.firstOrNull() ?: currentGlobalSheet
    val sheetRows = if (currentFolder.isNotBlank()) {
        spreadsheetData?.getRows(currentFolder, effectiveSheet) ?: (sheets[effectiveSheet] ?: emptyList())
    } else {
        sheets[effectiveSheet] ?: emptyList()
    }
    val safeHeaderRowIndex = headerRowIndex.coerceIn(0, maxOf(0, maxRowIndex))
    val headerRow = sheetRows.getOrNull(safeHeaderRowIndex) ?: emptyList()
    val totalCols = headerRow.size.takeIf { it > 0 } ?: (sheetRows.maxOfOrNull { it.size } ?: 0)

    val resolvedName = when (group.nameSourceType) {
        "CELL" -> StatisticsEngine().resolveCellText(sheetRows, group.cellReference) ?: "(Célula vazia/inválida)"
        "SHEET", "SHEET_NAME", "ABANOME" -> effectiveSheet
        else -> group.name.ifBlank { if (currentFolder.isNotBlank()) currentFolder else "Grupo ${groupIndex + 1}" }
    }

    var nameTypeExpanded by remember { mutableStateOf(false) }
    val nameTypeLabel = when (group.nameSourceType) {
        "CELL" -> "Célula"
        "SHEET", "SHEET_NAME", "ABANOME" -> "Aba"
        else -> "Texto"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF02121C)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                        tint = Color(0xFFEBA036)
                    )
                    Text(
                        text = resolvedName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDeleteGroup) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Remover Grupo",
                        tint = Color(0xFFFF6B6B)
                    )
                }
            }

            // a - um dropdown, para escolher entre as pastas encontradas, que contem planilhas encontradas.
            // b - ao lado, um dropdown exibindo as abas da planilha dentro da pasta escolhida.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown Pasta Mãe
                Box(modifier = Modifier.weight(1f)) {
                    var folderExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = folderExpanded,
                        onExpandedChange = { folderExpanded = !folderExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (folderNames.isNotEmpty()) currentFolder else "Pasta Principal",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pasta Mãe", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                            colors = darkTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = folderExpanded,
                            onDismissRequest = { folderExpanded = false },
                            modifier = Modifier.background(Color(0xFF02121C))
                        ) {
                            val displayFolders = if (folderNames.isNotEmpty()) folderNames else listOf("Pasta Principal")
                            displayFolders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder, color = Color.White) },
                                    colors = MenuDefaults.itemColors(textColor = Color.White),
                                    onClick = {
                                        folderExpanded = false
                                        val fSheets = spreadsheetData?.getSheetNamesForFolder(folder) ?: sheetNames
                                        val newSheet = fSheets.firstOrNull() ?: ""
                                        onUpdateGroup(
                                            group.copy(
                                                selectedFolder = folder,
                                                selectedSheets = if (newSheet.isNotBlank()) listOf(newSheet) else emptyList(),
                                                name = if (group.nameSourceType == "TEXT" && (group.name.isBlank() || group.name.startsWith("Grupo ") || folderNames.contains(group.name))) folder else group.name
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Dropdown Aba da Planilha
                Box(modifier = Modifier.weight(1f)) {
                    var sheetExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = sheetExpanded,
                        onExpandedChange = { sheetExpanded = !sheetExpanded }
                    ) {
                        OutlinedTextField(
                            value = effectiveSheet.ifBlank { "Aba 1" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Aba da Planilha", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sheetExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                            colors = darkTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = sheetExpanded,
                            onDismissRequest = { sheetExpanded = false },
                            modifier = Modifier.background(Color(0xFF02121C))
                        ) {
                            val displaySheets = if (folderSheetNames.isNotEmpty()) folderSheetNames else listOf("Aba 1")
                            displaySheets.forEach { sheet ->
                                DropdownMenuItem(
                                    text = { Text(sheet, color = Color.White) },
                                    colors = MenuDefaults.itemColors(textColor = Color.White),
                                    onClick = {
                                        sheetExpanded = false
                                        onUpdateGroup(group.copy(selectedSheets = listOf(sheet)))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // c - abaixo exibirá as opcoes de nome do grupo com as mesmas opcoes que ja existe.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = nameTypeExpanded,
                    onExpandedChange = { nameTypeExpanded = !nameTypeExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = nameTypeLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Origem do Nome", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                        colors = darkTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = nameTypeExpanded,
                        onDismissRequest = { nameTypeExpanded = false },
                        modifier = Modifier.background(Color(0xFF02121C))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Texto", color = Color.White) },
                            colors = MenuDefaults.itemColors(textColor = Color.White),
                            onClick = {
                                nameTypeExpanded = false
                                onUpdateGroup(group.copy(nameSourceType = "TEXT"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Célula", color = Color.White) },
                            colors = MenuDefaults.itemColors(textColor = Color.White),
                            onClick = {
                                nameTypeExpanded = false
                                onUpdateGroup(group.copy(nameSourceType = "CELL"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Aba", color = Color.White) },
                            colors = MenuDefaults.itemColors(textColor = Color.White),
                            onClick = {
                                nameTypeExpanded = false
                                onUpdateGroup(group.copy(nameSourceType = "SHEET"))
                            }
                        )
                    }
                }

                if (group.nameSourceType == "CELL") {
                    OutlinedTextField(
                        value = group.cellReference,
                        onValueChange = { onUpdateGroup(group.copy(cellReference = it)) },
                        label = { Text("Ref. Célula", fontSize = 11.sp) },
                        placeholder = { Text("ex: B2", color = Color(0xFF788497)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                        colors = darkTextFieldColors()
                    )
                } else if (group.nameSourceType == "TEXT") {
                    OutlinedTextField(
                        value = group.name,
                        onValueChange = { onUpdateGroup(group.copy(name = it)) },
                        label = { Text("Nome do Grupo", fontSize = 11.sp) },
                        placeholder = { Text("ex: Resumo", color = Color(0xFF788497)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                        colors = darkTextFieldColors()
                    )
                } else {
                    OutlinedTextField(
                        value = effectiveSheet,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Aba Utilizada", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                        colors = darkTextFieldColors()
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF2C3E55))

            // d - Abaixo as configuracoes de itens, nelas quero poder adicionar varios itens no mesmo grupo, pois estes itens que irao compor os cards.
            Text(
                text = "Itens do Grupo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEBA036)
            )

            group.items.forEachIndexed { itemIdx, item ->
                DisplayGroupItemCard(
                    item = item,
                    totalCols = totalCols,
                    maxRowIndex = maxRowIndex,
                    headerRow = headerRow,
                    customColumnTitles = customColumnTitles,
                    globalStartRow = globalStartRow,
                    globalEndRow = globalEndRow,
                    onUpdateItem = { updatedItem ->
                        val updatedItems = group.items.toMutableList().apply {
                            set(itemIdx, updatedItem)
                        }
                        onUpdateGroup(group.copy(items = updatedItems))
                    },
                    onDeleteItem = {
                        if (group.items.size > 1) {
                            val updatedItems = group.items.toMutableList().apply {
                                removeAt(itemIdx)
                            }
                            onUpdateGroup(group.copy(items = updatedItems))
                        }
                    },
                    canDelete = group.items.size > 1
                )
            }

            OutlinedButton(
                onClick = {
                    val newItem = DisplayGroupItem(
                        startColumn = 0,
                        endColumn = (totalCols - 1).coerceAtLeast(0),
                        startRow = null,
                        endRow = null,
                        metric = "Média"
                    )
                    onUpdateGroup(group.copy(items = group.items + newItem))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEBA036)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEBA036))
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFFEBA036))
                Spacer(modifier = Modifier.width(8.dp))
                Text("+ Adicionar Item ao Grupo", color = Color(0xFFEBA036))
            }
        }
    }
}

@Composable
fun DisplayGroupItemCard(
    item: DisplayGroupItem,
    totalCols: Int,
    maxRowIndex: Int,
    headerRow: List<String>,
    customColumnTitles: Map<Int, String>,
    globalStartRow: Int?,
    globalEndRow: Int?,
    onUpdateItem: (DisplayGroupItem) -> Unit,
    onDeleteItem: () -> Unit,
    canDelete: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF02121C)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = item.customLabel,
                    onValueChange = { onUpdateItem(item.copy(customLabel = it)) },
                    label = { Text("Rótulo do Item (Opcional)") },
                    placeholder = { Text("ex: Vendas Nacionais", color = Color(0xFF788497)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = Color.White),
                    colors = darkTextFieldColors()
                )
                if (canDelete) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDeleteItem) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remover Item",
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColumnDropdown(
                    label = "Coluna Início",
                    selectedColumn = item.startColumn,
                    totalCols = totalCols,
                    headerRow = headerRow,
                    customColumnTitles = customColumnTitles,
                    onColumnSelected = { selected ->
                        val newEnd = if (selected > item.endColumn) selected else item.endColumn
                        onUpdateItem(item.copy(startColumn = selected, endColumn = newEnd))
                    },
                    modifier = Modifier.weight(1f)
                )

                ColumnDropdown(
                    label = "Coluna Fim",
                    selectedColumn = item.endColumn,
                    totalCols = totalCols,
                    headerRow = headerRow,
                    customColumnTitles = customColumnTitles,
                    onColumnSelected = { selected ->
                        val newStart = if (selected < item.startColumn) selected else item.startColumn
                        onUpdateItem(item.copy(startColumn = newStart, endColumn = selected))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RowInputField(
                    label = "Linha Início",
                    selectedRow = item.startRow,
                    onRowSelected = { row ->
                        onUpdateItem(item.copy(startRow = row))
                    },
                    modifier = Modifier.weight(1f)
                )

                RowInputField(
                    label = "Linha Fim",
                    selectedRow = item.endRow,
                    onRowSelected = { row ->
                        onUpdateItem(item.copy(endRow = row))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            ColumnMetricDropdown(
                currentMetric = item.metric,
                onMetricSelected = { newMetric ->
                    onUpdateItem(item.copy(metric = newMetric))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RowInputField(
    label: String,
    selectedRow: Int?,
    onRowSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(selectedRow) {
        mutableStateOf(if (selectedRow != null) "${selectedRow + 1}" else "")
    }

    OutlinedTextField(
        value = textValue,
        onValueChange = { input ->
            val clean = input.filter { it.isDigit() }
            textValue = clean
            if (clean.isBlank()) {
                onRowSelected(null)
            } else {
                val rowNumber = clean.toIntOrNull()
                if (rowNumber != null && rowNumber > 0) {
                    onRowSelected(rowNumber - 1)
                } else if (rowNumber == 0) {
                    onRowSelected(0)
                }
            }
        },
        label = { Text(label, fontSize = 11.sp) },
        placeholder = { Text("Livre", color = Color(0xFF788497), fontSize = 11.sp) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        ),
        shape = RoundedCornerShape(8.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = Color.White),
        colors = darkTextFieldColors()
    )
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
        return if (title != null) "$excelName - $title" else excelName
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
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = Color.White),
            colors = darkTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF02121C))
        ) {
            for (colIdx in 0 until totalCols) {
                DropdownMenuItem(
                    text = { Text(getColumnLabel(colIdx), color = Color.White) },
                    colors = MenuDefaults.itemColors(textColor = Color.White),
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
            label = { Text("Métrica / Fórmula") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = Color.White),
            colors = darkTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF02121C))
        ) {
            METRIC_OPTIONS.forEach { metric ->
                DropdownMenuItem(
                    text = { Text(metric, color = Color.White) },
                    colors = MenuDefaults.itemColors(textColor = Color.White),
                    onClick = {
                        expanded = false
                        onMetricSelected(metric)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardFieldConfigCard(
    fieldIndex: Int,
    cardConfig: com.example.analizer.model.CardConfig,
    cardIdx: Int,
    cards: List<com.example.analizer.model.CardConfig>,
    displayGroups: List<DisplayGroup>,
    linkedGroup: DisplayGroup?,
    config: AnalysisConfig,
    onConfigChanged: (AnalysisConfig) -> Unit
) {
    val defaultTitle = DEFAULT_CARD_DETAIL_TITLES.getOrElse(fieldIndex) { "Campo ${fieldIndex + 1}" }
    val mapping = cardConfig.fieldMappings.find { it.fieldIndex == fieldIndex }
    val titleRef = mapping?.titleItemReference?.ifBlank { defaultTitle } ?: defaultTitle
    val valueRef = mapping?.valueItemReference ?: ""

    val groupItems = linkedGroup?.items ?: emptyList()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF02121C)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Campo ${fieldIndex + 1} ($defaultTitle)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEBA036)
            )

            // Title selection / input
            var titleExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = titleExpanded,
                onExpandedChange = { titleExpanded = !titleExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = titleRef,
                    onValueChange = { newTitle ->
                        val updatedMappings = cardConfig.fieldMappings.toMutableList()
                        val existingIdx = updatedMappings.indexOfFirst { it.fieldIndex == fieldIndex }
                        if (existingIdx >= 0) {
                            updatedMappings[existingIdx] = updatedMappings[existingIdx].copy(titleItemReference = newTitle)
                        } else {
                            updatedMappings.add(com.example.analizer.model.CardFieldMapping(fieldIndex = fieldIndex, titleItemReference = newTitle, valueItemReference = valueRef))
                        }
                        val updatedCards = cards.toMutableList()
                        updatedCards[cardIdx] = cardConfig.copy(fieldMappings = updatedMappings)
                        onConfigChanged(config.copy(cards = updatedCards))
                    },
                    label = { Text("Título") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = titleExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                    colors = darkTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = titleExpanded,
                    onDismissRequest = { titleExpanded = false },
                    modifier = Modifier.background(Color(0xFF02121C))
                ) {
                    DropdownMenuItem(
                        text = { Text("Padrão: $defaultTitle", color = Color.White) },
                        colors = MenuDefaults.itemColors(textColor = Color.White),
                        onClick = {
                            titleExpanded = false
                            val updatedMappings = cardConfig.fieldMappings.toMutableList()
                            val existingIdx = updatedMappings.indexOfFirst { it.fieldIndex == fieldIndex }
                            if (existingIdx >= 0) {
                                updatedMappings[existingIdx] = updatedMappings[existingIdx].copy(titleItemReference = defaultTitle)
                            } else {
                                updatedMappings.add(com.example.analizer.model.CardFieldMapping(fieldIndex = fieldIndex, titleItemReference = defaultTitle, valueItemReference = valueRef))
                            }
                            val updatedCards = cards.toMutableList()
                            updatedCards[cardIdx] = cardConfig.copy(fieldMappings = updatedMappings)
                            onConfigChanged(config.copy(cards = updatedCards))
                        }
                    )
                    groupItems.forEachIndexed { itemIdx, item ->
                        val itemLabel = item.customLabel.ifBlank { "Item ${itemIdx + 1}" }
                        DropdownMenuItem(
                            text = { Text(itemLabel, color = Color.White) },
                            colors = MenuDefaults.itemColors(textColor = Color.White),
                            onClick = {
                                titleExpanded = false
                                val updatedMappings = cardConfig.fieldMappings.toMutableList()
                                val existingIdx = updatedMappings.indexOfFirst { it.fieldIndex == fieldIndex }
                                if (existingIdx >= 0) {
                                    updatedMappings[existingIdx] = updatedMappings[existingIdx].copy(titleItemReference = itemLabel)
                                } else {
                                    updatedMappings.add(com.example.analizer.model.CardFieldMapping(fieldIndex = fieldIndex, titleItemReference = itemLabel, valueItemReference = valueRef))
                                }
                                val updatedCards = cards.toMutableList()
                                updatedCards[cardIdx] = cardConfig.copy(fieldMappings = updatedMappings)
                                onConfigChanged(config.copy(cards = updatedCards))
                            }
                        )
                    }
                }
            }

            // Value selection dropdown (Item from group)
            var valueExpanded by remember { mutableStateOf(false) }
            val selectedValueItemName = if (valueRef == "none") {
                "Nenhum (--)"
            } else {
                groupItems.find { it.id == valueRef }?.let { it.customLabel.ifBlank { "Item" } } ?: if (valueRef.isBlank()) "Nenhum (--)" else "Item"
            }

            ExposedDropdownMenuBox(
                expanded = valueExpanded,
                onExpandedChange = { valueExpanded = !valueExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedValueItemName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Valor (Item do Grupo)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = valueExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                    colors = darkTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = valueExpanded,
                    onDismissRequest = { valueExpanded = false },
                    modifier = Modifier.background(Color(0xFF02121C))
                ) {
                    DropdownMenuItem(
                        text = { Text("Nenhum (--)", color = Color.White) },
                        colors = MenuDefaults.itemColors(textColor = Color.White),
                        onClick = {
                            valueExpanded = false
                            val updatedMappings = cardConfig.fieldMappings.toMutableList()
                            val existingIdx = updatedMappings.indexOfFirst { it.fieldIndex == fieldIndex }
                            if (existingIdx >= 0) {
                                updatedMappings[existingIdx] = updatedMappings[existingIdx].copy(valueItemReference = "none")
                            } else {
                                updatedMappings.add(com.example.analizer.model.CardFieldMapping(fieldIndex = fieldIndex, titleItemReference = titleRef, valueItemReference = "none"))
                            }
                            val updatedCards = cards.toMutableList()
                            updatedCards[cardIdx] = cardConfig.copy(fieldMappings = updatedMappings)
                            onConfigChanged(config.copy(cards = updatedCards))
                        }
                    )
                    groupItems.forEachIndexed { itemIdx, item ->
                        val itemLabel = item.customLabel.ifBlank { "Item ${itemIdx + 1}" }
                        DropdownMenuItem(
                            text = { Text("$itemLabel (${item.metric})", color = Color.White) },
                            colors = MenuDefaults.itemColors(textColor = Color.White),
                            onClick = {
                                valueExpanded = false
                                val updatedMappings = cardConfig.fieldMappings.toMutableList()
                                val existingIdx = updatedMappings.indexOfFirst { it.fieldIndex == fieldIndex }
                                if (existingIdx >= 0) {
                                    updatedMappings[existingIdx] = updatedMappings[existingIdx].copy(valueItemReference = item.id)
                                } else {
                                    updatedMappings.add(com.example.analizer.model.CardFieldMapping(fieldIndex = fieldIndex, titleItemReference = titleRef, valueItemReference = item.id))
                                }
                                val updatedCards = cards.toMutableList()
                                updatedCards[cardIdx] = cardConfig.copy(fieldMappings = updatedMappings)
                                onConfigChanged(config.copy(cards = updatedCards))
                            }
                        )
                    }
                }
            }
        }
    }
}
