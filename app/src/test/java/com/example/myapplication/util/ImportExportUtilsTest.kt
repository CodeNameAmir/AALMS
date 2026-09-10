package com.example.myapplication.util

import com.example.myapplication.data.model.ExternalLogEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class ImportExportUtilsTest {

    @Test
    fun testParseTimestamp_ISO8601() {
        val timestamp = "2026-07-04T10:49:02.0799478Z"
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val expected = sdf.parse("2026-07-04T10:49:02.079Z")?.time ?: 0L
        
        val actual = ImportExportUtils.parseTimestamp(timestamp)
        assertEquals(expected, actual)
    }

    @Test
    fun testParseTimestamp_Long() {
        val now = System.currentTimeMillis()
        val actual = ImportExportUtils.parseTimestamp(now.toString())
        assertEquals(now, actual)
    }

    @Test
    fun testParseCsvLine_WithQuotesAndCommas() {
        val line = "61,INFO,\"General\",\"This is a test, with a comma\",1720082942000,true"
        val parts = ImportExportUtils.parseCsvLine(line)
        assertEquals(6, parts.size)
        assertEquals("61", parts[0])
        assertEquals("INFO", parts[1])
        assertEquals("General", parts[2])
        assertEquals("This is a test, with a comma", parts[3])
        assertEquals("1720082942000", parts[4])
        assertEquals("true", parts[5])
    }

    @Test
    fun testParseCsvLine_WithEscapedQuotes() {
        val line = "62,DEBUG,\"Logs\",\"Message with \"\"quotes\"\" inside\",1720082942000,false"
        val parts = ImportExportUtils.parseCsvLine(line)
        assertEquals("Message with \"quotes\" inside", parts[3])
    }
}
