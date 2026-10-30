package com.example.analizer.domain

import com.example.analizer.model.AnalysisConfig
import com.example.analizer.model.ColumnRangeInterval
import com.example.analizer.model.SpreadsheetData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StatisticsEngineTest {

    private val engine = StatisticsEngine()

    @Test
    fun testParseNumber() {
        assertEquals(123.45, engine.parseNumber("123.45"))
        assertEquals(1234.56, engine.parseNumber("1,234.56"))
        assertEquals(1234.56, engine.parseNumber("1.234,56"))
        assertEquals(12.5, engine.parseNumber("12,5"))
        assertEquals(100.0, engine.parseNumber("$100.00"))
        assertEquals(50.0, engine.parseNumber(" 50% "))
        assertEquals(-45.6, engine.parseNumber("-45.6"))
        assertEquals(null, engine.parseNumber("abc"))
        assertEquals(null, engine.parseNumber(""))
    }

    @Test
    fun testResolveColumnName() {
        val sheet = listOf(
            listOf("ID", "Product Name", "Price"),
            listOf("1", "Apple", "1.50"),
            listOf("2", "Banana", "0.75")
        )

        assertEquals("Product Name", engine.resolveColumnName(sheet, 1, 0, emptyMap()))
        assertEquals("CustomName", engine.resolveColumnName(sheet, 1, 0, mapOf(1 to "CustomName")))
        assertEquals("D", engine.resolveColumnName(sheet, 3, 0, emptyMap()))
    }

    @Test
    fun testCalculateStats() {
        val sheet = listOf(
            listOf("Category", "Score"),
            listOf("A", "10"),
            listOf("B", "20"),
            listOf("C", "30"),
            listOf("D", "invalid"),
            listOf("E", "40")
        )

        val spreadsheetData = SpreadsheetData(
            sheetNames = listOf("Sheet1"),
            sheets = mapOf("Sheet1" to sheet)
        )

        val config = AnalysisConfig(
            sheetName = "Sheet1",
            headerRowIndex = 0,
            selectedColumnIndices = listOf(1),
            startRow = 1,
            endRow = 5
        )

        val statsList = engine.calculateStats(spreadsheetData, config)
        assertEquals(1, statsList.size)
        val stats = statsList[0]

        assertEquals("Score", stats.columnName)
        assertEquals(1, stats.columnIndex)
        assertEquals(4L, stats.count) // 10, 20, 30, 40 (invalid is ignored)
        assertEquals(100.0, stats.sum, 0.001)
        assertEquals(25.0, stats.mean, 0.001)
        assertEquals(10.0, stats.min, 0.001)
        assertEquals(40.0, stats.max, 0.001)
        assertEquals(25.0, stats.median, 0.001) // median of [10, 20, 30, 40] is (20+30)/2 = 25
        assertNotNull(stats.stdDev)
    }

    @Test
    fun testColumnMetricsInConfig() {
        val config = AnalysisConfig(
            columnMetrics = mapOf(0 to "Soma", 1 to "Mediana")
        )

        assertEquals("Soma", config.getMetricForColumn(0))
        assertEquals("Mediana", config.getMetricForColumn(1))
        assertEquals("Média", config.getMetricForColumn(2)) // Default
    }

    @Test
    fun testCalculateStatsForIntervals() {
        val sheet = listOf(
            listOf("Item", "Val1", "Val2", "Val3"), // row 0: headers
            listOf("A", "10", "100", "1000"),       // row 1
            listOf("B", "20", "200", "2000"),       // row 2
            listOf("C", "30", "300", "3000"),       // row 3
            listOf("D", "40", "400", "4000")        // row 4
        )

        val spreadsheetData = SpreadsheetData(
            sheetNames = listOf("Sheet1"),
            sheets = mapOf("Sheet1" to sheet)
        )

        val interval1 = ColumnRangeInterval(
            name = "Group1",
            startColumn = 1,
            endColumn = 2,
            startRow = 1,
            endRow = 2,
            metric = "Soma"
        )
        val interval2 = ColumnRangeInterval(
            name = "Group2",
            startColumn = 3,
            endColumn = 3,
            startRow = null,
            endRow = null,
            metric = "Máximo"
        )

        val config = AnalysisConfig(
            sheetName = "Sheet1",
            headerRowIndex = 0,
            startRow = 1,
            endRow = 4,
            intervals = listOf(interval1, interval2)
        )

        val statsList = engine.calculateStats(spreadsheetData, config)
        assertEquals(3, statsList.size)

        // Stat 0: Group1 - Val1 (rows 1..2: values 10, 20)
        assertEquals("Group1 - Val1", statsList[0].columnName)
        assertEquals(2L, statsList[0].count)
        assertEquals(30.0, statsList[0].sum, 0.001)

        // Stat 1: Group1 - Val2 (rows 1..2: values 100, 200)
        assertEquals("Group1 - Val2", statsList[1].columnName)
        assertEquals(2L, statsList[1].count)
        assertEquals(300.0, statsList[1].sum, 0.001)

        // Stat 2: Group2 (col 3: values 1000, 2000, 3000, 4000)
        assertEquals("Group2", statsList[2].columnName)
        assertEquals(4L, statsList[2].count)
        assertEquals(4000.0, statsList[2].max, 0.001)
    }

    @Test
    fun testIntervalMetricInConfig() {
        val interval = ColumnRangeInterval(
            startColumn = 1,
            endColumn = 3,
            metric = "Desvio Padrão"
        )
        val config = AnalysisConfig(
            intervals = listOf(interval)
        )

        assertEquals("Média", config.getMetricForColumn(0))
        assertEquals("Desvio Padrão", config.getMetricForColumn(1))
        assertEquals("Desvio Padrão", config.getMetricForColumn(2))
        assertEquals("Desvio Padrão", config.getMetricForColumn(3))
        assertEquals("Média", config.getMetricForColumn(4))
    }

    @Test
    fun testMultiSheetIntervals() {
        val sheetA = listOf(
            listOf("Item", "ValA"),
            listOf("1", "10"),
            listOf("2", "20")
        )
        val sheetB = listOf(
            listOf("Item", "ValB"),
            listOf("1", "100"),
            listOf("2", "200")
        )

        val spreadsheetData = SpreadsheetData(
            sheetNames = listOf("Aba1", "Aba2"),
            sheets = mapOf("Aba1" to sheetA, "Aba2" to sheetB)
        )

        val interval1 = ColumnRangeInterval(
            name = "Int1",
            sheetName = "Aba1",
            startColumn = 1,
            endColumn = 1,
            metric = "Soma"
        )
        val interval2 = ColumnRangeInterval(
            name = "Int2",
            sheetName = "Aba2",
            startColumn = 1,
            endColumn = 1,
            metric = "Média"
        )

        val config = AnalysisConfig(
            sheetName = "Aba1",
            headerRowIndex = 0,
            startRow = 1,
            endRow = 2,
            intervals = listOf(interval1, interval2)
        )

        val stats = engine.calculateStats(spreadsheetData, config)
        assertEquals(2, stats.size)
        assertEquals("[Aba1] Int1", stats[0].columnName)
        assertEquals("Aba1", stats[0].sheetName)
        assertEquals(30.0, stats[0].sum, 0.001)

        assertEquals("[Aba2] Int2", stats[1].columnName)
        assertEquals("Aba2", stats[1].sheetName)
        assertEquals(150.0, stats[1].mean, 0.001)
    }
}
