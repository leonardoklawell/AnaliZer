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
    val sheetName: String? = null,
    val startColumn: Int = 0,
    val endColumn: Int = 0,
    val startRow: Int? = null,
    val endRow: Int? = null,
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
    val customLabel: String = "",
    val cardFieldIndex: Int? = null
)

@Serializable
data class DisplayGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val nameSourceType: String = "TEXT",
    val cellReference: String = "A1",
    val selectedSheets: List<String> = emptyList(),
    val selectedFolder: String = "",
    val items: List<DisplayGroupItem> = emptyList(),
    val enabledForAnalysis: Boolean = true,
    val displayOrder: Int = 0
)

val DEFAULT_CARD_FIELD_NAMES = (1..19).map { "ITEM $it" }

@Serializable
data class CardFieldMapping(
    val id: String = UUID.randomUUID().toString(),
    val fieldIndex: Int = 0,
    val titleItemReference: String = "",
    val valueItemReference: String = "", // Cell reference (e.g. A1, S3) or direct text
    val enabled: Boolean = true,
    val numberFormat: String = "AUTO", // "AUTO", "CURRENCY", "INTEGER", "DECIMAL", "PERCENT"
    val thousandsSeparator: Boolean = true
)

fun formatFieldValue(valueStr: String, numberFormat: String, thousandsSeparator: Boolean): String {
    if (valueStr.isBlank() || valueStr == "--" || numberFormat == "AUTO") return valueStr
    val cleanStr = valueStr.replace("R$", "")
        .replace("%", "")
        .trim()
        .replace(".", "")
        .replace(",", ".")
    val doubleVal = cleanStr.toDoubleOrNull() ?: return valueStr

    val formatted = when (numberFormat) {
        "CURRENCY" -> {
            val num = String.format(java.util.Locale("pt", "BR"), "%,.2f", doubleVal)
            "R$ $num"
        }
        "INTEGER" -> String.format(java.util.Locale("pt", "BR"), "%,d", doubleVal.toLong())
        "DECIMAL" -> String.format(java.util.Locale("pt", "BR"), "%,.2f", doubleVal)
        "PERCENT" -> {
            val num = String.format(java.util.Locale("pt", "BR"), "%,.1f", doubleVal * 100)
            "$num%"
        }
        else -> valueStr
    }
    return if (!thousandsSeparator) {
        formatted.replace(".", "")
    } else formatted
}

@Serializable
data class MonthSelectorOption(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val columnOffset: Int = 0,
    val isDefault: Boolean = false
)

val DEFAULT_MONTH_OPTIONS = listOf(
    MonthSelectorOption(id = "opt_todos", name = "TODOS", columnOffset = 0, isDefault = true),
    MonthSelectorOption(id = "opt_jan", name = "JAN", columnOffset = 0),
    MonthSelectorOption(id = "opt_fev", name = "FEV", columnOffset = 1),
    MonthSelectorOption(id = "opt_mar", name = "MAR", columnOffset = 2),
    MonthSelectorOption(id = "opt_abr", name = "ABR", columnOffset = 3),
    MonthSelectorOption(id = "opt_mai", name = "MAI", columnOffset = 4),
    MonthSelectorOption(id = "opt_jun", name = "JUN", columnOffset = 5),
    MonthSelectorOption(id = "opt_jul", name = "JUL", columnOffset = 6),
    MonthSelectorOption(id = "opt_ago", name = "AGO", columnOffset = 7),
    MonthSelectorOption(id = "opt_set", name = "SET", columnOffset = 8),
    MonthSelectorOption(id = "opt_out", name = "OUT", columnOffset = 9),
    MonthSelectorOption(id = "opt_nov", name = "NOV", columnOffset = 10),
    MonthSelectorOption(id = "opt_dez", name = "DEZ", columnOffset = 11)
)

val MONTH_OPTIONS = listOf(
    "TODOS", "JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"
)

@Serializable
data class CardConfig(
    val id: String = UUID.randomUUID().toString(),
    val groupId: String = "",
    val selectedSheet: String = "",
    val obeyMonthSelector: Boolean = true,
    val fieldMappings: List<CardFieldMapping> = (0 until 19).map { idx ->
        CardFieldMapping(
            fieldIndex = idx,
            titleItemReference = DEFAULT_CARD_FIELD_NAMES.getOrElse(idx) { "ITEM ${idx + 1}" },
            valueItemReference = ""
        )
    }
)

