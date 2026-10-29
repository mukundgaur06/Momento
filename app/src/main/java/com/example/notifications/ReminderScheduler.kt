package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.*
import java.util.Calendar

/** One alarm per local record. Stable request codes make replacements and cancellation idempotent. */
class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val preferences = NotificationPreferences(context)

    fun scheduleTask(task: Task) = schedule("task", task.id, task.dueDate?.takeIf { it > System.currentTimeMillis() && !task.isCompleted && !task.isArchived }, "TASK_REMINDERS", "Task due", task.title, preferences.tasks)
    fun scheduleEvent(event: Event) = schedule("event", event.id, (event.timeMillis - event.reminderMinutes.coerceAtLeast(0) * 60_000L).takeIf { it > System.currentTimeMillis() }, "EVENT_ALERTS", "Upcoming event", event.title, preferences.events)
    fun scheduleGoal(goal: Goal) = schedule("goal", goal.id, goal.targetDateMillis.takeIf { it > System.currentTimeMillis() && goal.status != Goal.GoalStatus.COMPLETED }, "GOAL_UPDATES", "Goal deadline", goal.title, preferences.goals)
    fun scheduleHabit(habit: Habit) {
        val next = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, preferences.habitHour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        schedule("habit", habit.id, next, "HABIT_REMINDERS", "Habit check-in", habit.name, preferences.habits)
    }
    fun cancel(type: String, id: Int) { alarms.cancel(pending(type, id, null)) }
    fun cancelCategory(type: String, ids: Iterable<Int>) = ids.forEach { cancel(type, it) }
    fun notifyExpenseThreshold() {
        ReminderReceiver().onReceive(context, Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_CHANNEL, "EXPENSE_ALERTS"); putExtra(ReminderReceiver.EXTRA_ID, "expense".hashCode())
            putExtra(ReminderReceiver.EXTRA_TITLE, "Monthly budget reached"); putExtra(ReminderReceiver.EXTRA_TEXT, "Your recorded spending has reached the monthly limit.")
        })
    }
    private fun schedule(type: String, id: Int, at: Long?, channel: String, title: String, text: String, enabled: Boolean) {
        val pending = pending(type, id, if (at == null || !enabled) null else Intent(context, ReminderReceiver::class.java).setAction("momento.$type.$id").apply { putExtra(ReminderReceiver.EXTRA_CHANNEL, channel); putExtra(ReminderReceiver.EXTRA_ID, requestCode(type, id)); putExtra(ReminderReceiver.EXTRA_TITLE, title); putExtra(ReminderReceiver.EXTRA_TEXT, text) })
        alarms.cancel(pending)
        if (at == null || !enabled) return
        if (Build.VERSION.SDK_INT >= 31 && alarms.canScheduleExactAlarms()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending) else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }
    private fun pending(type: String, id: Int, intent: Intent?): PendingIntent = PendingIntent.getBroadcast(context, requestCode(type, id), intent ?: Intent(context, ReminderReceiver::class.java).setAction("momento.$type.$id"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun requestCode(type: String, id: Int) = 31 * type.hashCode() + id
}
