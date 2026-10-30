package com.example.analizer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.analizer.Screen
import com.example.analizer.domain.StatisticsEngine
import com.example.analizer.model.AnalysisConfig
import com.example.analizer.model.CardFieldMapping
import com.example.analizer.model.DisplayGroup
import com.example.analizer.model.DisplayGroupItem
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

    val displayGroups = if (config.displayGroups.isNotEmpty()) {
        config.displayGroups
    } else {
        config.effectiveDisplayGroups
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuração de Grupos de Exibição", fontWeight = FontWeight.Bold) },
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
                        text = "Aba da Planilha Principal",
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
                            label = { Text("Selecione a Aba Principal") },
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

                                        onConfigChanged(
                                            config.copy(
                                                sheetName = sheet,
                                                selectedColumnIndices = cols,
                                                startRow = if (rows.size > 1) 1 else 0,
                                                endRow = if (rows.isNotEmpty()) rows.size - 1 else 0
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                    if (sheetNames.size > 1) {
                        Text(
                            text = "Abas encontradas (${sheetNames.size}): ${sheetNames.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

            // Display Groups Container Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Grupos de Exibição",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Organize suas análises em Grupos de Exibição com nome em texto ou por célula, múltiplas abas e sub-intervalos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (displayGroups.isEmpty()) {
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
                                    text = "Nenhum grupo configurado. Clique abaixo para adicionar.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        displayGroups.forEachIndexed { index, group ->
                            DisplayGroupCard(
                                group = group,
                                groupIndex = index,
                                sheets = spreadsheetData.sheets,
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

                    // Add Group Button
                    OutlinedButton(
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar Grupo de Exibição")
                    }
                }
            }

            // Group Ordering & Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Posicionamento e Seleção para Análise",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Escolha quais grupos devem participar da análise e reordene a posição em que aparecem nos cards e gráficos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    displayGroups.forEachIndexed { index, group ->
                        val groupName = if (group.nameSourceType == "CELL") "Célula ${group.cellReference}" else (group.name.ifBlank { "Grupo ${index + 1}" })
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
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
                                        }
                                    )
                                    Text(
                                        text = "${index + 1}. $groupName",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Mover para Cima")
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
                                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Mover para Baixo")
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
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Seleção de Grupos para Gráficos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Escolha quais grupos serão comparados nos gráficos (se nenhum for marcado, todos os ativos serão comparados).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    displayGroups.forEachIndexed { index, group ->
                        val groupName = if (group.nameSourceType == "CELL") "Célula ${group.cellReference}" else (group.name.ifBlank { "Grupo ${index + 1}" })
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
                                }
                            )
                            Text(text = groupName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Card Field Mappings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Configuração de Campos do Card",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Personalize os títulos e vincule grupos e métricas a cada um dos 6 campos dos cards de análise.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val defaultFieldNames = listOf("Média", "Mediana", "Desv. Padrão", "Mín / Máx", "Soma", "Contagem")
                    for (fieldIdx in 0..5) {
                        val mapping = config.cardFieldMappings.find { it.fieldIndex == fieldIdx }
                        val customTitle = mapping?.customTitle ?: ""
                        val selectedGroupId = mapping?.groupId ?: ""
                        val selectedMetric = mapping?.metric ?: "Média"

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Campo ${fieldIdx + 1} (Padrão: ${defaultFieldNames[fieldIdx]})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedTextField(
                                    value = customTitle,
                                    onValueChange = { newTitle ->
                                        val mutableMappings = config.cardFieldMappings.toMutableList()
                                        val existingIndex = mutableMappings.indexOfFirst { it.fieldIndex == fieldIdx }
                                        if (existingIndex >= 0) {
                                            mutableMappings[existingIndex] = mutableMappings[existingIndex].copy(customTitle = newTitle)
                                        } else {
                                            mutableMappings.add(CardFieldMapping(fieldIndex = fieldIdx, customTitle = newTitle, groupId = selectedGroupId, metric = selectedMetric))
                                        }
                                        onConfigChanged(config.copy(cardFieldMappings = mutableMappings))
                                    },
                                    label = { Text("Título Personalizado") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                // Group Link Dropdown
                                var groupDropdownExpanded by remember { mutableStateOf(false) }
                                val linkedGroupName = displayGroups.find { it.id == selectedGroupId || it.name == selectedGroupId }?.let {
                                    if (it.nameSourceType == "CELL") "Célula ${it.cellReference}" else it.name
                                } ?: "Grupo Padrão"

                                ExposedDropdownMenuBox(
                                    expanded = groupDropdownExpanded,
                                    onExpandedChange = { groupDropdownExpanded = !groupDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = linkedGroupName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Grupo Vinculado") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = groupDropdownExpanded,
                                        onDismissRequest = { groupDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Grupo Padrão (Automático)") },
                                            onClick = {
                                                groupDropdownExpanded = false
                                                val mutableMappings = config.cardFieldMappings.toMutableList()
                                                val existingIndex = mutableMappings.indexOfFirst { it.fieldIndex == fieldIdx }
                                                val gId = ""
                                                if (existingIndex >= 0) {
                                                    mutableMappings[existingIndex] = mutableMappings[existingIndex].copy(groupId = gId)
                                                } else {
                                                    mutableMappings.add(CardFieldMapping(fieldIndex = fieldIdx, customTitle = customTitle, groupId = gId, metric = selectedMetric))
                                                }
                                                onConfigChanged(config.copy(cardFieldMappings = mutableMappings))
                                            }
                                        )
                                        displayGroups.forEach { grp ->
                                            val gName = if (grp.nameSourceType == "CELL") "Célula ${grp.cellReference}" else grp.name.ifBlank { "Grupo" }
                                            DropdownMenuItem(
                                                text = { Text(gName) },
                                                onClick = {
                                                    groupDropdownExpanded = false
                                                    val mutableMappings = config.cardFieldMappings.toMutableList()
                                                    val existingIndex = mutableMappings.indexOfFirst { it.fieldIndex == fieldIdx }
                                                    if (existingIndex >= 0) {
                                                        mutableMappings[existingIndex] = mutableMappings[existingIndex].copy(groupId = grp.id)
                                                    } else {
                                                        mutableMappings.add(CardFieldMapping(fieldIndex = fieldIdx, customTitle = customTitle, groupId = grp.id, metric = selectedMetric))
                                                    }
                                                    onConfigChanged(config.copy(cardFieldMappings = mutableMappings))
                                                }
                                            )
                                        }
                                    }
                                }

                                // Metric Dropdown for this field
                                var metricDropdownExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = metricDropdownExpanded,
                                    onExpandedChange = { metricDropdownExpanded = !metricDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedMetric,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Métrica do Campo") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = metricDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = metricDropdownExpanded,
                                        onDismissRequest = { metricDropdownExpanded = false }
                                    ) {
                                        METRIC_OPTIONS.forEach { met ->
                                            DropdownMenuItem(
                                                text = { Text(met) },
                                                onClick = {
                                                    metricDropdownExpanded = false
                                                    val mutableMappings = config.cardFieldMappings.toMutableList()
                                                    val existingIndex = mutableMappings.indexOfFirst { it.fieldIndex == fieldIdx }
                                                    if (existingIndex >= 0) {
                                                        mutableMappings[existingIndex] = mutableMappings[existingIndex].copy(metric = met)
                                                    } else {
                                                        mutableMappings.add(CardFieldMapping(fieldIndex = fieldIdx, customTitle = customTitle, groupId = selectedGroupId, metric = met))
                                                    }
                                                    onConfigChanged(config.copy(cardFieldMappings = mutableMappings))
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
fun DisplayGroupCard(
    group: DisplayGroup,
    groupIndex: Int,
    sheets: Map<String, List<List<String>>>,
    sheetNames: List<String>,
    currentGlobalSheet: String,
    headerRowIndex: Int,
    customColumnTitles: Map<Int, String>,
    globalStartRow: Int,
    globalEndRow: Int,
    maxRowIndex: Int,
    onUpdateGroup: (DisplayGroup) -> Unit,
    onDeleteGroup: () -> Unit
) {
    val effectiveSheet = group.selectedSheets.firstOrNull() ?: currentGlobalSheet
    val sheetRows = sheets[effectiveSheet] ?: emptyList()
    val safeHeaderRowIndex = headerRowIndex.coerceIn(0, maxOf(0, maxRowIndex))
    val headerRow = sheetRows.getOrNull(safeHeaderRowIndex) ?: emptyList()
    val totalCols = headerRow.size.takeIf { it > 0 } ?: (sheetRows.maxOfOrNull { it.size } ?: 0)

    val resolvedName = if (group.nameSourceType == "CELL") {
        StatisticsEngine().resolveCellText(sheetRows, group.cellReference) ?: "(Célula vazia/inválida)"
    } else {
        group.name.ifBlank { "Grupo ${groupIndex + 1}" }
    }

    var nameTypeExpanded by remember { mutableStateOf(false) }
    val nameTypeLabel = if (group.nameSourceType == "CELL") "Pegar de Célula" else "Texto"

    var sheetsDropdownExpanded by remember { mutableStateOf(false) }
    val sheetsSummaryText = if (group.selectedSheets.isEmpty()) {
        "Aba Global ($currentGlobalSheet)"
    } else {
        group.selectedSheets.joinToString(", ")
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = resolvedName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDeleteGroup) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Remover Grupo",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Name Source Type Dropdown (Texto vs Pegar de Célula)
            ExposedDropdownMenuBox(
                expanded = nameTypeExpanded,
                onExpandedChange = { nameTypeExpanded = !nameTypeExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nameTypeLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Origem do Nome do Grupo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameTypeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = nameTypeExpanded,
                    onDismissRequest = { nameTypeExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Texto") },
                        onClick = {
                            nameTypeExpanded = false
                            onUpdateGroup(group.copy(nameSourceType = "TEXT"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Pegar de Célula") },
                        onClick = {
                            nameTypeExpanded = false
                            onUpdateGroup(group.copy(nameSourceType = "CELL"))
                        }
                    )
                }
            }

            if (group.nameSourceType != "CELL") {
                OutlinedTextField(
                    value = group.name,
                    onValueChange = { onUpdateGroup(group.copy(name = it)) },
                    label = { Text("Nome do Grupo (Texto)") },
                    placeholder = { Text("ex: Resumo Financeiro (ou deixe em branco)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            } else {
                OutlinedTextField(
                    value = group.cellReference,
                    onValueChange = { onUpdateGroup(group.copy(cellReference = it)) },
                    label = { Text("Referência da Célula (ex: A1)") },
                    placeholder = { Text("ex: B2") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    supportingText = {
                        val textFound = StatisticsEngine().resolveCellText(sheetRows, group.cellReference)
                        Text(if (textFound != null) "Texto encontrado: $textFound" else "Célula não encontrada ou vazia")
                    }
                )
            }

            // Sheets Dropdown with checkboxes
            ExposedDropdownMenuBox(
                expanded = sheetsDropdownExpanded,
                onExpandedChange = { sheetsDropdownExpanded = !sheetsDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = sheetsSummaryText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Abas Incluídas no Grupo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sheetsDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = sheetsDropdownExpanded,
                    onDismissRequest = { sheetsDropdownExpanded = false }
                ) {
                    sheetNames.forEach { sheet ->
                        val isChecked = group.selectedSheets.contains(sheet) || (group.selectedSheets.isEmpty() && sheet == currentGlobalSheet)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null
                                    )
                                    Text(sheet)
                                }
                            },
                            onClick = {
                                val currentSelected = if (group.selectedSheets.isEmpty()) listOf(currentGlobalSheet) else group.selectedSheets
                                val newSelected = if (isChecked) {
                                    currentSelected.filter { it != sheet }
                                } else {
                                    currentSelected + sheet
                                }
                                onUpdateGroup(group.copy(selectedSheets = newSelected))
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Items within group
            Text(
                text = "Itens do Grupo (Colunas e Linhas)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
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

            // Add Item button ("+")
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
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar Colunas/Linhas neste Grupo")
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
    globalStartRow: Int,
    globalEndRow: Int,
    onUpdateItem: (DisplayGroupItem) -> Unit,
    onDeleteItem: () -> Unit,
    canDelete: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
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
                    placeholder = { Text("ex: Vendas Nacionais") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                if (canDelete) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDeleteItem) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remover Item",
                            tint = MaterialTheme.colorScheme.error
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
                RowDropdown(
                    label = "Linha Início",
                    selectedRow = item.startRow,
                    maxRowIndex = maxRowIndex,
                    onRowSelected = { row ->
                        onUpdateItem(item.copy(startRow = row))
                    },
                    modifier = Modifier.weight(1f)
                )

                RowDropdown(
                    label = "Linha Fim",
                    selectedRow = item.endRow,
                    maxRowIndex = maxRowIndex,
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
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowDropdown(
    label: String,
    selectedRow: Int?,
    maxRowIndex: Int,
    onRowSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val options = (0..minOf(998, maxRowIndex)).map { it }
    val selectedText = if (selectedRow != null && selectedRow in 0..maxRowIndex) {
        "Linha ${selectedRow + 1}"
    } else {
        "Padrão Global"
    }

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
            shape = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Padrão Global") },
                onClick = {
                    expanded = false
                    onRowSelected(null)
                }
            )
            for (rowIdx in options) {
                DropdownMenuItem(
                    text = { Text("Linha ${rowIdx + 1}") },
                    onClick = {
                        expanded = false
                        onRowSelected(rowIdx)
                    }
                )
            }
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
            label = { Text("Métrica / Fórmula") },
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
