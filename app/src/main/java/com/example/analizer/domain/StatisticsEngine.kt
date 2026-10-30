package com.example.analizer.domain

import com.example.analizer.model.AnalysisConfig
import com.example.analizer.model.ColumnStats
import com.example.analizer.model.SpreadsheetData
import kotlin.math.sqrt

class StatisticsEngine {

    fun calculateStats(
        spreadsheetData: SpreadsheetData,
        config: AnalysisConfig
    ): List<ColumnStats> {
        if (config.displayGroups.isNotEmpty()) {
            return calculateStatsForDisplayGroups(spreadsheetData, config)
        }
        if (config.intervals.isNotEmpty()) {
            return calculateStatsForIntervals(spreadsheetData, config)
        }

        val sheetName = if (config.sheetName.isNotBlank() && spreadsheetData.sheets.containsKey(config.sheetName)) {
            config.sheetName
        } else {
            spreadsheetData.sheetNames.firstOrNull() ?: spreadsheetData.sheets.keys.firstOrNull() ?: ""
        }
        val sheet = spreadsheetData.sheets[sheetName] ?: emptyList()

        return calculateStats(
            sheet = sheet,
            rowRange = config.rowRange,
            columnIndices = config.selectedColumnIndices,
            headerRowIndex = config.headerRowIndex,
            customColumnTitles = config.customColumnTitles
        )
    }

    fun calculateStatsForIntervals(
        spreadsheetData: SpreadsheetData,
        config: AnalysisConfig
    ): List<ColumnStats> {
        if (spreadsheetData.sheets.isEmpty() || config.intervals.isEmpty()) return emptyList()

        val defaultSheetName = if (config.sheetName.isNotBlank() && spreadsheetData.sheets.containsKey(config.sheetName)) {
            config.sheetName
        } else {
            spreadsheetData.sheetNames.firstOrNull() ?: spreadsheetData.sheets.keys.firstOrNull() ?: ""
        }

        val result = mutableListOf<ColumnStats>()

        for (interval in config.intervals) {
            val targetSheetName = if (!interval.sheetName.isNullOrBlank() && spreadsheetData.sheets.containsKey(interval.sheetName)) {
                interval.sheetName
            } else {
                defaultSheetName
            }
            val sheet = spreadsheetData.sheets[targetSheetName] ?: emptyList()
            val totalCols = sheet.maxOfOrNull { it.size } ?: 0
            val maxRowIdx = sheet.size - 1
            if (totalCols == 0) continue

            val startCol = interval.startColumn.coerceIn(0, maxOf(0, totalCols - 1))
            val endCol = interval.endColumn.coerceIn(0, maxOf(0, totalCols - 1))
            if (startCol > endCol) continue

            val startR = (interval.startRow ?: config.startRow).coerceIn(0, maxOf(0, maxRowIdx))
            val endR = (interval.endRow ?: config.endRow).coerceIn(0, maxOf(0, maxRowIdx))

            for (colIndex in startCol..endCol) {
                val baseColumnName = resolveColumnName(sheet, colIndex, config.headerRowIndex, config.customColumnTitles)
                val sheetPrefix = if (spreadsheetData.sheetNames.size > 1) "[$targetSheetName] " else ""
                val columnName = if (interval.name.isNotBlank()) {
                    if (startCol == endCol) "$sheetPrefix${interval.name}" else "$sheetPrefix${interval.name} - $baseColumnName"
                } else {
                    "$sheetPrefix$baseColumnName"
                }

                val numericValues = mutableListOf<Double>()
                val rawStringValues = mutableListOf<String>()

                if (startR <= endR) {
                    for (rowIndex in startR..endR) {
                        if (rowIndex == config.headerRowIndex) continue // skip header row
                        val row = sheet.getOrNull(rowIndex) ?: continue
                        val cellValue = row.getOrNull(colIndex)?.trim() ?: continue
                        if (cellValue.isNotEmpty()) {
                            rawStringValues.add(cellValue)
                            parseNumber(cellValue)?.let { num ->
                                numericValues.add(num)
                            }
                        }
                    }
                }

                result.add(
                    computeColumnStats(
                        columnName = columnName,
                        values = numericValues,
                        rawStringValues = rawStringValues,
                        columnIndex = colIndex,
                        metric = interval.metric,
                        sheetName = targetSheetName
                    )
                )
            }
        }

        return result
    }

    fun calculateStatsForIntervals(
        sheet: List<List<String>>,
        config: AnalysisConfig
    ): List<ColumnStats> {
        val dummyData = SpreadsheetData(
            sheetNames = listOf(config.sheetName.ifBlank { "Sheet1" }),
            sheets = mapOf(config.sheetName.ifBlank { "Sheet1" } to sheet)
        )
        return calculateStatsForIntervals(dummyData, config)
    }

