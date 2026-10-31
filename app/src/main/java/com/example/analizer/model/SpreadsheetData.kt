package com.example.analizer.model

import kotlinx.serialization.Serializable

@Serializable
data class FolderSpreadsheet(
    val folderName: String,
    val spreadsheetName: String = "",
    val sheetNames: List<String> = emptyList(),
    val sheets: Map<String, List<List<String>>> = emptyMap()
)

@Serializable
data class SpreadsheetData(
    val sheetNames: List<String> = emptyList(),
    val sheets: Map<String, List<List<String>>> = emptyMap(),
    val folders: List<FolderSpreadsheet> = emptyList(),
    val currentFolderName: String = ""
) {
    val rawData: Map<String, List<List<String>>> get() = sheets

    val folderNames: List<String>
        get() = if (folders.isNotEmpty()) folders.map { it.folderName }.distinct() else emptyList()

    fun getSheetsForFolder(folderName: String): Map<String, List<List<String>>> {
        val match = folders.find { it.folderName == folderName }
        return match?.sheets ?: if (folderName.isBlank()) sheets else emptyMap()
    }

    fun getSheetNamesForFolder(folderName: String): List<String> {
        val match = folders.find { it.folderName == folderName }
        return match?.sheetNames ?: if (folderName.isBlank()) sheetNames else emptyList()
    }

    fun getRows(folderName: String, sheetName: String): List<List<String>> {
        val folderSheets = getSheetsForFolder(folderName)
        return folderSheets[sheetName] ?: sheets[sheetName] ?: emptyList()
    }
}
