package app.sukoon.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.sukoon.domain.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules daily reminders with WorkManager. Delivery is inexact by design (within a few
 * minutes of the chosen time), which avoids the exact-alarm permission entirely.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class Plan(
        val daily: Int?,
        val morning: Int?,
        val evening: Int?,
    ) {
        companion object {
            fun from(s: UserSettings) = Plan(
                daily = s.dailyMinuteOfDay.takeIf { s.dailyEnabled },
                morning = s.morningMinuteOfDay.takeIf { s.morningEnabled },
                evening = s.eveningMinuteOfDay.takeIf { s.eveningEnabled },
            )
        }
    }

    fun apply(plan: Plan) {
        schedule(ReminderWorker.TYPE_DAILY, plan.daily)
        schedule(ReminderWorker.TYPE_MORNING, plan.morning)
        schedule(ReminderWorker.TYPE_EVENING, plan.evening)
        scheduleWidgetRefresh()
    }

    private fun schedule(type: String, minuteOfDay: Int?) {
        val wm = WorkManager.getInstance(context)
        val name = "reminder_$type"
        if (minuteOfDay == null) {
            wm.cancelUniqueWork(name)
            return
        }
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(minuteOfDay).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_TYPE to type, ReminderWorker.KEY_MINUTE to minuteOfDay))
            .addTag(TAG)
            .build()
        wm.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    private fun scheduleWidgetRefresh() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(1).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_TYPE to ReminderWorker.TYPE_WIDGET))
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("widget_refresh", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    companion object {
        const val TAG = "sukoon_reminders"

        fun delayUntil(minuteOfDay: Int, now: LocalDateTime = LocalDateTime.now()): Duration {
            val target = LocalTime.of((minuteOfDay / 60).coerceIn(0, 23), (minuteOfDay % 60).coerceIn(0, 59))
            var next = now.toLocalDate().atTime(target)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next)
        }
    }
}
