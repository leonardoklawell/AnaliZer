package com.example.analizer.domain

import com.example.analizer.model.ColumnStats
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

data class ChartDataPoint(
    val x: Float,
    val y: Float,
    val label: String = ""
)

data class ChartSeries(
    val name: String,
    val points: List<ChartDataPoint>
)

class ChartDataMapper {

    /**
     * Maps a list of ColumnStats into a Vico CartesianChartModelProducer for column charts.
     */
    suspend fun createVicoColumnModelProducer(statsList: List<ColumnStats>): CartesianChartModelProducer {
        val modelProducer = CartesianChartModelProducer()
        modelProducer.runTransaction {
            columnSeries {
                statsList.forEachIndexed { index, stats ->
                    series(index.toFloat(), stats.mean)
                }
            }
        }
        return modelProducer
    }

    /**
     * Maps a list of ColumnStats into a Vico CartesianChartModelProducer for line charts.
     */
    suspend fun createVicoLineModelProducer(statsList: List<ColumnStats>): CartesianChartModelProducer {
        val modelProducer = CartesianChartModelProducer()
        modelProducer.runTransaction {
            lineSeries {
                statsList.forEachIndexed { index, stats ->
                    series(index.toFloat(), stats.mean)
                }
            }
        }
        return modelProducer
    }

    /**
     * Maps a specific ColumnStats' raw values into a Vico CartesianChartModelProducer (index vs value).
     */
    suspend fun createVicoSeriesModelProducer(columnStats: ColumnStats): CartesianChartModelProducer {
        val modelProducer = CartesianChartModelProducer()
        modelProducer.runTransaction {
            lineSeries {
                columnStats.rawValues.forEachIndexed { index, value ->
                    series(index.toFloat(), value)
                }
            }
        }
        return modelProducer
    }

    /**
     * Maps ColumnStats into generic chart series for custom Compose charting.
     */
    fun mapToCustomChartSeries(
        statsList: List<ColumnStats>,
        metricSelector: (ColumnStats) -> Double = { it.mean }
    ): ChartSeries {
        val points = statsList.mapIndexed { index, stats ->
            ChartDataPoint(
                x = index.toFloat(),
                y = metricSelector(stats).toFloat(),
                label = stats.columnName
            )
        }
        return ChartSeries(name = "Statistics", points = points)
    }

    /**
     * Maps a single ColumnStats raw values into generic chart series for custom Compose charting.
     */
    fun mapRawValuesToCustomSeries(columnStats: ColumnStats): ChartSeries {
        val points = columnStats.rawValues.mapIndexed { index, value ->
            ChartDataPoint(
                x = index.toFloat(),
                y = value.toFloat(),
                label = columnStats.rawStringValues.getOrNull(index) ?: index.toString()
            )
        }
        return ChartSeries(name = columnStats.columnName, points = points)
    }
}
