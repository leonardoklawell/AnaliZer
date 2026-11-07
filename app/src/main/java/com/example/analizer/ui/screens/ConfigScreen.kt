package com.example.analizer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextOverflow
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

    val pagerState = rememberPagerState(pageCount = { 6 })
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
                    text = { Text("Grupos & Perfis", fontWeight = FontWeight.SemiBold) },
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
                Tab(
                    selected = pagerState.currentPage == 4,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(4) } },
                    text = { Text("Gráfico de Barras", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = Color(0xFF0D1525),
                    unselectedContentColor = Color(0xFF788497)
                )
                Tab(
                    selected = pagerState.currentPage == 5,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(5) } },
                    text = { Text("Gráfico de Linhas", fontWeight = FontWeight.SemiBold) },
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
                        // Tab 0: Display Groups & Profiles (Separated Containers)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Container 1: Save & Load Profiles
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1825))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Perfis de Configuração dos Grupos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEBA036)
                                    )
                                    Text(
                                        text = "Salve o conjunto atual de grupos e cards como um perfil no aplicativo para restaurar a qualquer momento.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD0DCE5)
                                    )

                                    var profileNameInput by remember { mutableStateOf("") }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = profileNameInput,
                                            onValueChange = { profileNameInput = it },
                                            label = { Text("Nome do Perfil", fontSize = 11.sp) },
                                            placeholder = { Text("ex: Perfil Produção", fontSize = 11.sp, color = Color(0xFF788497)) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, color = Color.White),
                                            colors = darkTextFieldColors()
                                        )

                                        Button(
                                            onClick = {
                                                if (profileNameInput.isNotBlank()) {
                                                    val newProfile = com.example.analizer.model.GroupProfile(
                                                        name = profileNameInput.trim(),
                                                        displayGroups = config.displayGroups,
                                                        cards = config.cards
                                                    )
                                                    onConfigChanged(config.copy(savedProfiles = config.savedProfiles + newProfile))
                                                    profileNameInput = ""
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEBA036), contentColor = Color(0xFF0D1525))
                                        ) {
                                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Salvar Perfil", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }

                                    if (config.savedProfiles.isNotEmpty()) {
                                        Text(
                                            text = "Perfis Salvos (${config.savedProfiles.size}):",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        config.savedProfiles.forEachIndexed { pIdx, profile ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF02121C)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(profile.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                        Text("${profile.displayGroups.size} grupos, ${profile.cards.size} cards", fontSize = 11.sp, color = Color(0xFF8A99B5))
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Button(
                                                            onClick = {
                                                                onConfigChanged(config.copy(displayGroups = profile.displayGroups, cards = profile.cards))
                                                            },
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B273B), contentColor = Color(0xFFEBA036))
                                                        ) {
                                                            Text("Aplicar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                val updatedProfiles = config.savedProfiles.toMutableList().apply { removeAt(pIdx) }
                                                                onConfigChanged(config.copy(savedProfiles = updatedProfiles))
                                                            }
                                                        ) {
                                                            Icon(Icons.Rounded.Delete, contentDescription = "Excluir Perfil", tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Header for Groups Management
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Grupos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0D1525)
                                )

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
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B1825), contentColor = Color(0xFFEBA036))
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFFEBA036))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Adicionar Grupo", color = Color(0xFFEBA036), fontWeight = FontWeight.Bold)
                                }
                            }

                            if (displayGroups.isEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1825)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Nenhum grupo configurado. Clique em 'Adicionar Grupo' acima.",
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
                                        onDuplicateGroup = {
                                            val newGroupId = UUID.randomUUID().toString()
                                            val duplicatedGroup = group.copy(
                                                id = newGroupId,
                                                name = if (group.name.isNotBlank()) "${group.name} (Cópia)" else ""
                                            )
                                            val updatedGroups = displayGroups.toMutableList()
                                            updatedGroups.add(index + 1, duplicatedGroup)

                                            val linkedCards = cards.filter { it.groupId == group.id }
                                            val duplicatedCards = linkedCards.map { card ->
                                                card.copy(
                                                    id = UUID.randomUUID().toString(),
                                                    groupId = newGroupId,
                                                    fieldMappings = card.fieldMappings.map { mapping ->
                                                        mapping.copy(id = UUID.randomUUID().toString())
                                                    }
                                                )
                                            }

                                            onConfigChanged(config.copy(
                                                displayGroups = updatedGroups,
                                                cards = cards + duplicatedCards
                                            ))
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

                            // Run Analysis Button
                            Button(
                                onClick = {
                                    onRunAnalysis()
                                    onScreenSelected(Screen.Analytics)
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B1825), contentColor = Color(0xFFEBA036))
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
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1825)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1825))
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
                                        text = "Adicione, remova ou marque qual item é o Padrão (célula base). Ao clicar em um item anterior/posterior, o deslocamento é calculado relativamente à esquerda ou direita.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD0DCE5)
                                    )

                                    monthOptions.forEachIndexed { optIdx, option ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF02121C)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Default Option Checkbox
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable {
                                                        val updatedOptions = monthOptions.mapIndexed { idx, opt ->
                                                            opt.copy(isDefault = (idx == optIdx))
                                                        }
                                                        onConfigChanged(config.copy(monthOptions = updatedOptions))
                                                    }
                                                ) {
                                                    RadioButton(
                                                        selected = option.isDefault,
                                                        onClick = {
                                                            val updatedOptions = monthOptions.mapIndexed { idx, opt ->
                                                                opt.copy(isDefault = (idx == optIdx))
                                                            }
                                                            onConfigChanged(config.copy(monthOptions = updatedOptions))
                                                        },
                                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFEBA036), unselectedColor = Color(0xFF788497))
                                                    )
                                                    Text("Padrão", fontSize = 11.sp, color = if (option.isDefault) Color(0xFFEBA036) else Color.White, fontWeight = if (option.isDefault) FontWeight.Bold else FontWeight.Normal)
                                                }

                                                val resolvedOptName = com.example.analizer.model.resolveMonthOptionDisplayName(option, spreadsheetData, config.selectedFolder, config.sheetName, displayGroups)

                                                OutlinedTextField(
                                                    value = option.name,
                                                    onValueChange = { newName ->
                                                        val updatedOptions = monthOptions.toMutableList()
                                                        updatedOptions[optIdx] = option.copy(name = newName)
                                                        onConfigChanged(config.copy(monthOptions = updatedOptions))
                                                    },
                                                    label = { Text("Nomes", fontSize = 11.sp) },
                                                    supportingText = if (resolvedOptName != option.name) {
                                                        { Text("Exibe: $resolvedOptName", fontSize = 10.sp, color = Color(0xFFEBA036)) }
                                                    } else null,
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
                                                    label = { Text("Deslocamento", fontSize = 11.sp) },
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
                                                        Icon(Icons.Rounded.Delete, contentDescription = "Excluir Opção", tint = Color(0xFFFF6B6B))
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
                                                columnOffset = nextIndex,
                                                isDefault = monthOptions.isEmpty()
                                            )
                                            onConfigChanged(config.copy(monthOptions = monthOptions + newOption))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF02121C), contentColor = Color(0xFFEBA036))
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFFEBA036))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Adicionar Item ao Seletor", color = Color(0xFFEBA036), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // Quadro: Itens dos Cards que obedecem ao Seletor de Período
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1825))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "Itens dos Cards que obedecem ao Seletor",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEBA036)
                                    )
                                    Text(
                                        text = "Marque ou desmarque os itens dos cards que vão obedecer à mudança de célula realizada pelo seletor de período.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD0DCE5)
                                    )

                                    // Action Buttons: Marcar Todos / Desmarcar Todos
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val currentCards = config.effectiveCards
                                                val updatedCards = currentCards.map { card ->
                                                    val updatedMappings = card.fieldMappings.map { mapping ->
                                                        mapping.copy(obeyMonthSelector = true)
                                                    }
                                                    card.copy(obeyMonthSelector = true, fieldMappings = updatedMappings)
                                                }
                                                onConfigChanged(config.copy(cards = updatedCards))
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF02121C),
                                                contentColor = Color(0xFFEBA036)
                                            )
                                        ) {
                                            Text("Marcar Todos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                val currentCards = config.effectiveCards
                                                val updatedCards = currentCards.map { card ->
                                                    val updatedMappings = card.fieldMappings.map { mapping ->
                                                        mapping.copy(obeyMonthSelector = false)
                                                    }
                                                    card.copy(obeyMonthSelector = false, fieldMappings = updatedMappings)
                                                }
                                                onConfigChanged(config.copy(cards = updatedCards))
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF02121C),
                                                contentColor = Color(0xFFFF6B6B)
                                            )
                                        ) {
                                            Text("Desmarcar Todos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    val activeCards = config.effectiveCards
                                    if (activeCards.isEmpty()) {
                                        Text(
                                            text = "Nenhum card configurado no momento.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF788497)
                                        )
                                    } else {
                                        activeCards.forEachIndexed { cardIdx, cardConfig ->
                                            val linkedGroup = config.effectiveDisplayGroups.find { it.id == cardConfig.groupId }
                                            val groupFolderClean = (linkedGroup?.selectedFolder ?: "").substringBefore(" - ").substringAfterLast("/").trim()
                                            val groupTitle = linkedGroup?.name?.takeIf { it.isNotBlank() }
                                                ?: groupFolderClean.ifBlank { "Card ${cardIdx + 1}" }

                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF02121C)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    // Header with Card toggle
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            modifier = Modifier.clickable {
                                                                val newObey = !cardConfig.obeyMonthSelector
                                                                val updatedCards = activeCards.toMutableList()
                                                                val updatedMappings = cardConfig.fieldMappings.map { it.copy(obeyMonthSelector = newObey) }
                                                                updatedCards[cardIdx] = cardConfig.copy(obeyMonthSelector = newObey, fieldMappings = updatedMappings)
                                                                onConfigChanged(config.copy(cards = updatedCards))
                                                            }
                                                        ) {
                                                            Checkbox(
                                                                checked = cardConfig.obeyMonthSelector,
                                                                onCheckedChange = { checked ->
                                                                    val updatedCards = activeCards.toMutableList()
                                                                    val updatedMappings = cardConfig.fieldMappings.map { it.copy(obeyMonthSelector = checked) }
                                                                    updatedCards[cardIdx] = cardConfig.copy(obeyMonthSelector = checked, fieldMappings = updatedMappings)
                                                                    onConfigChanged(config.copy(cards = updatedCards))
                                                                },
                                                                colors = CheckboxDefaults.colors(
                                                                    checkedColor = Color(0xFFEBA036),
                                                                    uncheckedColor = Color(0xFF788497)
                                                                )
                                                            )
                                                            Text(
                                                                text = "Card ${cardIdx + 1} - $groupTitle",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp,
                                                                color = Color(0xFFEBA036)
                                                            )
                                                        }
                                                    }

                                                    HorizontalDivider(color = Color(0xFF2C3E55))

                                                    // Items list for this Card
                                                    val enabledMappings = cardConfig.fieldMappings.filter { it.enabled }
                                                    val displayMappings = if (enabledMappings.isNotEmpty()) enabledMappings else cardConfig.fieldMappings.take(19)

                                                    val cardFolderName = linkedGroup?.selectedFolder?.ifBlank { linkedGroup?.name ?: "" } ?: ""
                                                    val cardSheetName = cardConfig.selectedSheet.ifBlank { linkedGroup?.selectedSheets?.firstOrNull() ?: "" }
                                                    val cardSheetRows = if (cardFolderName.isNotBlank()) {
                                                        spreadsheetData?.getRows(cardFolderName, cardSheetName)
                                                            ?: (spreadsheetData?.sheets?.get(cardSheetName) ?: emptyList())
                                                    } else {
                                                        spreadsheetData?.sheets?.get(cardSheetName) ?: emptyList()
                                                    }.ifEmpty { spreadsheetData?.sheets?.values?.firstOrNull() ?: emptyList() }
                                                    val engine = com.example.analizer.domain.StatisticsEngine()

                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        displayMappings.forEach { mapping ->
                                                            val defaultLabel = "ITEM ${mapping.fieldIndex + 1}"
                                                            val cellRef = mapping.valueItemReference.trim().ifBlank { mapping.titleItemReference.trim() }
                                                            val resolvedText = if (cellRef.isNotBlank() && engine.parseCellReference(cellRef) != null) {
                                                                engine.resolveCellText(cardSheetRows, cellRef)
                                                            } else null

                                                            val itemLabel = resolvedText?.takeIf { it.isNotBlank() }
                                                                ?: mapping.titleItemReference.takeIf { it.isNotBlank() && engine.parseCellReference(it) == null && it != defaultLabel }
                                                                ?: defaultLabel
                                                            val isItemObey = cardConfig.obeyMonthSelector && mapping.obeyMonthSelector

                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable {
                                                                        val newObey = !isItemObey
                                                                        val updatedMappings = cardConfig.fieldMappings.map { m ->
                                                                            if (m.fieldIndex == mapping.fieldIndex) m.copy(obeyMonthSelector = newObey) else m
                                                                        }
                                                                        val updatedCards = activeCards.toMutableList()
                                                                        updatedCards[cardIdx] = cardConfig.copy(fieldMappings = updatedMappings)
                                                                        onConfigChanged(config.copy(cards = updatedCards))
                                                                    }
                                                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                                                            ) {
                                                                Checkbox(
                                                                    checked = isItemObey,
                                                                    onCheckedChange = { checked ->
                                                                        val updatedMappings = cardConfig.fieldMappings.map { m ->
                                                                            if (m.fieldIndex == mapping.fieldIndex) m.copy(obeyMonthSelector = checked) else m
                                                                        }
                                                                        val updatedCards = activeCards.toMutableList()
                                                                        updatedCards[cardIdx] = cardConfig.copy(fieldMappings = updatedMappings)
                                                                        onConfigChanged(config.copy(cards = updatedCards))
                                                                    },
                                                                    colors = CheckboxDefaults.colors(
                                                                        checkedColor = Color(0xFFEBA036),
                                                                        uncheckedColor = Color(0xFF788497)
                                                                    )
                                                                )
                                                                Text(
                                                                    text = itemLabel,
                                                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                                                    color = if (isItemObey) Color.White else Color(0xFF64748B),
                                                                    fontWeight = if (isItemObey) FontWeight.SemiBold else FontWeight.Normal
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
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1825))
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
                                        text = "Personalize as métricas do topo da tela de Análise. Escolha entre Métricas do Sistema (Linhas, Colunas, Grupos, Cards) ou Célula de Planilha direta com formatação.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD0DCE5)
                                    )

                                    summaryMetrics.forEachIndexed { smIdx, metric ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF02121C)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = metric.enabled,
                                                        onCheckedChange = { checked ->
                                                            val updated = summaryMetrics.toMutableList()
                                                            updated[smIdx] = metric.copy(enabled = checked)
                                                            onConfigChanged(config.copy(summaryMetrics = updated))
                                                        },
                                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEBA036), uncheckedColor = Color(0xFF788497))
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

                                                    // Type Switch
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFF1B273B),
                                                        modifier = Modifier.clickable {
                                                            val nextType = if (metric.valueSourceType == "CELL") "METRIC" else "CELL"
                                                            val updated = summaryMetrics.toMutableList()
                                                            updated[smIdx] = metric.copy(valueSourceType = nextType)
                                                            onConfigChanged(config.copy(summaryMetrics = updated))
                                                        }
                                                    ) {
                                                        Text(
                                                            text = if (metric.valueSourceType == "CELL") "Célula" else metric.metricType,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFEBA036)
                                                        )
                                                    }
                                                }

                                                if (metric.valueSourceType == "CELL") {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        OutlinedTextField(
                                                            value = metric.cellReference,
                                                            onValueChange = { newCell ->
                                                                val updated = summaryMetrics.toMutableList()
                                                                updated[smIdx] = metric.copy(cellReference = newCell)
                                                                onConfigChanged(config.copy(summaryMetrics = updated))
                                                            },
                                                            label = { Text("Célula (ex: A1)", fontSize = 10.sp) },
                                                            modifier = Modifier.weight(1f),
                                                            singleLine = true,
                                                            shape = RoundedCornerShape(8.dp),
                                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color.White),
                                                            colors = darkTextFieldColors()
                                                        )

                                                        var fmtExpanded by remember { mutableStateOf(false) }
                                                        val formatOpts = listOf("AUTO" to "Automático", "CURRENCY" to "Moeda R$", "INTEGER" to "Inteiro", "DECIMAL" to "Decimal", "PERCENT" to "Porcentagem %")
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            ExposedDropdownMenuBox(
                                                                expanded = fmtExpanded,
                                                                onExpandedChange = { fmtExpanded = !fmtExpanded }
                                                            ) {
                                                                val selName = formatOpts.find { it.first == metric.numberFormat }?.second ?: "Automático"
                                                                OutlinedTextField(
                                                                    value = selName,
                                                                    onValueChange = {},
                                                                    readOnly = true,
                                                                    label = { Text("Formato", fontSize = 10.sp) },
                                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fmtExpanded) },
                                                                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    singleLine = true,
                                                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color.White),
                                                                    colors = darkTextFieldColors()
                                                                )
                                                                ExposedDropdownMenu(
                                                                    expanded = fmtExpanded,
                                                                    onDismissRequest = { fmtExpanded = false },
                                                                    modifier = Modifier.background(Color(0xFF02121C))
                                                                ) {
                                                                    formatOpts.forEach { (code, name) ->
                                                                        DropdownMenuItem(
                                                                            text = { Text(name, color = Color.White, fontSize = 12.sp) },
                                                                            onClick = {
                                                                                fmtExpanded = false
                                                                                val updated = summaryMetrics.toMutableList()
                                                                                updated[smIdx] = metric.copy(numberFormat = code)
                                                                                onConfigChanged(config.copy(summaryMetrics = updated))
                                                                            }
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
                    4 -> {
                        // Tab 4: Bar Chart Detailed Config
                        ChartConfigTab(
                            isLineChart = false,
                            config = config,
                            spreadsheetData = spreadsheetData,
                            columnStats = columnStats,
                            onConfigChanged = onConfigChanged
                        )
                    }
                    5 -> {
                        // Tab 5: Line Chart Detailed Config
                        ChartConfigTab(
                            isLineChart = true,
                            config = config,
                            spreadsheetData = spreadsheetData,
                            columnStats = columnStats,
                            onConfigChanged = onConfigChanged
                        )
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
    onDuplicateGroup: () -> Unit,
    onAddCardToGroup: () -> Unit
) {
    val folderNames = spreadsheetData.folderNames
    val currentFolder = group.selectedFolder.ifBlank { folderNames.firstOrNull() ?: "Pasta Principal" }
    val cleanParentFolderName = currentFolder
        .replace(Regex("\\.(xlsx|xls|csv|gsheet)$", RegexOption.IGNORE_CASE), "")
        .substringBefore(" - ")
        .substringAfterLast("/")
        .trim()
    val groupDisplayName = cleanParentFolderName

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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onDuplicateGroup,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0B1825),
                            contentColor = Color(0xFFEBA036)
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Duplicar Grupo",
                            tint = Color(0xFFEBA036),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Duplicar", fontSize = 12.sp, color = Color(0xFFEBA036), fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(
                        onClick = onDeleteGroup,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Remover Grupo",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
                            value = cleanParentFolderName,
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
                                val cleanFolderItem = folder
                                    .replace(Regex("\\.(xlsx|xls|csv|gsheet)$", RegexOption.IGNORE_CASE), "")
                                    .substringBefore(" - ")
                                    .substringAfterLast("/")
                                    .trim()
                                DropdownMenuItem(
                                    text = { Text(cleanFolderItem, color = Color.White) },
                                    colors = MenuDefaults.itemColors(textColor = Color.White),
                                    onClick = {
                                        folderExpanded = false
                                        val fSheets = spreadsheetData.getSheetNamesForFolder(cleanFolderItem)
                                        val newSheet = fSheets.firstOrNull() ?: ""
                                        onUpdateGroup(
                                            group.copy(
                                                selectedFolder = cleanFolderItem,
                                                selectedSheets = if (newSheet.isNotBlank()) listOf(newSheet) else emptyList(),
                                                name = if (group.name.isBlank() || group.name == currentFolder || group.name == cleanParentFolderName) cleanFolderItem else group.name
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = group.name,
                    onValueChange = { onUpdateGroup(group.copy(name = it)) },
                    label = { Text("Nome do Grupo", fontSize = 11.sp) },
                    placeholder = { Text(cleanParentFolderName, color = Color(0xFF788497)) },
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
    val cleanGroupFolder = groupFolder.substringBefore(" - ").substringAfterLast("/").trim()
    val availableSheets = spreadsheetData.getSheetNamesForFolder(cleanGroupFolder).ifEmpty { spreadsheetData.sheetNames }
    val currentSheet = cardConfig.selectedSheet.ifBlank { availableSheets.firstOrNull() ?: "" }
    val groupNameDisplay = linkedGroup?.name?.takeIf { it.isNotBlank() } ?: cleanGroupFolder

    var isExpanded by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        CardSettingsDialog(
            cardConfig = cardConfig,
            onSave = { updatedCard ->
                val updatedCards = cards.toMutableList()
                updatedCards[cardIdx] = updatedCard
                onConfigChanged(config.copy(cards = updatedCards))
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

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
                    // Gear icon for settings
                    IconButton(
                        onClick = { showSettingsDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Configurações do Card",
                            tint = Color(0xFFEBA036),
                            modifier = Modifier.size(20.dp)
                        )
                    }

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
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Excluir Card",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = "Expandir / Recolher Card",
                            tint = Color.White
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

            if (isExpanded) {
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
                            val selectedGroupDisplay = linkedGroup?.name?.takeIf { it.isNotBlank() }
                                ?: linkedGroup?.selectedFolder?.substringBefore(" - ")?.substringAfterLast("/")?.trim()
                                ?: "Selecione o Grupo"
                            OutlinedTextField(
                                value = selectedGroupDisplay,
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
                                    val grpLabel = grp.name.takeIf { it.isNotBlank() }
                                        ?: grp.selectedFolder.substringBefore(" - ").substringAfterLast("/").trim().ifBlank { "Grupo sem nome" }
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

                // Header for Card Fields with expand/collapse arrow on far right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Campos do Card (19 itens)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEBA036)
                    )

                    IconButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = "Expandir/Recolher Campos",
                            tint = Color(0xFFEBA036),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

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
fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
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
fun CardSettingsDialog(
    cardConfig: CardConfig,
    onSave: (CardConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var fieldMappings by remember { mutableStateOf(cardConfig.fieldMappings.toMutableList()) }

    val formatOptions = listOf(
        "AUTO" to "Automático",
        "CURRENCY" to "Moeda (R$)",
        "INTEGER" to "Número Inteiro",
        "DECIMAL" to "Decimal (2 casas)",
        "PERCENT" to "Porcentagem (%)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D1525),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = null, tint = Color(0xFFEBA036))
                Text("Configuração do Card", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ative/desative a exibição dos itens e defina o formato de valores:",
                    fontSize = 12.sp,
                    color = Color(0xFFB0BEC5)
                )

                (0 until 19).forEach { fieldIdx ->
                    val mappingIdx = fieldMappings.indexOfFirst { it.fieldIndex == fieldIdx }
                    val currentMapping = if (mappingIdx >= 0) fieldMappings[mappingIdx] else CardFieldMapping(fieldIndex = fieldIdx, titleItemReference = "ITEM ${fieldIdx + 1}")

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF02121C)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Checkbox(
                                        checked = currentMapping.enabled,
                                        onCheckedChange = { checked ->
                                            val updated = fieldMappings.toMutableList()
                                            val newMap = currentMapping.copy(enabled = checked)
                                            if (mappingIdx >= 0) updated[mappingIdx] = newMap else updated.add(newMap)
                                            fieldMappings = updated
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFFEBA036),
                                            uncheckedColor = Color(0xFF788497)
                                        )
                                    )
                                    Text(
                                        text = "ITEM ${fieldIdx + 1} (${currentMapping.titleItemReference.ifBlank { "Sem nome" }})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (currentMapping.enabled) Color.White else Color(0xFF64748B)
                                    )
                                }
                            }

                            if (currentMapping.enabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var formatExpanded by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.weight(1f)) {
                                        ExposedDropdownMenuBox(
                                            expanded = formatExpanded,
                                            onExpandedChange = { formatExpanded = !formatExpanded }
                                        ) {
                                            val selectedLabel = formatOptions.find { it.first == currentMapping.numberFormat }?.second ?: "Automático"
                                            OutlinedTextField(
                                                value = selectedLabel,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Formato", fontSize = 10.sp) },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color.White),
                                                colors = darkTextFieldColors()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = formatExpanded,
                                                onDismissRequest = { formatExpanded = false },
                                                modifier = Modifier.background(Color(0xFF02121C))
                                            ) {
                                                formatOptions.forEach { (code, name) ->
                                                    DropdownMenuItem(
                                                        text = { Text(name, color = Color.White, fontSize = 12.sp) },
                                                        onClick = {
                                                            formatExpanded = false
                                                            val updated = fieldMappings.toMutableList()
                                                            val newMap = currentMapping.copy(numberFormat = code)
                                                            if (mappingIdx >= 0) updated[mappingIdx] = newMap else updated.add(newMap)
                                                            fieldMappings = updated
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                val updated = fieldMappings.toMutableList()
                                                val newMap = currentMapping.copy(thousandsSeparator = !currentMapping.thousandsSeparator)
                                                if (mappingIdx >= 0) updated[mappingIdx] = newMap else updated.add(newMap)
                                                fieldMappings = updated
                                            }
                                        ) {
                                            Checkbox(
                                                checked = currentMapping.thousandsSeparator,
                                                onCheckedChange = { checked ->
                                                    val updated = fieldMappings.toMutableList()
                                                    val newMap = currentMapping.copy(thousandsSeparator = checked)
                                                    if (mappingIdx >= 0) updated[mappingIdx] = newMap else updated.add(newMap)
                                                    fieldMappings = updated
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFFEBA036),
                                                    uncheckedColor = Color(0xFF788497)
                                                )
                                            )
                                            Text("Milhar", fontSize = 11.sp, color = Color.White)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                val updated = fieldMappings.toMutableList()
                                                val newMap = currentMapping.copy(obeyMonthSelector = !currentMapping.obeyMonthSelector)
                                                if (mappingIdx >= 0) updated[mappingIdx] = newMap else updated.add(newMap)
                                                fieldMappings = updated
                                            }
                                        ) {
                                            Checkbox(
                                                checked = currentMapping.obeyMonthSelector,
                                                onCheckedChange = { checked ->
                                                    val updated = fieldMappings.toMutableList()
                                                    val newMap = currentMapping.copy(obeyMonthSelector = checked)
                                                    if (mappingIdx >= 0) updated[mappingIdx] = newMap else updated.add(newMap)
                                                    fieldMappings = updated
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFFEBA036),
                                                    uncheckedColor = Color(0xFF788497)
                                                )
                                            )
                                            Text("Obedecer Seletor", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(cardConfig.copy(fieldMappings = fieldMappings))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEBA036), contentColor = Color(0xFF0D1525))
            ) {
                Text("Salvar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFFB0BEC5))
            }
        }
    )
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartConfigTab(
    isLineChart: Boolean,
    config: AnalysisConfig,
    spreadsheetData: SpreadsheetData?,
    columnStats: List<com.example.analizer.model.ColumnStats>,
    onConfigChanged: (AnalysisConfig) -> Unit
) {
    val cards = config.effectiveCards
    val displayGroups = if (config.displayGroups.isNotEmpty()) config.displayGroups else config.effectiveDisplayGroups

    val currentQueryMode = if (isLineChart) config.lineChartQueryMode else config.barChartQueryMode
    val currentSelectedCardIndex = if (isLineChart) config.lineChartSelectedCardIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0)) else config.barChartSelectedCardIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0))
    val currentSelectedItemIndex = if (isLineChart) config.lineChartSelectedItemIndex else config.barChartSelectedItemIndex
    val currentCustomInterval = if (isLineChart) config.lineChartCustomInterval else config.barChartCustomInterval
    val currentItemsSubMode = if (isLineChart) config.lineChartItemsSubMode else config.barChartItemsSubMode

    var cardDropdownExpanded by remember { mutableStateOf(false) }
    var itemDropdownExpanded by remember { mutableStateOf(false) }

    val allowedValueIndices = listOf(4, 8, 12, 16, 6, 10, 14, 18)

    fun updateConfig(
        queryMode: String = currentQueryMode,
        cardIdx: Int = currentSelectedCardIndex,
        itemIdx: Int = currentSelectedItemIndex,
        interval: String = currentCustomInterval,
        subMode: String = currentItemsSubMode
    ) {
        if (isLineChart) {
            onConfigChanged(
                config.copy(
                    lineChartQueryMode = queryMode,
                    lineChartSelectedCardIndex = cardIdx,
                    lineChartSelectedItemIndex = itemIdx,
                    lineChartCustomInterval = interval,
                    lineChartItemsSubMode = subMode
                )
            )
        } else {
            onConfigChanged(
                config.copy(
                    barChartQueryMode = queryMode,
                    barChartSelectedCardIndex = cardIdx,
                    barChartSelectedItemIndex = itemIdx,
                    barChartCustomInterval = interval,
                    barChartItemsSubMode = subMode
                )
            )
        }
    }

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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1825)),
            border = BorderStroke(1.dp, Color(0xFF1E283E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF152238), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLineChart) Icons.Rounded.ShowChart else Icons.Rounded.BarChart,
                            contentDescription = null,
                            tint = Color(0xFFEBA036),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isLineChart) "Configuração do Gráfico de Linhas" else "Configuração do Gráfico de Barras",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEBA036)
                        )
                        Text(
                            text = "Configure os dados e intervalos que serão consultados na aba de Análise.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E283E))

                // Chart Title
                OutlinedTextField(
                    value = if (isLineChart) config.lineChartTitle else config.barChartTitle,
                    onValueChange = {
                        if (isLineChart) onConfigChanged(config.copy(lineChartTitle = it))
                        else onConfigChanged(config.copy(barChartTitle = it))
                    },
                    label = { Text("Título do Gráfico", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFEBA036),
                        unfocusedBorderColor = Color(0xFF1E283E),
                        focusedContainerColor = Color(0xFF152238),
                        unfocusedContainerColor = Color(0xFF152238)
                    )
                )

                // Query Mode Selection
                Text(
                    text = "Dados a Serem Consultados:",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                // Chips for query mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        Triple("ITEMS", "Itens dos Cards", Icons.Rounded.ViewList),
                        Triple("GROUPS", "Grupos", Icons.Rounded.Folder),
                        Triple("CARDS", "Cards", Icons.Rounded.CreditCard),
                        Triple("INTERVAL", "Intervalo", Icons.Rounded.GridOn)
                    )
                    modes.forEach { (modeKey, label, icon) ->
                        val isSelected = currentQueryMode == modeKey
                        Surface(
                            onClick = { updateConfig(queryMode = modeKey) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFEBA036) else Color(0xFF152238),
                            border = if (!isSelected) BorderStroke(1.dp, Color(0xFF1E283E)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF0B1825) else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF0B1825) else Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }

                // Sub-controls based on active query mode
                when (currentQueryMode) {
                    "ITEMS" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Sub-mode toggle: Card Items vs Compare Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = { updateConfig(subMode = "CARD_ITEMS") },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (currentItemsSubMode == "CARD_ITEMS") Color(0xFF1E293B) else Color(0xFF101726),
                                    border = BorderStroke(1.dp, if (currentItemsSubMode == "CARD_ITEMS") Color(0xFFEBA036) else Color(0xFF1E283E)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Itens do Card",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (currentItemsSubMode == "CARD_ITEMS") Color(0xFFEBA036) else Color(0xFF94A3B8),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { updateConfig(subMode = "COMPARE_CARDS") },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (currentItemsSubMode == "COMPARE_CARDS") Color(0xFF1E293B) else Color(0xFF101726),
                                    border = BorderStroke(1.dp, if (currentItemsSubMode == "COMPARE_CARDS") Color(0xFFEBA036) else Color(0xFF1E283E)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Comparar Cards",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (currentItemsSubMode == "COMPARE_CARDS") Color(0xFFEBA036) else Color(0xFF94A3B8),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            if (currentItemsSubMode == "CARD_ITEMS") {
                                // Card selection dropdown
                                Text("Selecione o Card a consultar:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Box {
                                    OutlinedButton(
                                        onClick = { cardDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color(0xFF152238),
                                            contentColor = Color.White
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF1E283E))
                                    ) {
                                        val currentCard = cards.getOrNull(currentSelectedCardIndex)
                                        val group = displayGroups.find { it.id == currentCard?.groupId }
                                        val currentCardTitle = resolveGroupName(group, spreadsheetData, config.sheetName, currentSelectedCardIndex)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Card: $currentCardTitle", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color(0xFFEBA036))
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = cardDropdownExpanded,
                                        onDismissRequest = { cardDropdownExpanded = false },
                                        modifier = Modifier.background(Color(0xFF0D1525))
                                    ) {
                                        cards.forEachIndexed { idx, card ->
                                            val group = displayGroups.find { it.id == card.groupId }
                                            val title = resolveGroupName(group, spreadsheetData, config.sheetName, idx)
                                            DropdownMenuItem(
                                                text = { Text(title, color = if (currentSelectedCardIndex == idx) Color(0xFFEBA036) else Color.White) },
                                                onClick = {
                                                    updateConfig(cardIdx = idx)
                                                    cardDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Item selector dropdown
                                Text("Selecione o Item para comparar entre os cards:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Box {
                                    OutlinedButton(
                                        onClick = { itemDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color(0xFF152238),
                                            contentColor = Color.White
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF1E283E))
                                    ) {
                                        val currentTitle = resolveItemTitle(currentSelectedItemIndex, cards.firstOrNull(), columnStats, displayGroups, spreadsheetData, config)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Item: $currentTitle", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color(0xFFEBA036))
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = itemDropdownExpanded,
                                        onDismissRequest = { itemDropdownExpanded = false },
                                        modifier = Modifier.background(Color(0xFF0D1525))
                                    ) {
                                        allowedValueIndices.forEach { vIdx ->
                                            val title = resolveItemTitle(vIdx, cards.firstOrNull(), columnStats, displayGroups, spreadsheetData, config)
                                            DropdownMenuItem(
                                                text = { Text(title, color = if (currentSelectedItemIndex == vIdx) Color(0xFFEBA036) else Color.White) },
                                                onClick = {
                                                    updateConfig(itemIdx = vIdx)
                                                    itemDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "GROUPS", "CARDS" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (currentQueryMode == "GROUPS") "Selecione o item para comparar entre os Grupos:" else "Selecione o item para comparar entre os Cards:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Box {
                                OutlinedButton(
                                    onClick = { itemDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFF152238),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF1E283E))
                                ) {
                                    val currentTitle = resolveItemTitle(currentSelectedItemIndex, cards.firstOrNull(), columnStats, displayGroups, spreadsheetData, config)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Item: $currentTitle", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color(0xFFEBA036))
                                    }
                                }
                                DropdownMenu(
                                    expanded = itemDropdownExpanded,
                                    onDismissRequest = { itemDropdownExpanded = false },
                                    modifier = Modifier.background(Color(0xFF0D1525))
                                ) {
                                    allowedValueIndices.forEach { vIdx ->
                                        val title = resolveItemTitle(vIdx, cards.firstOrNull(), columnStats, displayGroups, spreadsheetData, config)
                                        DropdownMenuItem(
                                            text = { Text(title, color = if (currentSelectedItemIndex == vIdx) Color(0xFFEBA036) else Color.White) },
                                            onClick = {
                                                updateConfig(itemIdx = vIdx)
                                                itemDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "INTERVAL" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Digite o intervalo de células da planilha a ser consultado:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            OutlinedTextField(
                                value = currentCustomInterval,
                                onValueChange = { updateConfig(interval = it) },
                                label = { Text("Intervalo de Células", color = Color(0xFF94A3B8)) },
                                placeholder = { Text("Ex: B2:B12 ou C2:C15", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFEBA036),
                                    unfocusedBorderColor = Color(0xFF1E283E),
                                    focusedContainerColor = Color(0xFF152238),
                                    unfocusedContainerColor = Color(0xFF152238)
                                )
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("B2:B10", "B2:B15", "C2:C10", "D2:D10", "S3:S15").forEach { preset ->
                                    Surface(
                                        onClick = { updateConfig(interval = preset) },
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF152238),
                                        border = BorderStroke(1.dp, Color(0xFF1E283E))
                                    ) {
                                        Text(
                                            text = preset,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color(0xFFEBA036),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1E283E))

                // Preview summary badge
                val previewPoints = computeChartDataPoints(isLineChart, config, spreadsheetData, columnStats)
                Surface(
                    color = Color(0xFF152238),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E283E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Prévia da Consulta:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEBA036)
                            )
                            Text(
                                text = "${previewPoints.size} pontos encontrados",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        if (previewPoints.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                previewPoints.take(8).forEach { pt ->
                                    Surface(
                                        color = Color(0xFF0B1825),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF1E283E))
                                    ) {
                                        Text(
                                            text = "${pt.label}: ${pt.formattedValue}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (previewPoints.size > 8) {
                                    Text(
                                        text = "+${previewPoints.size - 8} mais",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Nenhum valor numérico encontrado para a consulta selecionada.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}
