package com.example.calendar

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.data.CalendarEventLink
import com.example.data.Event
import com.example.data.MomentoDatabase

/**
 * Integration with the device Calendar Provider. Google Calendar accounts exposed by Android are
 * supported without embedding OAuth credentials or a WebView. Calls are safe no-ops until the
 * user explicitly grants calendar permission and selects a writable calendar.
 */
class CalendarIntegration(private val context: Context, private val database: MomentoDatabase) {
    private val prefs = context.getSharedPreferences("calendar_integration", Context.MODE_PRIVATE)
    val isConnected get() = hasPermission() && selectedCalendarId() != null
    fun hasPermission() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
    fun selectedCalendarId(): Long? = prefs.getLong("calendar_id", -1L).takeIf { it >= 0 }
    fun selectedCalendarName(): String? = prefs.getString("calendar_name", null)
    fun disconnect() = prefs.edit().clear().apply()
    fun availableCalendars(): List<CalendarChoice> {
        if (!hasPermission()) return emptyList()
        return context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL), "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?", arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()), null)?.use { cursor ->
            buildList { while (cursor.moveToNext()) add(CalendarChoice(cursor.getLong(0), cursor.getString(1))) }
        } ?: emptyList()
    }
    fun selectCalendar(choice: CalendarChoice) { prefs.edit().putLong("calendar_id", choice.id).putString("calendar_name", choice.name).apply() }
    suspend fun export(event: Event) {
        val calendarId = selectedCalendarId() ?: return
        if (!hasPermission()) return
        val link = database.calendarEventLinkDao().getByLocalId(event.id)
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId); put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description); put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.Events.DTSTART, event.timeMillis); put(CalendarContract.Events.DTEND, event.endTimeMillis ?: event.timeMillis + 3_600_000L)
            put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
        }
        val providerId = if (link == null) context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)?.lastPathSegment?.toLongOrNull() else {
            context.contentResolver.update(CalendarContract.Events.CONTENT_URI.buildUpon().appendPath(link.providerEventId.toString()).build(), values, null, null); link.providerEventId
        }
        if (providerId != null) database.calendarEventLinkDao().upsert(CalendarEventLink(event.id, providerId, calendarId))
    }
    suspend fun deleteLinked(localId: Int) {
        val link = database.calendarEventLinkDao().getByLocalId(localId) ?: return
        if (hasPermission()) context.contentResolver.delete(CalendarContract.Events.CONTENT_URI.buildUpon().appendPath(link.providerEventId.toString()).build(), null, null)
        database.calendarEventLinkDao().deleteByLocalId(localId)
    }
    /** Explicit pull; provider IDs are linked before insertion so repeated imports are duplicate-safe. */
    suspend fun importEvents(): Int {
        val calendarId = selectedCalendarId() ?: return 0
        if (!hasPermission()) return 0
        var imported = 0
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID, CalendarContract.Events.TITLE, CalendarContract.Events.DESCRIPTION, CalendarContract.Events.EVENT_LOCATION, CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND),
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DELETED} = 0",
            arrayOf(calendarId.toString()), null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val providerId = cursor.getLong(0)
                if (database.calendarEventLinkDao().getByProviderId(providerId) != null) continue
                val start = cursor.getLong(4)
                if (start <= 0L) continue
                val localId = database.eventDao().insertEvent(Event(
                    title = cursor.getString(1)?.ifBlank { "Untitled event" } ?: "Untitled event",
                    description = cursor.getString(2) ?: "", location = cursor.getString(3),
                    timeMillis = start, endTimeMillis = cursor.getLong(5).takeIf { it > start }, category = "Google Calendar"
                )).toInt()
                database.calendarEventLinkDao().upsert(CalendarEventLink(localId, providerId, calendarId))
                imported++
            }
        }
        return imported
    }
    data class CalendarChoice(val id: Long, val name: String)
}
