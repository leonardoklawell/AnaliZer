package com.example.analizer.data

import android.content.Context
import android.net.Uri
import com.example.analizer.model.AnalysisConfig
import com.example.analizer.model.SpreadsheetData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class AppPersistenceManager(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    private val spreadsheetFile: File
        get() = File(context.filesDir, "saved_spreadsheet_data.json")

    private val configFile: File
        get() = File(context.filesDir, "saved_analysis_config.json")

    suspend fun saveSpreadsheetData(data: SpreadsheetData) = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(SpreadsheetData.serializer(), data)
            spreadsheetFile.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveAnalysisConfig(config: AnalysisConfig) = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(AnalysisConfig.serializer(), config)
            configFile.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadSavedData(): Pair<SpreadsheetData?, AnalysisConfig?> = withContext(Dispatchers.IO) {
        var loadedData: SpreadsheetData? = null
        var loadedConfig: AnalysisConfig? = null

        try {
            if (spreadsheetFile.exists()) {
                val text = spreadsheetFile.readText()
                if (text.isNotBlank()) {
                    loadedData = json.decodeFromString(SpreadsheetData.serializer(), text)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (configFile.exists()) {
                val text = configFile.readText()
                if (text.isNotBlank()) {
                    loadedConfig = json.decodeFromString(AnalysisConfig.serializer(), text)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Pair(loadedData, loadedConfig)
    }

    suspend fun exportConfigToUri(context: Context, uri: Uri, config: AnalysisConfig) = withContext(Dispatchers.IO) {
        val exportJson = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }
        val text = exportJson.encodeToString(AnalysisConfig.serializer(), config)
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(text.toByteArray(Charsets.UTF_8))
        }
    }

    suspend fun importConfigFromUri(context: Context, uri: Uri): AnalysisConfig? = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        } ?: return@withContext null
        if (text.isBlank()) return@withContext null
        val importJson = Json { ignoreUnknownKeys = true }
        importJson.decodeFromString(AnalysisConfig.serializer(), text)
    }

    suspend fun clearSavedData() = withContext(Dispatchers.IO) {
        try {
            if (spreadsheetFile.exists()) spreadsheetFile.delete()
            if (configFile.exists()) configFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
