package com.example.analizer.domain

import com.example.analizer.model.ColumnStats
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ChartDataMapperTest {

    private val mapper = ChartDataMapper()

    @Test
    fun testMapToCustomChartSeries() {
        val statsList = listOf(
            ColumnStats(columnName = "Col1", mean = 10.0),
            ColumnStats(columnName = "Col2", mean = 25.5)
        )

        val series = mapper.mapToCustomChartSeries(statsList) { it.mean }
        assertEquals("Statistics", series.name)
        assertEquals(2, series.points.size)
        assertEquals(0f, series.points[0].x)
        assertEquals(10f, series.points[0].y)
        assertEquals("Col1", series.points[0].label)
        assertEquals(1f, series.points[1].x)
        assertEquals(25.5f, series.points[1].y)
        assertEquals("Col2", series.points[1].label)
    }

    @Test
    fun testMapRawValuesToCustomSeries() {
        val stats = ColumnStats(
            columnName = "Prices",
            rawValues = listOf(10.0, 20.0, 30.0),
            rawStringValues = listOf("10", "20", "30")
        )

        val series = mapper.mapRawValuesToCustomSeries(stats)
        assertEquals("Prices", series.name)
        assertEquals(3, series.points.size)
        assertEquals(10f, series.points[0].y)
        assertEquals("10", series.points[0].label)
    }

    @Test
    fun testVicoModelProducers() = runTest {
        val statsList = listOf(
            ColumnStats(columnName = "A", mean = 5.0),
            ColumnStats(columnName = "B", mean = 15.0)
        )

        val columnProducer = mapper.createVicoColumnModelProducer(statsList)
        assertNotNull(columnProducer)

        val lineProducer = mapper.createVicoLineModelProducer(statsList)
        assertNotNull(lineProducer)

        val seriesProducer = mapper.createVicoSeriesModelProducer(statsList[0])
        assertNotNull(seriesProducer)
    }
}
