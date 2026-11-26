package app.sukoon.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import app.sukoon.R

object NotificationChannels {
    const val MORNING = "morning_reminder"
    const val EVENING = "evening_reminder"
    const val DAILY = "dua_of_the_day"

    fun create(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val channels = listOf(
            NotificationChannel(MORNING, context.getString(R.string.channel_morning), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_morning_desc) },
            NotificationChannel(EVENING, context.getString(R.string.channel_evening), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_evening_desc) },
            NotificationChannel(DAILY, context.getString(R.string.channel_daily), NotificationManager.IMPORTANCE_LOW)
                .apply { description = context.getString(R.string.channel_daily_desc) },
        )
        manager.createNotificationChannels(channels)
    }
}
