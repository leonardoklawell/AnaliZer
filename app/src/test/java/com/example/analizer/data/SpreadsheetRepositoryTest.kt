package com.example.analizer.data

import kotlinx.coroutines.test.runTest
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SpreadsheetRepositoryTest {

    private val repository = SpreadsheetRepository()

    @Test
    fun testTransformGoogleSheetUrl() {
        val shareLink = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit?usp=sharing"
        val expected = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/export?format=xlsx"
        assertEquals(expected, repository.transformGoogleSheetUrl(shareLink))

        val linkWithGid = "https://docs.google.com/spreadsheets/d/1ABC123/edit#gid=12345"
        val expectedWithGid = "https://docs.google.com/spreadsheets/d/1ABC123/export?format=csv&gid=12345"
        assertEquals(expectedWithGid, repository.transformGoogleSheetCsvUrl(linkWithGid))

        val linkWithQueryGid = "https://docs.google.com/spreadsheets/d/1iJutQlIzSyzJkOCXYNxnSP1FUTXLHd0TTCmxMtvvkeQ/edit?gid=209726842#gid=209726842"
        val expectedWithQueryGid = "https://docs.google.com/spreadsheets/d/1iJutQlIzSyzJkOCXYNxnSP1FUTXLHd0TTCmxMtvvkeQ/export?format=csv&gid=209726842"
        assertEquals(expectedWithQueryGid, repository.transformGoogleSheetCsvUrl(linkWithQueryGid))
    }

    @Test
    fun testParseCsv() = runTest {
        val csvContent = "Name,Age,Score\nAlice,30,95.5\nBob,25,88.0"
        val inputStream = ByteArrayInputStream(csvContent.toByteArray(Charsets.UTF_8))

        val result = repository.parseCsv(inputStream, "TestSheet")
        assertEquals(listOf("TestSheet"), result.sheetNames)
        assertTrue(result.sheets.containsKey("TestSheet"))
        val rows = result.sheets["TestSheet"]!!
        assertEquals(3, rows.size)
        assertEquals(listOf("Name", "Age", "Score"), rows[0])
        assertEquals(listOf("Alice", "30", "95.5"), rows[1])
    }

    @Test
    fun testParseExcel() = runTest {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Sheet1")
        val row0 = sheet.createRow(0)
        row0.createCell(0).setCellValue("Header1")
        row0.createCell(1).setCellValue(123.0)

        val row1 = sheet.createRow(1)
        row1.createCell(0).setCellValue("Value1")
        row1.createCell(1).setCellValue(456.0)

        val bos = ByteArrayOutputStream()
        workbook.write(bos)
        workbook.close()

        val inputStream = ByteArrayInputStream(bos.toByteArray())
        val result = repository.parseExcel(inputStream)

        assertEquals(listOf("Sheet1"), result.sheetNames)
        val rows = result.sheets["Sheet1"]!!
        assertEquals(2, rows.size)
        assertEquals("Header1", rows[0][0])
        assertEquals("123", rows[0][1])
        assertEquals("Value1", rows[1][0])
        assertEquals("456", rows[1][1])
    }
}
