package app.sukoon.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.sukoon.MainActivity
import app.sukoon.R
import app.sukoon.domain.repository.DuaRepository
import app.sukoon.domain.repository.SettingsRepository
import app.sukoon.domain.usecase.GetDuaOfTheDayUseCase
import app.sukoon.widget.DuaWidget
import androidx.glance.appwidget.updateAll
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val duas: DuaRepository,
    private val settingsRepository: SettingsRepository,
    private val duaOfTheDay: GetDuaOfTheDayUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        duas.ensureSeeded()
        val settings = settingsRepository.settings.first()
        when (inputData.getString(KEY_TYPE)) {
            TYPE_DAILY -> if (settings.dailyEnabled) {
                val dua = duaOfTheDay(settings)
                if (dua != null) {
                    notify(
                        id = 1001,
                        channel = NotificationChannels.DAILY,
                        title = applicationContext.getString(R.string.notif_daily_title, dua.title),
                        text = dua.translation,
                        duaId = dua.id,
                    )
                }
            }
            TYPE_MORNING -> if (settings.morningEnabled) notify(
                id = 1002,
                channel = NotificationChannels.MORNING,
                title = applicationContext.getString(R.string.notif_morning_title),
                text = applicationContext.getString(R.string.notif_morning_text),
                duaId = null,
            )
            TYPE_EVENING -> if (settings.eveningEnabled) notify(
                id = 1003,
                channel = NotificationChannels.EVENING,
                title = applicationContext.getString(R.string.notif_evening_title),
                text = applicationContext.getString(R.string.notif_evening_text),
                duaId = null,
            )
        }
        DuaWidget().updateAll(applicationContext)
        Result.success()
    }.getOrElse { Result.retry() }

    private fun notify(id: Int, channel: String, title: String, text: String, duaId: String?) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            duaId?.let { putExtra(MainActivity.EXTRA_DUA_ID, it) }
        }
        val pending = PendingIntent.getActivity(
            ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(ContextCompat.getColor(ctx, R.color.brand_gold))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, notification)
    }

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_MINUTE = "minute"
        const val TYPE_DAILY = "daily"
        const val TYPE_MORNING = "morning"
        const val TYPE_EVENING = "evening"
        const val TYPE_WIDGET = "widget"
    }
}
