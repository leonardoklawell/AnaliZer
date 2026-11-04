package com.example.analizer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analizer.Screen
import com.example.analizer.domain.StatisticsEngine
import com.example.analizer.model.AnalysisConfig
import com.example.analizer.model.CardConfig
import com.example.analizer.model.CardFieldMapping
import com.example.analizer.model.DEFAULT_CARD_FIELD_NAMES
import com.example.analizer.model.DisplayGroup
import com.example.analizer.model.DisplayGroupItem
import com.example.analizer.model.METRIC_OPTIONS
import com.example.analizer.model.MonthSelectorOption
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
                        IconButton(onClick = onExportConfig) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = "Exportar Configurações", tint = Color(0xFFEBA036))
                        }
                        IconButton(onClick = onImportConfig) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = "Importar Configurações", tint = Color(0xFFEBA036))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D1525),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
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

    val displayGroups = if (config.displayGroups.isNotEmpty()) {
        config.displayGroups
    } else {
        config.effectiveDisplayGroups
    }

    val cards = config.effectiveCards
    val monthOptions = config.monthOptions

    val summaryMetrics = if (config.summaryMetrics.isEmpty()) com.example.analizer.model.DEFAULT_SUMMARY_METRICS else config.summaryMetrics

    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onScreenSelected(Screen.Import) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Grupos", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = Color(0xFF0D1525),
                    unselectedContentColor = Color(0xFF788497)
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Cards de Análise", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = Color(0xFF0D1525),
                    unselectedContentColor = Color(0xFF788497)
                )
                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                    text = { Text("Seletor de Período", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = Color(0xFF0D1525),
                    unselectedContentColor = Color(0xFF788497)
                )
                Tab(
                    selected = pagerState.currentPage == 3,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(3) } },
                    text = { Text("Métricas de Resumo", fontWeight = FontWeight.SemiBold) },
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
                        // Tab 0: Display Groups
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
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
                                        text = "Configuração de Grupos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEBA036)
                                    )
                                    Text(
                                        text = "O nome do grupo tem como padrão o nome da Pasta Mãe selecionada, mas você pode personalizá-lo digitando abaixo. Ao adicionar um card, os 19 campos são criados automaticamente.",
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
                                            DisplayGroupEditorCard(
                                                group = group,
                                                groupIndex = index,
                                                spreadsheetData = spreadsheetData,
                                                cards = cards,
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
                                                },
                                                onAddCardToGroup = {
                                                    val newCard = CardConfig(
                                                        groupId = group.id,
                                                        selectedSheet = spreadsheetData.getSheetNamesForFolder(group.selectedFolder).firstOrNull() ?: "",
                                                        fieldMappings = (0 until 19).map { idx ->
                                                            CardFieldMapping(
                                                                fieldIndex = idx,
                                                                titleItemReference = DEFAULT_CARD_FIELD_NAMES.getOrElse(idx) { "ITEM ${idx + 1}" },
                                                                valueItemReference = ""
                                                            )
                                                        }
                                                    )
                                                    onConfigChanged(config.copy(cards = cards + newCard))
                                                }
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val folderNames = spreadsheetData.folderNames
                                            val defaultFolder = folderNames.firstOrNull() ?: "Pasta Principal"
                                            val newGroup = DisplayGroup(
                                                name = defaultFolder,
                                                selectedFolder = defaultFolder,
                                                selectedSheets = if (defaultFolder.isNotBlank()) {
                                                    listOf(spreadsheetData.getSheetNamesForFolder(defaultFolder).firstOrNull() ?: "")
                                                } else emptyList(),
                                                displayOrder = displayGroups.size
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
                                        Text("Adicionar Grupo", color = Color(0xFFEBA036), fontWeight = FontWeight.SemiBold)
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
                                Text("Ver Cards na Tela de Análise", fontWeight = FontWeight.Bold, color = Color(0xFFEBA036))
                            }
                        }
                    }
                    1 -> {
                        // Tab 1: Cards Configuration
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
                                        text = "Configuração e Pré-visualização dos Cards",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0B1825)
                                    )
                                    Text(
                                        text = "Ao adicionar um card, os 19 campos são criados prontos para configuração via digitação direta de células.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Button(
                                    onClick = {
                                        val firstGroup = displayGroups.firstOrNull()
                                        val newCard = CardConfig(
                                            groupId = firstGroup?.id ?: "",
                                            selectedSheet = if (firstGroup != null && firstGroup.selectedFolder.isNotBlank()) {
                                                spreadsheetData.getSheetNamesForFolder(firstGroup.selectedFolder).firstOrNull() ?: ""
                                            } else "",
                                            fieldMappings = (0 until 19).map { idx ->
                                                CardFieldMapping(
                                                    fieldIndex = idx,
                                                    titleItemReference = DEFAULT_CARD_FIELD_NAMES.getOrElse(idx) { "ITEM ${idx + 1}" },
                                                    valueItemReference = ""
                                                )
                                            }
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
                                            text = "Nenhum card configurado. Clique em 'Adicionar Card' acima.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFD0DCE5)
                                        )
                                    }
                                }
                            } else {
                                cards.forEachIndexed { cardIdx, cardConfig ->
                                    CardEditorView(
                                        cardConfig = cardConfig,
                                        cardIdx = cardIdx,
                                        cards = cards,
                                        displayGroups = displayGroups,
                                        spreadsheetData = spreadsheetData,
                                        columnStats = columnStats,
                                        config = config,
                                        onConfigChanged = onConfigChanged
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Tab 2: Period / Month Selector Configuration
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
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
                                        text = "Configuração do Seletor de Período",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEBA036)
                                    )
                                    Text(
                                        text = "Adicione, remova ou renomeie as opções do seletor (meses). Configure o deslocamento de colunas para mover automaticamente as células dos cards com a opção ativada.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD0DCE5)
                                    )

                                    monthOptions.forEachIndexed { optIdx, option ->
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
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = option.name,
                                                    onValueChange = { newName ->
                                                        val updatedOptions = monthOptions.toMutableList()
                                                        updatedOptions[optIdx] = option.copy(name = newName)
                                                        onConfigChanged(config.copy(monthOptions = updatedOptions))
                                                    },
                                                    label = { Text("Nome da Opção", fontSize = 11.sp) },
                                                    modifier = Modifier.weight(1.2f),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp),
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                                                    colors = darkTextFieldColors()
                                                )

                                                OutlinedTextField(
                                                    value = "${option.columnOffset}",
                                                    onValueChange = { input ->
                                                        val clean = input.filter { it.isDigit() || it == '-' }
                                                        val newOffset = clean.toIntOrNull() ?: 0
                                                        val updatedOptions = monthOptions.toMutableList()
                                                        updatedOptions[optIdx] = option.copy(columnOffset = newOffset)
                                                        onConfigChanged(config.copy(monthOptions = updatedOptions))
                                                    },
                                                    label = { Text("Deslocamento Coluna", fontSize = 11.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    shape = RoundedCornerShape(8.dp),
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                                                    colors = darkTextFieldColors()
                                                )

                                                if (monthOptions.size > 1) {
                                                    IconButton(
                                                        onClick = {
                                                            val updatedOptions = monthOptions.toMutableList().apply { removeAt(optIdx) }
                                                            onConfigChanged(config.copy(monthOptions = updatedOptions))
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Delete,
                                                            contentDescription = "Excluir Opção",
                                                            tint = Color(0xFFFF6B6B)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val nextIndex = monthOptions.size
                                            val newOption = MonthSelectorOption(
                                                name = "Mês $nextIndex",
                                                columnOffset = nextIndex
                                            )
                                            onConfigChanged(config.copy(monthOptions = monthOptions + newOption))
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
                                        Text("Adicionar Item ao Seletor", color = Color(0xFFEBA036), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // Tab 3: Summary Metrics Configuration
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
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
                                        text = "Configuração das Métricas de Resumo",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEBA036)
                                    )
                                    Text(
                                        text = "Marque ou desmarque os cards superiores exibidos no topo da tela de Análise (Linhas, Colunas, Grupos, Cards) e personalize seus rótulos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD0DCE5)
                                    )

                                    summaryMetrics.forEachIndexed { smIdx, metric ->
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
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = metric.enabled,
                                                    onCheckedChange = { checked ->
                                                        val updated = summaryMetrics.toMutableList()
                                                        updated[smIdx] = metric.copy(enabled = checked)
                                                        onConfigChanged(config.copy(summaryMetrics = updated))
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = Color(0xFFEBA036),
                                                        uncheckedColor = Color(0xFF788497),
                                                        checkmarkColor = Color(0xFF0D1525)
                                                    )
                                                )

                                                OutlinedTextField(
                                                    value = metric.label,
                                                    onValueChange = { newLabel ->
                                                        val updated = summaryMetrics.toMutableList()
                                                        updated[smIdx] = metric.copy(label = newLabel)
                                                        onConfigChanged(config.copy(summaryMetrics = updated))
                                                    },
                                                    label = { Text("Rótulo do Card", fontSize = 11.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp),
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                                                    colors = darkTextFieldColors()
                                                )

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF1B273B)
                                                ) {
                                                    Text(
                                                        text = metric.metricType,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFEBA036)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayGroupEditorCard(
    group: DisplayGroup,
    groupIndex: Int,
    spreadsheetData: SpreadsheetData,
    cards: List<CardConfig>,
    onUpdateGroup: (DisplayGroup) -> Unit,
    onDeleteGroup: () -> Unit,
    onAddCardToGroup: () -> Unit
) {
    val folderNames = spreadsheetData.folderNames
    val currentFolder = group.selectedFolder.ifBlank { folderNames.firstOrNull() ?: "Pasta Principal" }
    val groupDisplayName = group.name.ifBlank { currentFolder }

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
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = Color(0xFFEBA036)
                    )
                    Text(
                        text = groupDisplayName,
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

            // Dropdown Pasta Mãe + Campo Nome do Grupo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    var folderExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = folderExpanded,
                        onExpandedChange = { folderExpanded = !folderExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentFolder,
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
                                        val fSheets = spreadsheetData.getSheetNamesForFolder(folder)
                                        val newSheet = fSheets.firstOrNull() ?: ""
                                        onUpdateGroup(
                                            group.copy(
                                                selectedFolder = folder,
                                                selectedSheets = if (newSheet.isNotBlank()) listOf(newSheet) else emptyList(),
                                                name = folder
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = groupDisplayName,
                    onValueChange = { onUpdateGroup(group.copy(name = it)) },
                    label = { Text("Nome do Grupo", fontSize = 11.sp) },
                    placeholder = { Text(currentFolder, color = Color(0xFF788497)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                    colors = darkTextFieldColors()
                )
            }

            HorizontalDivider(color = Color(0xFF2C3E55))

            // Cards in this Group
            val groupCards = cards.filter { it.groupId == group.id || (group.id.isNotBlank() && it.groupId.isBlank() && cards.indexOf(it) == 0) }
            Text(
                text = "Cards vinculados a este Grupo - ${groupCards.size}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEBA036)
            )

            Button(
                onClick = onAddCardToGroup,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0B1825),
                    contentColor = Color(0xFFEBA036)
                )
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFFEBA036))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Adicionar Card a este Grupo", color = Color(0xFFEBA036), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorView(
    cardConfig: CardConfig,
    cardIdx: Int,
    cards: List<CardConfig>,
    displayGroups: List<DisplayGroup>,
    spreadsheetData: SpreadsheetData,
    columnStats: List<com.example.analizer.model.ColumnStats>,
    config: AnalysisConfig,
    onConfigChanged: (AnalysisConfig) -> Unit
) {
    val linkedGroup = displayGroups.find { it.id == cardConfig.groupId } ?: displayGroups.firstOrNull()
    val groupFolder = linkedGroup?.selectedFolder ?: spreadsheetData.folderNames.firstOrNull() ?: ""
    val availableSheets = spreadsheetData.getSheetNamesForFolder(groupFolder).ifEmpty { spreadsheetData.sheetNames }
    val currentSheet = cardConfig.selectedSheet.ifBlank { availableSheets.firstOrNull() ?: "" }
    val groupNameDisplay = linkedGroup?.name?.ifBlank { linkedGroup.selectedFolder }

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
            // Header Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (!groupNameDisplay.isNullOrBlank()) "Card ${cardIdx + 1} - $groupNameDisplay" else "Card ${cardIdx + 1}",
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
                                id = UUID.randomUUID().toString(),
                                fieldMappings = cardConfig.fieldMappings.map {
                                    it.copy(id = UUID.randomUUID().toString())
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
                        modifier = Modifier.height(34.dp)
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
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Excluir Card",
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                }
            }

            // Live Card Preview
            AnalysisStatCard(
                cardConfig = cardConfig,
                columnStats = columnStats,
                displayGroups = displayGroups,
                spreadsheetData = spreadsheetData,
                currentSheetName = currentSheet,
                cardIndex = cardIdx,
                selectedMonth = config.selectedMonth,
                monthOptions = config.monthOptions
            )

            HorizontalDivider(color = Color(0xFF2C3E55))

            // Dropdown selection for Group & Sheet Tab
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Select Group
                Box(modifier = Modifier.weight(1f)) {
                    var groupExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = groupExpanded,
                        onExpandedChange = { groupExpanded = !groupExpanded }
                    ) {
                        OutlinedTextField(
                            value = linkedGroup?.name?.ifBlank { linkedGroup.selectedFolder } ?: "Selecione o Grupo",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Grupo", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = Color.White),
                            colors = darkTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = groupExpanded,
                            onDismissRequest = { groupExpanded = false },
                            modifier = Modifier.background(Color(0xFF02121C))
                        ) {
                            displayGroups.forEach { grp ->
                                val grpLabel = grp.name.ifBlank { grp.selectedFolder.ifBlank { "Grupo sem nome" } }
                                DropdownMenuItem(
                                    text = { Text(grpLabel, color = Color.White) },
                                    colors = MenuDefaults.itemColors(textColor = Color.White),
                                    onClick = {
                                        groupExpanded = false
                                        val gSheets = spreadsheetData.getSheetNamesForFolder(grp.selectedFolder)
                                        val newCard = cardConfig.copy(
                                            groupId = grp.id,
                                            selectedSheet = gSheets.firstOrNull() ?: ""
                                        )
                                        val updatedCards = cards.toMutableList()
                                        updatedCards[cardIdx] = newCard
                                        onConfigChanged(config.copy(cards = updatedCards))
                                    }
                                )
                            }
                        }
                    }
                }

                // Select Sheet Tab ("Aba da Planilha")
                Box(modifier = Modifier.weight(1f)) {
                    var sheetExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = sheetExpanded,
                        onExpandedChange = { sheetExpanded = !sheetExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentSheet.ifBlank { "Selecione a Aba" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Aba da Planilha", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sheetExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = Color.White),
                            colors = darkTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = sheetExpanded,
                            onDismissRequest = { sheetExpanded = false },
                            modifier = Modifier.background(Color(0xFF02121C))
                        ) {
                            availableSheets.forEach { sheet ->
                                DropdownMenuItem(
                                    text = { Text(sheet, color = Color.White) },
                                    colors = MenuDefaults.itemColors(textColor = Color.White),
                                    onClick = {
                                        sheetExpanded = false
                                        val updatedCards = cards.toMutableList()
                                        updatedCards[cardIdx] = cardConfig.copy(selectedSheet = sheet)
                                        onConfigChanged(config.copy(cards = updatedCards))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Checkbox to obey the period/month selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val updatedCards = cards.toMutableList()
                        updatedCards[cardIdx] = cardConfig.copy(obeyMonthSelector = !cardConfig.obeyMonthSelector)
                        onConfigChanged(config.copy(cards = updatedCards))
                    }
            ) {
                Checkbox(
                    checked = cardConfig.obeyMonthSelector,
                    onCheckedChange = { checked ->
                        val updatedCards = cards.toMutableList()
                        updatedCards[cardIdx] = cardConfig.copy(obeyMonthSelector = checked)
                        onConfigChanged(config.copy(cards = updatedCards))
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFEBA036),
                        uncheckedColor = Color(0xFF788497)
                    )
                )
                Text(
                    text = "Obedecer ao seletor de período (mover células por mês)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Text(
                text = "Configuração dos 19 Campos do Card",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEBA036)
            )

            // Render Header 3 Items
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Cabeçalho do Card",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SingleFieldDirectInput(
                        fieldIndex = 0,
                        label = "ITEM 1",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 1,
                        label = "ITEM 2",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 2,
                        label = "ITEM 3",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF2C3E55))

            // Render Body 16 Items
            Text(
                text = "Corpo do Card",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SingleFieldDirectInput(
                        fieldIndex = 3,
                        label = "ITEM 4",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 4,
                        label = "ITEM 5",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 5,
                        label = "ITEM 6",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 6,
                        label = "ITEM 7",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SingleFieldDirectInput(
                        fieldIndex = 7,
                        label = "ITEM 8",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 8,
                        label = "ITEM 9",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 9,
                        label = "ITEM 10",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 10,
                        label = "ITEM 11",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SingleFieldDirectInput(
                        fieldIndex = 11,
                        label = "ITEM 12",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 12,
                        label = "ITEM 13",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 13,
                        label = "ITEM 14",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 14,
                        label = "ITEM 15",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SingleFieldDirectInput(
                        fieldIndex = 15,
                        label = "ITEM 16",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 16,
                        label = "ITEM 17",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 17,
                        label = "ITEM 18",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                    SingleFieldDirectInput(
                        fieldIndex = 18,
                        label = "ITEM 19",
                        cardConfig = cardConfig,
                        cardIdx = cardIdx,
                        cards = cards,
                        config = config,
                        onConfigChanged = onConfigChanged,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SingleFieldDirectInput(
    fieldIndex: Int,
    label: String,
    cardConfig: CardConfig,
    cardIdx: Int,
    cards: List<CardConfig>,
    config: AnalysisConfig,
    onConfigChanged: (AnalysisConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val mapping = cardConfig.fieldMappings.find { it.fieldIndex == fieldIndex }
    val currentText = mapping?.valueItemReference?.ifBlank { mapping.titleItemReference } ?: ""

    OutlinedTextField(
        value = currentText,
        onValueChange = { newText ->
            val updatedMappings = cardConfig.fieldMappings.toMutableList()
            val existingIdx = updatedMappings.indexOfFirst { it.fieldIndex == fieldIndex }
            if (existingIdx >= 0) {
                updatedMappings[existingIdx] = updatedMappings[existingIdx].copy(
                    titleItemReference = newText,
                    valueItemReference = newText
                )
            } else {
                updatedMappings.add(
                    CardFieldMapping(
                        fieldIndex = fieldIndex,
                        titleItemReference = newText,
                        valueItemReference = newText
                    )
                )
            }
            val updatedCards = cards.toMutableList()
            updatedCards[cardIdx] = cardConfig.copy(fieldMappings = updatedMappings)
            onConfigChanged(config.copy(cards = updatedCards))
        },
        label = { Text(label, fontSize = 10.sp) },
        placeholder = { Text("ex: S3", color = Color(0xFF788497), fontSize = 10.sp) },
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color.White),
        colors = darkTextFieldColors()
    )
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
