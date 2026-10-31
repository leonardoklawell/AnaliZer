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
        targetSpreadsheetName: String = ""
    ): Result<SpreadsheetData> = withContext(Dispatchers.IO) {
        val trimmed = urlOrShareLink.trim()
        if (trimmed.contains("/folders/") || trimmed.contains("drive.google.com/drive")) {
            return@withContext searchGoogleDriveFolder(trimmed, targetSpreadsheetName)
        }

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

        // Fallback to drive folder search logic if direct URL fetch failed
        val driveFolderResult = searchGoogleDriveFolder(trimmed, targetSpreadsheetName)
        if (driveFolderResult.isSuccess) {
            return@withContext driveFolderResult
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
                    return@withContext Result.failure(IOException("Falha ao buscar URL (${response.code})"))
                }
                val body = response.body
                    ?: return@withContext Result.failure(IOException("Resposta da requisição vazia"))

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

    suspend fun searchGoogleDriveFolder(
        folderUrlOrId: String,
        targetSpreadsheetName: String,
        apiKey: String = "AIzaSyBt7RMey2FcEHq55jizwcEfT_ryqMEUit0"
    ): Result<SpreadsheetData> = withContext(Dispatchers.IO) {
        val folderId = extractFolderId(folderUrlOrId)
        if (folderId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("ID ou link da pasta inválido."))
        }

        try {
            // Check if the folderId is actually a direct file/spreadsheet or sheet URL
            val directItem = getDriveItemDetails(folderId, apiKey)
            if (directItem != null && directItem.isSpreadsheet) {
                val parsed = fetchDriveSpreadsheetData(directItem.id, directItem.mimeType, apiKey)
                if (parsed != null && parsed.sheetNames.isNotEmpty()) {
                    return@withContext Result.success(parsed)
                }
            }

            val rootName = directItem?.name ?: getDriveItemName(folderId, apiKey) ?: "Pasta Principal"
            val queue = ArrayDeque<Pair<String, String>>()
            queue.add(Pair(folderId, rootName))

            val foundSpreadsheets = mutableListOf<com.example.analizer.model.FolderSpreadsheet>()
            val visitedFolders = mutableSetOf<String>()
            val processedFileIds = mutableSetOf<String>()
            val allDiscoveredFiles = mutableListOf<Pair<DriveItem, String>>() // Item, FolderName

            // 1. BFS folder traversal up to 100 folders deep
            while (queue.isNotEmpty() && visitedFolders.size < 100) {
                val (currentFid, currentFname) = queue.removeFirst()
                if (!visitedFolders.add(currentFid)) continue

                val children = listDriveFolderChildren(currentFid, apiKey)
                for (child in children) {
                    if (child.isFolder) {
                        queue.add(Pair(child.id, child.name))
                    } else if (child.isSpreadsheet && processedFileIds.add(child.id)) {
                        allDiscoveredFiles.add(Pair(child, currentFname))
                    }
                }
            }

            // 2. Process matching spreadsheets first
            val targetMatches = allDiscoveredFiles.filter { (item, _) ->
                matchesTargetName(item.name, targetSpreadsheetName)
            }

            for ((child, folderName) in targetMatches) {
                val parsed = fetchDriveSpreadsheetData(child.id, child.mimeType, apiKey)
                if (parsed != null && parsed.sheetNames.isNotEmpty()) {
                    foundSpreadsheets.add(
                        com.example.analizer.model.FolderSpreadsheet(
                            folderName = folderName,
                            spreadsheetName = child.name,
                            sheetNames = parsed.sheetNames,
                            sheets = parsed.sheets
                        )
                    )
                }
            }

            // 3. Fallback: If no target matches yielded valid data, try all other discovered spreadsheets in folder
            if (foundSpreadsheets.isEmpty() && allDiscoveredFiles.isNotEmpty()) {
                for ((child, folderName) in allDiscoveredFiles) {
                    if (targetMatches.any { it.first.id == child.id }) continue // Skip already tried
                    val parsed = fetchDriveSpreadsheetData(child.id, child.mimeType, apiKey)
                    if (parsed != null && parsed.sheetNames.isNotEmpty()) {
                        foundSpreadsheets.add(
                            com.example.analizer.model.FolderSpreadsheet(
                                folderName = folderName,
                                spreadsheetName = child.name,
                                sheetNames = parsed.sheetNames,
                                sheets = parsed.sheets
                            )
                        )
                    }
                }
            }

            // 4. Direct global search query by name if targetSpreadsheetName is non-blank and BFS yielded no spreadsheet
            if (foundSpreadsheets.isEmpty() && targetSpreadsheetName.isNotBlank()) {
                val directSearchResults = searchDriveByName(targetSpreadsheetName, apiKey)
                for (child in directSearchResults) {
                    if (child.isSpreadsheet && processedFileIds.add(child.id)) {
                        val parsed = fetchDriveSpreadsheetData(child.id, child.mimeType, apiKey)
                        if (parsed != null && parsed.sheetNames.isNotEmpty()) {
                            foundSpreadsheets.add(
                                com.example.analizer.model.FolderSpreadsheet(
                                    folderName = rootName,
                                    spreadsheetName = child.name,
                                    sheetNames = parsed.sheetNames,
                                    sheets = parsed.sheets
                                )
                            )
                        }
                    }
                }
            }

            // 5. Check if targetSpreadsheetName itself is a URL/ID containing a sheet/file
            if (foundSpreadsheets.isEmpty() && targetSpreadsheetName.isNotBlank()) {
                val extractedId = extractFolderId(targetSpreadsheetName)
                if (extractedId.isNotBlank() && extractedId != folderId) {
                    val parsed = fetchDriveSpreadsheetData(extractedId, "application/vnd.google-apps.spreadsheet", apiKey)
                    if (parsed != null && parsed.sheetNames.isNotEmpty()) {
                        foundSpreadsheets.add(
                            com.example.analizer.model.FolderSpreadsheet(
                                folderName = rootName,
                                spreadsheetName = targetSpreadsheetName,
                                sheetNames = parsed.sheetNames,
                                sheets = parsed.sheets
                            )
                        )
                    }
                }
            }

            if (foundSpreadsheets.isEmpty()) {
                val errorMsg = if (targetSpreadsheetName.isNotBlank()) {
                    "Nenhuma planilha contendo '$targetSpreadsheetName' foi encontrada na pasta e subpastas. Certifique-se de que a pasta no Google Drive possui permissão de visualização pública ('Qualquer pessoa com o link')."
                } else {
                    "Nenhuma planilha foi encontrada na pasta informada. Certifique-se de que a pasta no Google Drive possui permissão de visualização pública ('Qualquer pessoa com o link')."
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

    private fun matchesTargetName(childName: String, targetName: String): Boolean {
        if (targetName.isBlank()) return true
        val childNorm = normalizeName(childName)
        val targetNorm = normalizeName(targetName)

        if (childNorm == targetNorm) return true
        if (childNorm.contains(targetNorm) || targetNorm.contains(childNorm)) return true

        val targetWords = targetNorm.split(" ").filter { it.length > 1 }
        val childWords = childNorm.split(" ").filter { it.length > 1 }

        if (targetWords.isNotEmpty()) {
            val matchesCount = targetWords.count { tWord -> childNorm.contains(tWord) }
            if (matchesCount == targetWords.size) return true
            if (targetWords.size >= 2 && matchesCount >= (targetWords.size + 1) / 2) return true
        }

        if (childWords.isNotEmpty()) {
            val matchesCount = childWords.count { cWord -> targetNorm.contains(cWord) }
            if (matchesCount == childWords.size) return true
        }

        return false
    }

    private fun normalizeName(str: String): String {
        val clean = str
            .replace(Regex("\\.(xlsx|xls|csv|gsheet)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[_\\-\\.\t]+"), " ")
        val nfd = java.text.Normalizer.normalize(clean, java.text.Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(nfd, "")
            .lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private data class DriveItem(val id: String, val name: String, val mimeType: String) {
        val isFolder: Boolean get() = mimeType == "application/vnd.google-apps.folder"
        val isSpreadsheet: Boolean get() =
            mimeType == "application/vnd.google-apps.spreadsheet" ||
            mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
            mimeType == "application/vnd.ms-excel" ||
            mimeType.contains("spreadsheet") ||
            name.endsWith(".xlsx", ignoreCase = true) ||
            name.endsWith(".xls", ignoreCase = true) ||
            name.endsWith(".csv", ignoreCase = true)
    }

    private fun listDriveFolderChildren(folderId: String, apiKey: String): List<DriveItem> {
        val list = mutableListOf<DriveItem>()
        val seenIds = mutableSetOf<String>()

        // 1. Google Drive API v3 query
        try {
            val q = "'$folderId' in parents and trashed = false"
            val encodedQ = java.net.URLEncoder.encode(q, "UTF-8")
            val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQ&supportsAllDrives=true&includeItemsFromAllDrives=true&pageSize=100&fields=files(id,name,mimeType)&key=$apiKey"
            val request = Request.Builder().url(url).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = org.json.JSONObject(body)
                        val files = json.optJSONArray("files")
                        if (files != null) {
                            for (i in 0 until files.length()) {
                                val obj = files.getJSONObject(i)
                                val item = DriveItem(
                                    id = obj.optString("id"),
                                    name = obj.optString("name"),
                                    mimeType = obj.optString("mimeType")
                                )
                                if (item.id.isNotBlank() && seenIds.add(item.id)) {
                                    list.add(item)
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore API error and proceed to HTML scraping
        }

        // 2. Web HTML Scraping fallback
        val htmlItems = scrapeDriveFolderHtml(folderId, apiKey)
        for (item in htmlItems) {
            if (item.id.isNotBlank() && seenIds.add(item.id)) {
                list.add(item)
            }
        }

        return list
    }

    private fun scrapeDriveFolderHtml(folderId: String, apiKey: String): List<DriveItem> {
        val items = mutableListOf<DriveItem>()
        val visitedIds = mutableSetOf<String>()
        visitedIds.add(folderId)

        val urlsToTry = listOf(
            "https://drive.google.com/drive/folders/$folderId",
            "https://drive.google.com/drive/u/0/folders/$folderId",
            "https://drive.google.com/embeddedfolderview?id=$folderId#list"
        )

        for (urlStr in urlsToTry) {
            try {
                val request = Request.Builder()
                    .url(urlStr)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val html = response.body?.string() ?: return@use

                    // Folder links regex
                    val folderRegexes = listOf(
                        Regex("""/folders/([a-zA-Z0-9-_]{25,50})"""),
                        Regex("""id=([a-zA-Z0-9-_]{25,50}).*?folder""")
                    )
                    for (regex in folderRegexes) {
                        regex.findAll(html).forEach { match ->
                            val fid = match.groups[1]?.value.orEmpty()
                            if (fid.isNotBlank() && visitedIds.add(fid)) {
                                val details = getDriveItemDetails(fid, apiKey)
                                val name = details?.name ?: "Subpasta"
                                items.add(DriveItem(id = fid, name = name, mimeType = "application/vnd.google-apps.folder"))
                            }
                        }
                    }

                    // Spreadsheet / File links regex
                    val fileRegexes = listOf(
                        Regex("""/spreadsheets/d/([a-zA-Z0-9-_]{25,50})"""),
                        Regex("""/file/d/([a-zA-Z0-9-_]{25,50})""")
                    )
                    for (regex in fileRegexes) {
                        regex.findAll(html).forEach { match ->
                            val fid = match.groups[1]?.value.orEmpty()
                            if (fid.isNotBlank() && visitedIds.add(fid)) {
                                val details = getDriveItemDetails(fid, apiKey)
                                val name = details?.name ?: "Planilha"
                                items.add(DriveItem(id = fid, name = name, mimeType = details?.mimeType ?: "application/vnd.google-apps.spreadsheet"))
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Continue trying next URL
            }
        }
        return items
    }

    private fun searchDriveByName(targetName: String, apiKey: String): List<DriveItem> {
        val cleanName = targetName.replace("'", "\\'").trim()
        val q = "name contains '$cleanName' and trashed = false"
        val encodedQ = java.net.URLEncoder.encode(q, "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQ&supportsAllDrives=true&includeItemsFromAllDrives=true&pageSize=50&fields=files(id,name,mimeType)&key=$apiKey"
        val request = Request.Builder().url(url).build()

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

    private fun getDriveItemDetails(id: String, apiKey: String): DriveItem? {
        val url = "https://www.googleapis.com/drive/v3/files/$id?fields=id,name,mimeType&supportsAllDrives=true&key=$apiKey"
        val request = Request.Builder().url(url).build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = org.json.JSONObject(body)
                return DriveItem(
                    id = json.optString("id"),
                    name = json.optString("name"),
                    mimeType = json.optString("mimeType")
                )
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun getDriveItemName(id: String, apiKey: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files/$id?fields=name&supportsAllDrives=true&key=$apiKey"
        val request = Request.Builder().url(url).build()
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

    private suspend fun fetchDriveSpreadsheetData(id: String, mimeType: String, apiKey: String): SpreadsheetData? {
        val urlsToTry = mutableListOf<String>()

        // 1. Direct Sheets export as XLSX
        urlsToTry.add("https://docs.google.com/spreadsheets/d/$id/export?format=xlsx")

        // 2. Direct Google Drive file download (for uploaded Excel/CSV files)
        urlsToTry.add("https://drive.google.com/uc?export=download&id=$id")
        urlsToTry.add("https://drive.google.com/uc?id=$id&export=download")

        // 3. Direct Sheets export as CSV
        urlsToTry.add("https://docs.google.com/spreadsheets/d/$id/export?format=csv")

        // 4. API Key media download & export
        if (apiKey.isNotBlank()) {
            urlsToTry.add("https://www.googleapis.com/drive/v3/files/$id?alt=media&key=$apiKey")
            urlsToTry.add("https://www.googleapis.com/drive/v3/files/$id/export?mimeType=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet&key=$apiKey")
        }

        for (url in urlsToTry) {
            val res = fetchUrl(url)
            if (res.isSuccess) {
                val data = res.getOrNull()
                if (data != null && data.sheetNames.isNotEmpty()) {
                    return data
                }
            }
        }

        return null
    }

    fun extractFolderId(urlOrId: String): String {
        val trimmed = urlOrId.trim()
        val folderRegex = Regex("/folders/([a-zA-Z0-9-_]+)")
        val folderMatch = folderRegex.find(trimmed)
        if (folderMatch != null) return folderMatch.groups[1]?.value ?: trimmed

        val sheetRegex = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)")
        val sheetMatch = sheetRegex.find(trimmed)
        if (sheetMatch != null) return sheetMatch.groups[1]?.value ?: trimmed

        val fileRegex = Regex("/file/d/([a-zA-Z0-9-_]+)")
        val fileMatch = fileRegex.find(trimmed)
        if (fileMatch != null) return fileMatch.groups[1]?.value ?: trimmed

        val idParamRegex = Regex("[?&]id=([a-zA-Z0-9-_]+)")
        val idParamMatch = idParamRegex.find(trimmed)
        if (idParamMatch != null) return idParamMatch.groups[1]?.value ?: trimmed

        return trimmed
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