@Serializable
data class SummaryMetricConfig(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val metricType: String = "LINHAS", // "LINHAS", "COLUNAS", "GRUPOS", "CARDS", "CELL"
    val valueSourceType: String = "METRIC", // "METRIC", "CELL"
    val cellReference: String = "",
    val sheetName: String = "",
    val numberFormat: String = "AUTO",
    val thousandsSeparator: Boolean = true,
    val enabled: Boolean = true
)

val DEFAULT_SUMMARY_METRICS = listOf(
    SummaryMetricConfig(id = "sm_linhas", label = "Linhas", metricType = "LINHAS", valueSourceType = "METRIC", enabled = true),
    SummaryMetricConfig(id = "sm_colunas", label = "Colunas", metricType = "COLUNAS", valueSourceType = "METRIC", enabled = true),
    SummaryMetricConfig(id = "sm_grupos", label = "Grupos", metricType = "GRUPOS", valueSourceType = "METRIC", enabled = true),
    SummaryMetricConfig(id = "sm_cards", label = "Cards", metricType = "CARDS", valueSourceType = "METRIC", enabled = true)
)

@Serializable
data class GroupProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val displayGroups: List<DisplayGroup> = emptyList(),
    val cards: List<CardConfig> = emptyList()
)

@Serializable
data class AnalysisConfig(
    val sheetName: String = "",
    val selectedFolder: String = "",
    val headerRowIndex: Int = 0,
    val customColumnTitles: Map<Int, String> = emptyMap(),
    val selectedColumnIndices: List<Int> = emptyList(),
    val columnMetrics: Map<Int, String> = emptyMap(),
    val selectedMetrics: Map<Int, String> = emptyMap(),
    val startRow: Int? = null,
    val endRow: Int? = null,
    val intervals: List<ColumnRangeInterval> = emptyList(),
    val displayGroups: List<DisplayGroup> = emptyList(),
    val chartSelectedGroupIds: List<String> = emptyList(),
    val cardTitles: Map<String, String> = emptyMap(),
    val cardFieldMappings: List<CardFieldMapping> = emptyList(),
    val cards: List<CardConfig> = emptyList(),
    val selectedMonth: String = "TODOS",
    val monthOptions: List<MonthSelectorOption> = DEFAULT_MONTH_OPTIONS,
    val summaryMetrics: List<SummaryMetricConfig> = DEFAULT_SUMMARY_METRICS,
    val savedProfiles: List<GroupProfile> = emptyList(),
    val barChartTitle: String = "Gráfico de Barras (Médias por Coluna)",
    val barChartColor: String = "GOLD", // "GOLD", "NAVY", "CORAL", "EMERALD", "PURPLE"
    val lineChartTitle: String = "Gráfico de Linhas (Tendência de Médias)",
    val lineChartColor: String = "NAVY",
    val sortFieldIndex: Int? = null,
    val sortAscending: Boolean = true
) {
    val start: Int? get() = startRow
    val end: Int? get() = endRow

    val effectiveDisplayGroups: List<DisplayGroup> get() {
        return displayGroups.sortedBy { it.displayOrder }
    }

    val effectiveCards: List<CardConfig> get() {
        if (cards.isNotEmpty()) return cards
        val groups = effectiveDisplayGroups
        if (groups.isNotEmpty()) {
            return groups.map { grp ->
                CardConfig(
                    id = "card_${grp.id}",
                    groupId = grp.id,
                    selectedSheet = grp.selectedSheets.firstOrNull() ?: "",
                    fieldMappings = (0 until 19).map { idx ->
                        CardFieldMapping(
                            fieldIndex = idx,
                            titleItemReference = DEFAULT_CARD_FIELD_NAMES.getOrElse(idx) { "ITEM ${idx + 1}" },
                            valueItemReference = ""
                        )
                    }
                )
            }
        }
        return listOf(
            CardConfig(
                id = "card_default",
                groupId = "",
                selectedSheet = "",
                fieldMappings = (0 until 19).map { idx ->
                    CardFieldMapping(
                        fieldIndex = idx,
                        titleItemReference = DEFAULT_CARD_FIELD_NAMES.getOrElse(idx) { "ITEM ${idx + 1}" },
                        valueItemReference = ""
                    )
                }
            )
        )
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


