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

    val effectiveSheetNames: List<String>
        get() {
            val keysClean = sheets.keys.map { it.replace(Regex("^\\[.*?\\]\\s*"), "") }.distinct()
            return (sheetNames + keysClean + sheets.keys).distinct().filter { it.isNotBlank() }
        }

    fun getSheetsForFolder(folderName: String): Map<String, List<List<String>>> {
        val match = folders.find { it.folderName == folderName }
        return match?.sheets ?: if (folderName.isBlank()) sheets else emptyMap()
    }

    fun getSheetNamesForFolder(folderName: String): List<String> {
        val match = folders.find { it.folderName == folderName }
        if (match != null) {
            val keys = match.sheets.keys.toList()
            return (match.sheetNames + keys).distinct().filter { it.isNotBlank() }
        }
        return effectiveSheetNames
    }

    fun getRows(folderName: String, sheetName: String): List<List<String>> {
        val folderSheets = getSheetsForFolder(folderName)
        return folderSheets[sheetName]
            ?: sheets["[$folderName] $sheetName"]
            ?: sheets[sheetName]
            ?: emptyList()
    }
}
