package com.example.analizer.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.analizer.model.SpreadsheetData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.csv.CSVFormat
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal

class SpreadsheetRepository(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(java.time.Duration.ofSeconds(30))
        .readTimeout(java.time.Duration.ofSeconds(60))
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    suspend fun parseExcel(inputStream: InputStream): SpreadsheetData = withContext(Dispatchers.IO) {
        val workbook = WorkbookFactory.create(inputStream)
        val sheetNames = mutableListOf<String>()
        val sheets = mutableMapOf<String, List<List<String>>>()

        try {
            for (i in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(i)
                val sheetName = sheet.sheetName ?: "Sheet${i + 1}"
                sheetNames.add(sheetName)

                val rowsList = mutableListOf<List<String>>()
                val firstRowNum = sheet.firstRowNum
                val lastRowNum = sheet.lastRowNum

                if (firstRowNum >= 0 && lastRowNum >= 0) {
                    for (rowIndex in firstRowNum..lastRowNum) {
                        val row = sheet.getRow(rowIndex)
                        val rowValues = mutableListOf<String>()
                        if (row != null) {
                            val lastCellNum = row.lastCellNum.toInt()
                            for (colIndex in 0 until maxOf(0, lastCellNum)) {
                                val cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
                                val cellValue = if (cell == null) {
                                    ""
                                } else {
                                    when (cell.cellType) {
                                        CellType.STRING -> cell.stringCellValue ?: ""
                                        CellType.NUMERIC -> {
                                            if (DateUtil.isCellDateFormatted(cell)) {
                                                cell.dateCellValue?.toString() ?: ""
                                            } else {
                                                try {
                                                    BigDecimal.valueOf(cell.numericCellValue).stripTrailingZeros().toPlainString()
                                                } catch (_: Exception) {
                                                    cell.numericCellValue.toString()
                                                }
                                            }
                                        }
                                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                                        CellType.FORMULA -> {
                                            try {
                                                when (cell.cachedFormulaResultType) {
                                                    CellType.STRING -> cell.stringCellValue ?: ""
                                                    CellType.NUMERIC -> BigDecimal.valueOf(cell.numericCellValue).stripTrailingZeros().toPlainString()
                                                    CellType.BOOLEAN -> cell.booleanCellValue.toString()
                                                    else -> cell.cellFormula ?: ""
                                                }
                                            } catch (_: Exception) {
                                                cell.cellFormula ?: ""
                                            }
                                        }
                                        else -> ""
                                    }
                                }
                                rowValues.add(cellValue)
                            }
                        }
                        rowsList.add(rowValues)
                    }
                }
                sheets[sheetName] = rowsList
            }
        } finally {
            workbook.close()
        }

        SpreadsheetData(
            sheetNames = sheetNames,
            sheets = sheets
        )
    }

    suspend fun parseCsv(inputStream: InputStream, sheetName: String = "Sheet1"): SpreadsheetData = withContext(Dispatchers.IO) {
        val reader = inputStream.bufferedReader(Charsets.UTF_8)
        val parser = CSVFormat.DEFAULT
            .parse(reader)

        val rowsList = mutableListOf<List<String>>()
        for (record in parser) {
            val rowValues = record.map { it?.trim() ?: "" }
            rowsList.add(rowValues)
        }
        parser.close()

        SpreadsheetData(
            sheetNames = listOf(sheetName),
            sheets = mapOf(sheetName to rowsList)
        )
    }

    suspend fun fetchGoogleSheet(urlOrShareLink: String): Result<SpreadsheetData> = withContext(Dispatchers.IO) {
        val trimmed = urlOrShareLink.trim()
        val xlsxUrl = transformGoogleSheetUrl(trimmed)
        val xlsxResult = fetchUrl(xlsxUrl)
        if (xlsxResult.isSuccess) {
            return@withContext xlsxResult
        }
        val csvUrl = transformGoogleSheetCsvUrl(trimmed)
        if (csvUrl != xlsxUrl) {
            val csvResult = fetchUrl(csvUrl)
            if (csvResult.isSuccess) {
                return@withContext csvResult
            }
        }
        xlsxResult
    }

    private suspend fun fetchUrl(targetUrl: String): Result<SpreadsheetData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to fetch URL: ${response.code} ${response.message}"))
                }
                val body = response.body
                    ?: return@withContext Result.failure(IOException("Response body is null"))

                val bytes = body.bytes()
                val contentType = response.header("Content-Type", "").orEmpty()
                val isExcel = targetUrl.contains("format=xlsx") ||
                              targetUrl.endsWith(".xlsx", ignoreCase = true) ||
                              targetUrl.endsWith(".xls", ignoreCase = true) ||
                              contentType.contains("spreadsheet") ||
                              contentType.contains("excel") ||
                              contentType.contains("application/octet-stream")

                if (isExcel) {
                    try {
                        val excelData = parseExcel(ByteArrayInputStream(bytes))
                        return@withContext Result.success(excelData)
                    } catch (e: Throwable) {
                        try {
                            val csvData = parseCsv(ByteArrayInputStream(bytes), "Sheet1")
                            return@withContext Result.success(csvData)
                        } catch (_: Throwable) {
                            return@withContext Result.failure(e)
                        }
                    }
                } else {
                    try {
                        val csvData = parseCsv(ByteArrayInputStream(bytes), "Sheet1")
                        return@withContext Result.success(csvData)
                    } catch (e: Throwable) {
                        try {
                            val excelData = parseExcel(ByteArrayInputStream(bytes))
                            return@withContext Result.success(excelData)
                        } catch (_: Throwable) {
                            return@withContext Result.failure(e)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun loadSpreadsheetFromUri(context: Context, uri: Uri): Result<SpreadsheetData> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            val fileName = getFileName(context, uri)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val isExcel = fileName.endsWith(".xlsx", ignoreCase = true) ||
                              fileName.endsWith(".xls", ignoreCase = true) ||
                              mimeType?.contains("spreadsheet") == true ||
                              mimeType?.contains("excel") == true ||
                              mimeType?.contains("openxmlformats") == true

                if (isExcel) {
                    try {
                        val data = parseExcel(ByteArrayInputStream(bytes))
                        return@withContext Result.success(data)
                    } catch (e: Throwable) {
                        try {
                            val data = parseCsv(ByteArrayInputStream(bytes), "Sheet1")
                            return@withContext Result.success(data)
                        } catch (_: Throwable) {
                            return@withContext Result.failure(e)
                        }
                    }
                } else {
                    try {
                        val data = parseCsv(ByteArrayInputStream(bytes), "Sheet1")
                        return@withContext Result.success(data)
                    } catch (e: Throwable) {
                        try {
                            val data = parseExcel(ByteArrayInputStream(bytes))
                            return@withContext Result.success(data)
                        } catch (_: Throwable) {
                            return@withContext Result.failure(e)
                        }
                    }
                }
            } ?: Result.failure(IOException("Could not open InputStream for Uri: $uri"))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun streamSpreadsheetFromUri(context: Context, uri: Uri): Flow<Result<SpreadsheetData>> = flow {
        emit(loadSpreadsheetFromUri(context, uri))
    }.flowOn(Dispatchers.IO)

    fun streamGoogleSheet(urlOrShareLink: String): Flow<Result<SpreadsheetData>> = flow {
        emit(fetchGoogleSheet(urlOrShareLink))
    }.flowOn(Dispatchers.IO)

    internal fun transformGoogleSheetUrl(url: String): String {
        if (url.contains("docs.google.com/spreadsheets")) {
            val regex = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)")
            val match = regex.find(url)
            if (match != null) {
                val spreadsheetId = match.groups[1]?.value
                if (spreadsheetId != null) {
                    return "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=xlsx"
                }
            }
        }
        return url
    }

    internal fun transformGoogleSheetCsvUrl(url: String): String {
        if (url.contains("docs.google.com/spreadsheets")) {
            val regex = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)")
            val match = regex.find(url)
            if (match != null) {
                val spreadsheetId = match.groups[1]?.value
                if (spreadsheetId != null) {
                    val gidRegex = Regex("[?#&]gid=([0-9]+)")
                    val gidMatch = gidRegex.find(url)
                    val gidParam = if (gidMatch != null) "&gid=${gidMatch.groups[1]?.value}" else ""
                    return "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv$gidParam"
                }
            }
        }
        return url
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "document.xlsx"
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            cursor.getString(index)?.let { name = it }
                        }
                    }
                }
            } catch (_: Exception) {
                // ignore
            }
        } else {
            uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) {
                    name = path.substring(cut + 1)
                }
            }
        }
        return name
    }
}
