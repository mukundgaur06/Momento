package com.example.notifications

import android.content.Context

/** Small, synchronous preference store used by scheduling code and the Settings screen. */
class NotificationPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("notification_preferences", Context.MODE_PRIVATE)
    var tasks: Boolean
        get() = prefs.getBoolean("tasks", true); set(value) = prefs.edit().putBoolean("tasks", value).apply()
    var events: Boolean
        get() = prefs.getBoolean("events", true); set(value) = prefs.edit().putBoolean("events", value).apply()
    var goals: Boolean
        get() = prefs.getBoolean("goals", true); set(value) = prefs.edit().putBoolean("goals", value).apply()
    var habits: Boolean
        get() = prefs.getBoolean("habits", true); set(value) = prefs.edit().putBoolean("habits", value).apply()
    var expenses: Boolean
        get() = prefs.getBoolean("expenses", false); set(value) = prefs.edit().putBoolean("expenses", value).apply()
    var habitHour: Int
        get() = prefs.getInt("habit_hour", 19); set(value) = prefs.edit().putInt("habit_hour", value.coerceIn(0, 23)).apply()
    var expenseLimit: Float
        get() = prefs.getFloat("expense_limit", 0f); set(value) = prefs.edit().putFloat("expense_limit", value.coerceAtLeast(0f)).apply()
    fun expenseNotifiedMonth(): String? = prefs.getString("expense_notified_month", null)
    fun markExpenseNotified(month: String) = prefs.edit().putString("expense_notified_month", month).apply()
    fun clearExpenseNotification() = prefs.edit().remove("expense_notified_month").apply()
}
