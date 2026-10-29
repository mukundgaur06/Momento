package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: return
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val contentIntent = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(intent.getStringExtra(EXTRA_TITLE) ?: "Momento")
            .setContentText(intent.getStringExtra(EXTRA_TEXT) ?: "You have a reminder")
            .setAutoCancel(true).setContentIntent(contentIntent).setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }
    companion object { const val EXTRA_CHANNEL = "channel"; const val EXTRA_ID = "id"; const val EXTRA_TITLE = "title"; const val EXTRA_TEXT = "text" }
}
