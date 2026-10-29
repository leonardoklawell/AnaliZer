package com.example.analizer.model

import kotlinx.serialization.Serializable
import java.util.UUID

val METRIC_OPTIONS = listOf(
    "Soma",
    "Média",
    "Mediana",
    "Mínimo",
    "Máximo",
    "Desvio Padrão",
    "Contagem"
)

@Serializable
data class ColumnRangeInterval(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val startColumn: Int = 0, // 0-indexed column index
    val endColumn: Int = 0,   // 0-indexed column index
    val startRow: Int? = null, // specific start row (or null to use global)
    val endRow: Int? = null,   // specific end row (or null to use global)
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
    val intervals: List<ColumnRangeInterval> = emptyList()
) {
    val start: Int get() = startRow
    val end: Int get() = endRow
    val rowRange: IntRange get() = startRow..endRow

    fun getMetricForColumn(columnIndex: Int): String {
        return columnMetrics[columnIndex]
            ?: selectedMetrics[columnIndex]
            ?: intervals.find { columnIndex in it.startColumn..it.endColumn }?.metric
            ?: "Média"
    }
}
