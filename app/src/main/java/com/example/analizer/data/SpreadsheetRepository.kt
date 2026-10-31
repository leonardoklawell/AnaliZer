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

    suspend fun fetchGoogleSheet(
        urlOrShareLink: String,
        targetSpreadsheetName: String = "",
        authToken: String = ""
    ): Result<SpreadsheetData> = withContext(Dispatchers.IO) {
        val trimmed = urlOrShareLink.trim()
        if (trimmed.contains("/folders/") || trimmed.contains("drive.google.com/drive")) {
            return@withContext searchGoogleDriveFolder(trimmed, targetSpreadsheetName, authToken = authToken)
        }
        val xlsxUrl = transformGoogleSheetUrl(trimmed)
        val xlsxResult = fetchUrl(xlsxUrl, authToken = authToken)
        if (xlsxResult.isSuccess) {
            return@withContext xlsxResult
        }
        val csvUrl = transformGoogleSheetCsvUrl(trimmed)
        if (csvUrl != xlsxUrl) {
            val csvResult = fetchUrl(csvUrl, authToken = authToken)
            if (csvResult.isSuccess) {
                return@withContext csvResult
            }
        }
        xlsxResult
    }

    private suspend fun fetchUrl(targetUrl: String, authToken: String = ""): Result<SpreadsheetData> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            if (authToken.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $authToken")
            }
            val request = requestBuilder.build()
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

    fun streamGoogleSheet(urlOrShareLink: String, authToken: String = ""): Flow<Result<SpreadsheetData>> = flow {
        emit(fetchGoogleSheet(urlOrShareLink, authToken = authToken))
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

    suspend fun searchGoogleDriveFolder(
        folderUrlOrId: String,
        targetSpreadsheetName: String,
        apiKey: String = "AIzaSyBt7RMey2FcEHq55jizwcEfT_ryqMEUit0",
        authToken: String = ""
    ): Result<SpreadsheetData> = withContext(Dispatchers.IO) {
        val folderId = extractFolderId(folderUrlOrId)
        if (folderId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("ID ou link da pasta inválido."))
        }

        try {
            val rootName = getDriveItemName(folderId, apiKey, authToken) ?: "Pasta Principal"
            val queue = ArrayDeque<Pair<String, String>>()
            queue.add(Pair(folderId, rootName))

            val foundSpreadsheets = mutableListOf<com.example.analizer.model.FolderSpreadsheet>()
            val visited = mutableSetOf<String>()

            while (queue.isNotEmpty() && visited.size < 60) {
                val (currentFid, currentFname) = queue.removeFirst()
                if (!visited.add(currentFid)) continue

                val children = listDriveFolderChildren(currentFid, apiKey, authToken)
                for (child in children) {
                    if (child.isFolder) {
                        queue.add(Pair(child.id, child.name))
                    } else if (child.isSpreadsheet) {
                        val matches = targetSpreadsheetName.isBlank() ||
                                child.name.contains(targetSpreadsheetName.trim(), ignoreCase = true)
                        if (matches) {
                            val parsed = fetchDriveSpreadsheetData(child.id, child.mimeType, apiKey, authToken)
                            if (parsed != null && parsed.sheetNames.isNotEmpty()) {
                                foundSpreadsheets.add(
                                    com.example.analizer.model.FolderSpreadsheet(
                                        folderName = currentFname,
                                        spreadsheetName = child.name,
                                        sheetNames = parsed.sheetNames,
                                        sheets = parsed.sheets
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (foundSpreadsheets.isEmpty()) {
                val errorMsg = if (targetSpreadsheetName.isNotBlank()) {
                    "Nenhuma planilha contendo '$targetSpreadsheetName' foi encontrada na pasta e subpastas. Certifique-se de que a pasta foi compartilhada com a sua conta do Google ou possui permissão de leitura."
                } else {
                    "Nenhuma planilha foi encontrada na pasta do Google Drive informada. Certifique-se de que a pasta foi compartilhada com o seu e-mail do Google ou possui permissão de leitura."
                }
                return@withContext Result.failure(java.io.IOException(errorMsg))
            }

            val allSheets = mutableMapOf<String, List<List<String>>>()
            val allSheetNames = mutableListOf<String>()

            for (fs in foundSpreadsheets) {
                for ((sName, sRows) in fs.sheets) {
                    val qualified = "[${fs.folderName}] $sName"
                    allSheets[qualified] = sRows
                    if (!allSheets.containsKey(sName)) {
                        allSheets[sName] = sRows
                    }
                    if (!allSheetNames.contains(sName)) {
                        allSheetNames.add(sName)
                    }
                }
            }

            val firstFs = foundSpreadsheets.first()
            Result.success(
                SpreadsheetData(
                    sheetNames = firstFs.sheetNames.ifEmpty { allSheetNames },
                    sheets = allSheets,
                    folders = foundSpreadsheets,
                    currentFolderName = firstFs.folderName
                )
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private data class DriveItem(val id: String, val name: String, val mimeType: String) {
        val isFolder: Boolean get() = mimeType == "application/vnd.google-apps.folder"
        val isSpreadsheet: Boolean get() =
            mimeType == "application/vnd.google-apps.spreadsheet" ||
            mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
            mimeType == "application/vnd.ms-excel" ||
            name.endsWith(".xlsx", ignoreCase = true) ||
            name.endsWith(".xls", ignoreCase = true) ||
            name.endsWith(".csv", ignoreCase = true)
    }

    private fun listDriveFolderChildren(folderId: String, apiKey: String, authToken: String = ""): List<DriveItem> {
        val q = "'$folderId' in parents and trashed = false"
        val encodedQ = java.net.URLEncoder.encode(q, "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQ&supportsAllDrives=true&includeItemsFromAllDrives=true&pageSize=100&fields=files(id,name,mimeType)&key=$apiKey"
        val requestBuilder = Request.Builder().url(url)
        if (authToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $authToken")
        }
        val request = requestBuilder.build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val json = org.json.JSONObject(body)
                val files = json.optJSONArray("files") ?: return emptyList()
                val list = mutableListOf<DriveItem>()
                for (i in 0 until files.length()) {
                    val obj = files.getJSONObject(i)
                    list.add(
                        DriveItem(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            mimeType = obj.optString("mimeType")
                        )
                    )
                }
                return list
            }
        } catch (_: Exception) {
            return emptyList()
        }
    }

    private fun getDriveItemName(id: String, apiKey: String, authToken: String = ""): String? {
        val url = "https://www.googleapis.com/drive/v3/files/$id?fields=name&supportsAllDrives=true&key=$apiKey"
        val requestBuilder = Request.Builder().url(url)
        if (authToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $authToken")
        }
        val request = requestBuilder.build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                return org.json.JSONObject(body).optString("name").takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            return null
        }
    }

    private suspend fun fetchDriveSpreadsheetData(id: String, mimeType: String, apiKey: String, authToken: String = ""): SpreadsheetData? {
        val exportUrl = if (mimeType == "application/vnd.google-apps.spreadsheet") {
            "https://docs.google.com/spreadsheets/d/$id/export?format=xlsx"
        } else {
            "https://www.googleapis.com/drive/v3/files/$id?alt=media&key=$apiKey"
        }
        val res = fetchUrl(exportUrl, authToken = authToken)
        if (res.isSuccess) return res.getOrNull()

        if (mimeType == "application/vnd.google-apps.spreadsheet") {
            val apiExportUrl = "https://www.googleapis.com/drive/v3/files/$id/export?mimeType=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet&key=$apiKey"
            val apiRes = fetchUrl(apiExportUrl, authToken = authToken)
            if (apiRes.isSuccess) return apiRes.getOrNull()
        }
        return null
    }

    fun extractFolderId(urlOrId: String): String {
        val trimmed = urlOrId.trim()
        val regex = Regex("/folders/([a-zA-Z0-9-_]+)")
        val match = regex.find(trimmed)
        return match?.groups?.get(1)?.value ?: trimmed
    }

    fun createSampleMultiFolderData(): SpreadsheetData {
        val folders = listOf(
            com.example.analizer.model.FolderSpreadsheet(
                folderName = "TXE3I05",
                spreadsheetName = "Planilha_TXE3",
                sheetNames = listOf("Resumo", "Métricas"),
                sheets = mapOf(
                    "Resumo" to listOf(
                        listOf("Item", "Valor", "Métrica"),
                        listOf("Sensor A", "105.4", "Pressão"),
                        listOf("Sensor B", "210.8", "Temperatura"),
                        listOf("Sensor C", "315.2", "Fluxo")
                    ),
                    "Métricas" to listOf(
                        listOf("Data", "Produção", "Eficiência"),
                        listOf("2026-01-01", "1250", "98.5"),
                        listOf("2026-01-02", "1310", "99.1")
                    )
                )
            ),
            com.example.analizer.model.FolderSpreadsheet(
                folderName = "TXE3I06",
                spreadsheetName = "Planilha_TXE3",
                sheetNames = listOf("Resumo", "Métricas"),
                sheets = mapOf(
                    "Resumo" to listOf(
                        listOf("Item", "Valor", "Métrica"),
                        listOf("Sensor A", "98.2", "Pressão"),
                        listOf("Sensor B", "204.5", "Temperatura"),
                        listOf("Sensor C", "310.0", "Fluxo")
                    ),
                    "Métricas" to listOf(
                        listOf("Data", "Produção", "Eficiência"),
                        listOf("2026-01-01", "1180", "97.2"),
                        listOf("2026-01-02", "1220", "98.0")
                    )
                )
            ),
            com.example.analizer.model.FolderSpreadsheet(
                folderName = "TXE3I07",
                spreadsheetName = "Planilha_TXE3",
                sheetNames = listOf("Resumo", "Métricas"),
                sheets = mapOf(
                    "Resumo" to listOf(
                        listOf("Item", "Valor", "Métrica"),
                        listOf("Sensor A", "112.0", "Pressão"),
                        listOf("Sensor B", "215.3", "Temperatura"),
                        listOf("Sensor C", "322.4", "Fluxo")
                    ),
                    "Métricas" to listOf(
                        listOf("Data", "Produção", "Eficiência"),
                        listOf("2026-01-01", "1340", "99.4"),
                        listOf("2026-01-02", "1390", "99.8")
                    )
                )
            )
        )
        val allSheets = mutableMapOf<String, List<List<String>>>()
        folders.forEach { f ->
            f.sheets.forEach { (s, rows) ->
                allSheets["[${f.folderName}] $s"] = rows
                if (!allSheets.containsKey(s)) allSheets[s] = rows
            }
        }
        return SpreadsheetData(
            sheetNames = listOf("Resumo", "Métricas"),
            sheets = allSheets,
            folders = folders,
            currentFolderName = "TXE3I05"
        )
    }
}
