package ru.tabel.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notifManager: TabelNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val id    = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("title") ?: "Табель"
        val body  = intent.getStringExtra("body")  ?: ""
        val sound = intent.getStringExtra("sound") ?: "default"
        notifManager.showNotification(id, title, body, sound)
    }
}
