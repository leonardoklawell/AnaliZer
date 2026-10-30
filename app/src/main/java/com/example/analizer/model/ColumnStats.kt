package com.example.analizer.model

import kotlinx.serialization.Serializable

@Serializable
data class ColumnStats(
    val columnName: String,
    val count: Long = 0L,
    val sum: Double = 0.0,
    val mean: Double = 0.0,
    val min: Double = 0.0,
    val max: Double = 0.0,
    val median: Double = 0.0,
    val stdDev: Double = 0.0,
    val rawValues: List<Double> = emptyList(),
    val rawStringValues: List<String> = emptyList(),
    val columnIndex: Int = 0,
    val metric: String = "Média",
    val sheetName: String = "",
    val groupName: String = ""
)
