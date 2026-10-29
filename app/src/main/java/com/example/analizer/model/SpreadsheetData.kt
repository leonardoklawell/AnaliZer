package com.example.analizer.model

import kotlinx.serialization.Serializable

@Serializable
data class SpreadsheetData(
    val sheetNames: List<String> = emptyList(),
    val sheets: Map<String, List<List<String>>> = emptyMap()
) {
    val rawData: Map<String, List<List<String>>> get() = sheets
}