    fun parseCellReference(cellRef: String): Pair<Int, Int>? {
        val trimmed = cellRef.trim().uppercase()
        val match = Regex("^([A-Z]+)(\\d+)$").find(trimmed) ?: return null
        val colLetters = match.groupValues[1]
        val rowDigits = match.groupValues[2]

        var col = 0
        for (c in colLetters) {
            col = col * 26 + (c - 'A' + 1)
        }
        val colIndex = maxOf(0, col - 1)
        val rowIndex = maxOf(0, (rowDigits.toIntOrNull() ?: 1) - 1)
        return Pair(colIndex, rowIndex)
    }

    fun resolveCellText(sheetRows: List<List<String>>, cellRef: String): String? {
        val coords = parseCellReference(cellRef) ?: return null
        val row = sheetRows.getOrNull(coords.second) ?: return null
        return row.getOrNull(coords.first)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun calculateStatsForDisplayGroups(
        spreadsheetData: SpreadsheetData,
        config: AnalysisConfig
    ): List<ColumnStats> {
        val groups = config.displayGroups.filter { it.enabledForAnalysis }.sortedBy { it.displayOrder }
        if (spreadsheetData.sheets.isEmpty() || groups.isEmpty()) return emptyList()

        val defaultSheetName = if (config.sheetName.isNotBlank() && spreadsheetData.sheets.containsKey(config.sheetName)) {
            config.sheetName
        } else {
            spreadsheetData.sheetNames.firstOrNull() ?: spreadsheetData.sheets.keys.firstOrNull() ?: ""
        }

        val result = mutableListOf<ColumnStats>()

        for ((groupIndex, group) in groups.withIndex()) {
            val targetSheets = if (group.selectedSheets.isNotEmpty()) {
                group.selectedSheets.filter { spreadsheetData.sheets.containsKey(it) }
            } else {
                listOf(defaultSheetName)
            }.ifEmpty { listOf(defaultSheetName) }

            val firstSheet = targetSheets.firstOrNull() ?: defaultSheetName
            val firstSheetRows = spreadsheetData.sheets[firstSheet] ?: emptyList()

            val resolvedGroupName = if (group.nameSourceType == "CELL") {
                val cellText = resolveCellText(firstSheetRows, group.cellReference)
                if (!cellText.isNullOrBlank()) cellText else group.name.ifBlank { "Grupo ${groupIndex + 1}" }
            } else {
                group.name.ifBlank { "Grupo ${groupIndex + 1}" }
            }

            for (sheetName in targetSheets) {
                val sheet = spreadsheetData.sheets[sheetName] ?: emptyList()
                val totalCols = sheet.maxOfOrNull { it.size } ?: 0
                val maxRowIdx = sheet.size - 1
                if (totalCols == 0) continue

                for (item in group.items) {
                    val startCol = item.startColumn.coerceIn(0, maxOf(0, totalCols - 1))
                    val endCol = item.endColumn.coerceIn(0, maxOf(0, totalCols - 1))
                    if (startCol > endCol) continue

                    val startR = (item.startRow ?: config.startRow).coerceIn(0, maxOf(0, maxRowIdx))
                    val endR = (item.endRow ?: config.endRow).coerceIn(0, maxOf(0, maxRowIdx))

                    for (colIndex in startCol..endCol) {
                        val baseColName = resolveColumnName(sheet, colIndex, config.headerRowIndex, config.customColumnTitles)
                        val sheetPrefix = if (spreadsheetData.sheetNames.size > 1) "[$sheetName] " else ""
                        val itemPrefix = if (item.customLabel.isNotBlank()) "${item.customLabel}: " else ""
                        val columnName = "$sheetPrefix$resolvedGroupName - $itemPrefix$baseColName"

                        val numericValues = mutableListOf<Double>()
                        val rawStringValues = mutableListOf<String>()

                        if (item.metric == "Valor da Célula") {
                            val row = sheet.getOrNull(startR)
                            val cellVal = row?.getOrNull(colIndex)?.trim() ?: ""
                            if (cellVal.isNotEmpty()) {
                                rawStringValues.add(cellVal)
                                parseNumber(cellVal)?.let { numericValues.add(it) }
                            }
                        } else {
                            if (startR <= endR) {
                                for (rowIndex in startR..endR) {
                                    if (rowIndex == config.headerRowIndex) continue
                                    val row = sheet.getOrNull(rowIndex) ?: continue
                                    val cellVal = row.getOrNull(colIndex)?.trim() ?: continue
                                    if (cellVal.isNotEmpty()) {
                                        rawStringValues.add(cellVal)
                                        parseNumber(cellVal)?.let { numericValues.add(it) }
                                    }
                                }
                            }
                        }

                        result.add(
                            computeColumnStats(
                                columnName = columnName,
                                values = numericValues,
                                rawStringValues = rawStringValues,
                                columnIndex = colIndex,
                                metric = item.metric,
                                sheetName = sheetName,
                                groupName = resolvedGroupName
                            )
                        )
                    }
                }
            }
        }

        return result
    }

    fun calculateStats(
        sheet: List<List<String>>,
        rowRange: IntRange,
        columnIndices: List<Int>,
        headerRowIndex: Int = 0,
        customColumnTitles: Map<Int, String> = emptyMap()
    ): List<ColumnStats> {
        if (sheet.isEmpty() || columnIndices.isEmpty()) return emptyList()

        return columnIndices.map { colIndex ->
            val columnName = resolveColumnName(sheet, colIndex, headerRowIndex, customColumnTitles)
            val numericValues = mutableListOf<Double>()
            val rawStringValues = mutableListOf<String>()

            val start = maxOf(0, rowRange.first)
            val end = minOf(sheet.size - 1, rowRange.last)

            if (start <= end) {
                for (rowIndex in start..end) {
                    if (rowIndex == headerRowIndex) continue // skip header row
                    val row = sheet.getOrNull(rowIndex) ?: continue
                    val cellValue = row.getOrNull(colIndex)?.trim() ?: continue
                    if (cellValue.isNotEmpty()) {
                        rawStringValues.add(cellValue)
                        parseNumber(cellValue)?.let { num ->
                            numericValues.add(num)
                        }
                    }
                }
            }

            computeColumnStats(columnName, numericValues, rawStringValues, colIndex)
        }
    }

    internal fun resolveColumnName(
        sheet: List<List<String>>,
        colIndex: Int,
        headerRowIndex: Int,
        customColumnTitles: Map<Int, String>
    ): String {
        customColumnTitles[colIndex]?.let { if (it.isNotBlank()) return it }
        val headerRow = sheet.getOrNull(headerRowIndex)
        val headerCell = headerRow?.getOrNull(colIndex)?.trim()
        if (!headerCell.isNullOrBlank()) {
            return headerCell
        }
        return getExcelColumnName(colIndex)
    }

    internal fun getExcelColumnName(colIndex: Int): String {
        var n = colIndex
        val sb = StringBuilder()
        while (n >= 0) {
            sb.append(('A'.code + (n % 26)).toChar())
            n = (n / 26) - 1
        }
        return sb.reverse().toString()
    }

    internal fun parseNumber(input: String): Double? {
        if (input.isBlank()) return null
        val cleaned = input
            .replace(Regex("[$€£¥₹%\\s]"), "")
            .trim()

        if (cleaned.isEmpty()) return null

        cleaned.toDoubleOrNull()?.let { return it }

        val hasComma = cleaned.contains(',')
        val hasDot = cleaned.contains('.')

        return when {
            hasComma && hasDot -> {
                val lastComma = cleaned.lastIndexOf(',')
                val lastDot = cleaned.lastIndexOf('.')
                if (lastComma > lastDot) {
                    cleaned.replace(".", "").replace(',', '.').toDoubleOrNull()
                } else {
                    cleaned.replace(",", "").toDoubleOrNull()
                }
            }
            hasComma && !hasDot -> {
                val parts = cleaned.split(',')
                if (parts.size == 2 && parts[1].length <= 3) {
                    "${parts[0]}.${parts[1]}".toDoubleOrNull()
                } else {
                    cleaned.replace(",", "").toDoubleOrNull()
                }
            }
            else -> cleaned.toDoubleOrNull()
        }
    }

    private fun computeColumnStats(
        columnName: String,
        values: List<Double>,
        rawStringValues: List<String>,
        columnIndex: Int = 0,
        metric: String = "Média",
        sheetName: String = "",
        groupName: String = ""
    ): ColumnStats {
        val count = values.size.toLong()
        if (count == 0L) {
            return ColumnStats(
                columnName = columnName,
                count = 0L,
                sum = 0.0,
                mean = 0.0,
                min = 0.0,
                max = 0.0,
                median = 0.0,
                stdDev = 0.0,
                rawValues = emptyList(),
                rawStringValues = rawStringValues,
                columnIndex = columnIndex,
                metric = metric,
                sheetName = sheetName,
                groupName = groupName
            )
        }

        val sum = values.sum()
        val mean = sum / count
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0

        val sorted = values.sorted()
        val median = if (count % 2L == 1L) {
            sorted[(count / 2L).toInt()]
        } else {
            val mid = (count / 2L).toInt()
            (sorted[mid - 1] + sorted[mid]) / 2.0
        }

        val stdDev = if (count > 1L) {
            val variance = values.sumOf { (it - mean) * (it - mean) } / (count - 1.0)
            sqrt(variance)
        } else {
            0.0
        }

        return ColumnStats(
            columnName = columnName,
            count = count,
            sum = sum,
            mean = mean,
            min = min,
            max = max,
            median = median,
            stdDev = stdDev,
            rawValues = values,
            rawStringValues = rawStringValues,
            columnIndex = columnIndex,
            metric = metric,
            sheetName = sheetName,
            groupName = groupName
        )
    }
}
