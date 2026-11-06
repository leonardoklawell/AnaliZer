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
        get() = if (folders.isNotEmpty()) {
            folders.map {
                it.folderName
                    .replace(Regex("\\.(xlsx|xls|csv|gsheet)$", RegexOption.IGNORE_CASE), "")
                    .substringBefore(" - ")
                    .substringAfterLast("/")
                    .trim()
            }.filter { it.isNotBlank() }.distinct()
        } else emptyList()

    val effectiveSheetNames: List<String>
        get() {
            val keysClean = sheets.keys.map { it.replace(Regex("^\\[.*?\\]\\s*"), "") }.distinct()
            return (sheetNames + keysClean + sheets.keys).distinct().filter { it.isNotBlank() }
        }

    fun getSheetsForFolder(folderName: String): Map<String, List<List<String>>> {
        val clean = folderName
            .replace(Regex("\\.(xlsx|xls|csv|gsheet)$", RegexOption.IGNORE_CASE), "")
            .substringBefore(" - ")
            .substringAfterLast("/")
            .trim()
        val matches = folders.filter {
            it.folderName == folderName ||
            it.folderName
                .replace(Regex("\\.(xlsx|xls|csv|gsheet)$", RegexOption.IGNORE_CASE), "")
                .substringBefore(" - ")
                .substringAfterLast("/")
                .trim()
                .equals(clean, ignoreCase = true)
        }
        if (matches.isNotEmpty()) {
            val combined = mutableMapOf<String, List<List<String>>>()
            for (m in matches) {
                combined.putAll(m.sheets)
            }
            return combined
        }
        return if (folderName.isBlank()) sheets else emptyMap()
    }

    fun getSheetNamesForFolder(folderName: String): List<String> {
        val clean = folderName
            .replace(Regex("\\.(xlsx|xls|csv|gsheet)$", RegexOption.IGNORE_CASE), "")
            .substringBefore(" - ")
            .substringAfterLast("/")
            .trim()
        val matches = folders.filter {
            it.folderName == folderName ||
            it.folderName
                .replace(Regex("\\.(xlsx|xls|csv|gsheet)$", RegexOption.IGNORE_CASE), "")
                .substringBefore(" - ")
                .substringAfterLast("/")
                .trim()
                .equals(clean, ignoreCase = true)
        }
        if (matches.isNotEmpty()) {
            val names = mutableListOf<String>()
            for (m in matches) {
                names.addAll(m.sheetNames)
                names.addAll(m.sheets.keys)
            }
            return names.distinct().filter { it.isNotBlank() }
        }
        return effectiveSheetNames
    }

    fun getRows(folderName: String, sheetName: String): List<List<String>> {
        val folderSheets = getSheetsForFolder(folderName)
        val cleanFolder = folderName
            .replace(Regex("\\.(xlsx|xls|csv|gsheet)$", RegexOption.IGNORE_CASE), "")
            .substringBefore(" - ")
            .substringAfterLast("/")
            .trim()
        return folderSheets[sheetName]
            ?: sheets["[$folderName] $sheetName"]
            ?: sheets["[$cleanFolder] $sheetName"]
            ?: sheets[sheetName]
            ?: emptyList()
    }
}
