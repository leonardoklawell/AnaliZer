package com.example.analizer.data

import android.content.Context
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

    suspend fun clearSavedData() = withContext(Dispatchers.IO) {
        try {
            if (spreadsheetFile.exists()) spreadsheetFile.delete()
            if (configFile.exists()) configFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
