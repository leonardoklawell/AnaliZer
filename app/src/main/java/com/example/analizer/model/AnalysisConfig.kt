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

val DEFAULT_CARD_FIELD_NAMES = listOf(
    "Média",
    "Mediana",
    "Desv. Padrão",
    "Mín / Máx",
    "Soma",
    "Contagem",
    "Campo 7",
    "Campo 8",
    "Campo 9",
    "Campo 10",
    "Campo 11",
    "Campo 12",
    "Campo 13",
    "Campo 14"
)

@Serializable
data class CardFieldMapping(
    val id: String = UUID.randomUUID().toString(),
    val fieldIndex: Int = 0,
    val titleItemReference: String = "",
    val valueItemReference: String = "none" // Default unconfigured ("--")
)

@Serializable
data class CardConfig(
    val id: String = UUID.randomUUID().toString(),
    val groupId: String = "",
    val fieldMappings: List<CardFieldMapping> = (0 until 14).map { idx ->
        CardFieldMapping(
            fieldIndex = idx,
            titleItemReference = DEFAULT_CARD_FIELD_NAMES.getOrElse(idx) { "Campo ${idx + 1}" },
            valueItemReference = "none"
        )
    }
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
    val cards: List<CardConfig> = emptyList()
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
                    fieldMappings = (0 until 14).map { idx ->
                        CardFieldMapping(
                            fieldIndex = idx,
                            titleItemReference = DEFAULT_CARD_FIELD_NAMES.getOrElse(idx) { "Campo ${idx + 1}" },
                            valueItemReference = "none"
                        )
                    }
                )
            }
        }
        return listOf(
            CardConfig(
                id = "card_default",
                groupId = "",
                fieldMappings = (0 until 14).map { idx ->
                    CardFieldMapping(
                        fieldIndex = idx,
                        titleItemReference = DEFAULT_CARD_FIELD_NAMES.getOrElse(idx) { "Campo ${idx + 1}" },
                        valueItemReference = "none"
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
