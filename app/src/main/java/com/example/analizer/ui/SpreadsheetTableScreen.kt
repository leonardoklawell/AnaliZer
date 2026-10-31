package com.example.analizer.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analizer.Screen
import com.example.analizer.model.AnalysisConfig
import com.example.analizer.model.SpreadsheetData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetTableScreen(
    spreadsheetData: SpreadsheetData?,
    config: AnalysisConfig,
    onConfigChanged: (AnalysisConfig) -> Unit,
    onOpenSettings: () -> Unit = {},
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    val folderNames = spreadsheetData?.folderNames ?: emptyList()
    var selectedFolder by remember(spreadsheetData) {
        mutableStateOf(
            if (folderNames.isNotEmpty()) {
                if (config.selectedFolder.isNotBlank() && folderNames.contains(config.selectedFolder)) {
                    config.selectedFolder
                } else {
                    spreadsheetData?.currentFolderName?.ifBlank { folderNames.first() } ?: folderNames.first()
                }
            } else ""
        )
    }

    val availableSheetNames = if (selectedFolder.isNotBlank()) {
        spreadsheetData?.getSheetNamesForFolder(selectedFolder) ?: emptyList()
    } else {
        spreadsheetData?.sheetNames ?: emptyList()
    }

    var selectedSheet by remember(selectedFolder, availableSheetNames, config.sheetName) {
        mutableStateOf(
            if (availableSheetNames.contains(config.sheetName)) {
                config.sheetName
            } else {
                availableSheetNames.firstOrNull() ?: ""
            }
        )
    }

    var folderExpanded by remember { mutableStateOf(false) }
    var sheetExpanded by remember { mutableStateOf(false) }

    val rows = if (selectedFolder.isNotBlank()) {
        spreadsheetData?.getRows(selectedFolder, selectedSheet) ?: emptyList()
    } else {
        spreadsheetData?.sheets?.get(selectedSheet) ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dados da Planilha", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Configurações e Conta Google")
                    }
                    IconButton(onClick = { onScreenSelected(Screen.Import) }) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = "Importar Planilha")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF0D1525),
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
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
        if (spreadsheetData == null || rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Nenhuma planilha carregada.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Importe um arquivo ou link para visualizar e analisar os dados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { onScreenSelected(Screen.Import) }) {
                            Icon(Icons.Rounded.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importar Planilha")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top bar card: Dropdowns for Pasta Mãe and Aba da Planilha
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dropdown 1: Pasta Mãe
                            Box(modifier = Modifier.weight(1f)) {
                                ExposedDropdownMenuBox(
                                    expanded = folderExpanded,
                                    onExpandedChange = { folderExpanded = !folderExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = if (folderNames.isNotEmpty()) selectedFolder else "Principal",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Pasta Mãe", fontSize = 11.sp) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = folderExpanded,
                                        onDismissRequest = { folderExpanded = false }
                                    ) {
                                        val displayFolders = if (folderNames.isNotEmpty()) folderNames else listOf("Principal")
                                        displayFolders.forEach { folder ->
                                            DropdownMenuItem(
                                                text = { Text(folder) },
                                                onClick = {
                                                    folderExpanded = false
                                                    selectedFolder = folder
                                                    val fSheets = spreadsheetData.getSheetNamesForFolder(folder)
                                                    val newSheet = fSheets.firstOrNull() ?: ""
                                                    selectedSheet = newSheet
                                                    val sheetRows = spreadsheetData.getRows(folder, newSheet)
                                                    val cols = (0 until (sheetRows.firstOrNull()?.size ?: 0)).toList()
                                                    onConfigChanged(
                                                        config.copy(
                                                            selectedFolder = folder,
                                                            sheetName = newSheet,
                                                            selectedColumnIndices = cols,
                                                            startRow = if (sheetRows.size > 1) 1 else 0,
                                                            endRow = if (sheetRows.isNotEmpty()) sheetRows.size - 1 else 0
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Dropdown 2: Aba da Planilha
                            Box(modifier = Modifier.weight(1f)) {
                                ExposedDropdownMenuBox(
                                    expanded = sheetExpanded,
                                    onExpandedChange = { sheetExpanded = !sheetExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedSheet.ifBlank { "Padrão" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Aba da Planilha", fontSize = 11.sp) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sheetExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = sheetExpanded,
                                        onDismissRequest = { sheetExpanded = false }
                                    ) {
                                        val displaySheets = if (availableSheetNames.isNotEmpty()) availableSheetNames else listOf("Padrão")
                                        displaySheets.forEach { sheet ->
                                            DropdownMenuItem(
                                                text = { Text(sheet) },
                                                onClick = {
                                                    sheetExpanded = false
                                                    selectedSheet = sheet
                                                    val sheetRows = if (selectedFolder.isNotBlank()) {
                                                        spreadsheetData.getRows(selectedFolder, sheet)
                                                    } else {
                                                        spreadsheetData.sheets[sheet] ?: emptyList()
                                                    }
                                                    val cols = (0 until (sheetRows.firstOrNull()?.size ?: 0)).toList()
                                                    onConfigChanged(
                                                        config.copy(
                                                            sheetName = sheet,
                                                            selectedColumnIndices = cols,
                                                            startRow = if (sheetRows.size > 1) 1 else 0,
                                                            endRow = if (sheetRows.isNotEmpty()) sheetRows.size - 1 else 0
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val maxDisplayRows = 100
                    Text(
                        text = if (rows.size > maxDisplayRows) "Linhas: ${rows.size} (exibindo primeiras $maxDisplayRows)" else "Total de linhas: ${rows.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total de colunas: ${rows.maxOfOrNull { it.size } ?: 0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Scrollable Spreadsheet Table
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    val horizontalScrollState = rememberScrollState()
                    val verticalScrollState = rememberScrollState()
                    val displayRows = rows.take(100)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScrollState)
                            .verticalScroll(verticalScrollState)
                            .padding(8.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            displayRows.forEachIndexed { rowIndex, row ->
                                val isHeader = rowIndex == config.headerRowIndex
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Row number indicator
                                    Surface(
                                        modifier = Modifier.width(44.dp).height(36.dp),
                                        color = if (isHeader) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (isHeader) "H" else rowIndex.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isHeader) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    row.forEach { cell ->
                                        Surface(
                                            modifier = Modifier
                                                .width(130.dp)
                                                .height(36.dp),
                                            color = if (isHeader) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                            else MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(6.dp),
                                            shadowElevation = if (isHeader) 2.dp else 0.dp
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Text(
                                                    text = cell,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isHeader) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick actions at bottom of table screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onScreenSelected(Screen.Config) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Configurar Intervalo")
                    }
                    Button(
                        onClick = { onScreenSelected(Screen.Analytics) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Análise")
                    }
                }
            }
        }
    }
}
