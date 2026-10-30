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
import com.example.analizer.Screen
import com.example.analizer.model.AnalysisConfig
import com.example.analizer.model.SpreadsheetData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetTableScreen(
    spreadsheetData: SpreadsheetData?,
    config: AnalysisConfig,
    onConfigChanged: (AnalysisConfig) -> Unit,
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    var sheetExpanded by remember { mutableStateOf(false) }

    val sheetNames = spreadsheetData?.sheetNames ?: emptyList()
    val currentSheetName = if (spreadsheetData != null && config.sheetName.isNotBlank() && sheetNames.contains(config.sheetName)) {
        config.sheetName
    } else {
        sheetNames.firstOrNull() ?: ""
    }

    val rows = spreadsheetData?.sheets?.get(currentSheetName) ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dados da Planilha", fontWeight = FontWeight.Bold) },
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
                // Sheet Selector (if multiple sheets)
                if (sheetNames.size > 1) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Aba da Planilha:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            ExposedDropdownMenuBox(
                                expanded = sheetExpanded,
                                onExpandedChange = { sheetExpanded = !sheetExpanded }
                            ) {
                                OutlinedButton(
                                    onClick = { sheetExpanded = true },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                ) {
                                    Text(currentSheetName)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                }
                                ExposedDropdownMenu(
                                    expanded = sheetExpanded,
                                    onDismissRequest = { sheetExpanded = false }
                                ) {
                                    sheetNames.forEach { sheet ->
                                        DropdownMenuItem(
                                            text = { Text(sheet) },
                                            onClick = {
                                                sheetExpanded = false
                                                val sheetRows = spreadsheetData.sheets[sheet] ?: emptyList()
                                                val cols = (0 until (sheetRows.firstOrNull()?.size ?: 0)).toList()
                                                onConfigChanged(
                                                    config.copy(
                                                        sheetName = sheet,
                                                        selectedColumnIndices = cols,
                                                        startRow = if (sheetRows.size > 1) 1 else 0,
                                                        endRow = sheetRows.size - 1
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
