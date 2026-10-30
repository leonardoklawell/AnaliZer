package com.example.analizer.model

import kotlinx.serialization.Serializable
import java.util.UUID

val METRIC_OPTIONS = listOf(
    "Média",
    "Soma",
    "Mediana",
    "Mínimo",
    "Máximo",
    "Desvio Padrão",
    "Contagem",
    "Valor da Célula",
    "Variância",
    "Soma Acumulada",
    "Taxa de Crescimento"
)

@Serializable
data class ColumnRangeInterval(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val sheetName: String? = null, // specific sheet for this interval, or null to use global
    val startColumn: Int = 0, // 0-indexed column index
    val endColumn: Int = 0,   // 0-indexed column index
    val startRow: Int? = null, // specific start row (or null to use global)
    val endRow: Int? = null,   // specific end row (or null to use global)
    val metric: String = "Média"
)

@Serializable
data class DisplayGroupItem(
    val id: String = UUID.randomUUID().toString(),
    val startColumn: Int = 0,
    val endColumn: Int = 0,
    val startRow: Int? = null,
    val endRow: Int? = null,
    val metric: String = "Média",
    val customLabel: String = ""
)

@Serializable
data class DisplayGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val nameSourceType: String = "TEXT", // "TEXT" or "CELL" (or "MANUAL")
    val cellReference: String = "A1",
    val selectedSheets: List<String> = emptyList(), // Multi-select sheets (checkboxes)
    val items: List<DisplayGroupItem> = listOf(DisplayGroupItem()),
    val enabledForAnalysis: Boolean = true,
    val displayOrder: Int = 0
)

@Serializable
data class CardFieldMapping(
    val id: String = UUID.randomUUID().toString(),
    val fieldIndex: Int = 0, // 0 to 5 for the 6 card detail fields
    val customTitle: String = "",
    val groupId: String = "",
    val metric: String = "Média"
)

@Serializable
data class AnalysisConfig(
    val sheetName: String = "",
    val headerRowIndex: Int = 0,
    val customColumnTitles: Map<Int, String> = emptyMap(),
    val selectedColumnIndices: List<Int> = emptyList(),
    val columnMetrics: Map<Int, String> = emptyMap(),
    val selectedMetrics: Map<Int, String> = emptyMap(),
    val startRow: Int = 0,
    val endRow: Int = Int.MAX_VALUE,
    val intervals: List<ColumnRangeInterval> = emptyList(),
    val displayGroups: List<DisplayGroup> = emptyList(),
    val chartSelectedGroupIds: List<String> = emptyList(),
    val cardTitles: Map<String, String> = emptyMap(),
    val cardFieldMappings: List<CardFieldMapping> = emptyList()
) {
    val start: Int get() = startRow
    val end: Int get() = endRow
    val rowRange: IntRange get() = startRow..endRow

    val effectiveDisplayGroups: List<DisplayGroup> get() {
        val groups = if (displayGroups.isNotEmpty()) {
            displayGroups
        } else if (intervals.isNotEmpty()) {
            intervals.mapIndexed { index, interval ->
                DisplayGroup(
                    id = interval.id,
                    name = interval.name.ifBlank { "Grupo ${index + 1}" },
                    selectedSheets = if (!interval.sheetName.isNullOrBlank()) listOf(interval.sheetName) else emptyList(),
                    items = listOf(
                        DisplayGroupItem(
                            startColumn = interval.startColumn,
                            endColumn = interval.endColumn,
                            startRow = interval.startRow,
                            endRow = interval.endRow,
                            metric = interval.metric
                        )
                    ),
                    displayOrder = index
                )
            }
        } else {
            emptyList()
        }
        return groups.sortedBy { it.displayOrder }
    }

    fun getMetricForColumn(columnIndex: Int): String {
        return columnMetrics[columnIndex]
            ?: selectedMetrics[columnIndex]
            ?: displayGroups.firstNotNullOfOrNull { group ->
                group.items.find { columnIndex in it.startColumn..it.endColumn }?.metric
            }
            ?: intervals.find { columnIndex in it.startColumn..it.endColumn }?.metric
            ?: "Média"
    }
}
