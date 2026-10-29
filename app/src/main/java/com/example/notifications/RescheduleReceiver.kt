package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.MomentoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { reconcile(context.applicationContext) } finally { pending.finish() }
        }
    }
    companion object {
        suspend fun reconcile(context: Context) {
            val db = MomentoDatabase.getDatabase(context); val scheduler = ReminderScheduler(context)
            db.taskDao().getAllTasks().first().forEach(scheduler::scheduleTask)
            db.eventDao().getAllEvents().first().forEach(scheduler::scheduleEvent)
            db.goalDao().getAllGoals().first().forEach(scheduler::scheduleGoal)
            db.habitDao().getAllHabits().first().forEach(scheduler::scheduleHabit)
        }
    }
}
