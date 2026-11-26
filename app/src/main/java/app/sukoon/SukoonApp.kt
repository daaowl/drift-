package app.sukoon

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import app.sukoon.di.ApplicationScope
import app.sukoon.domain.repository.SettingsRepository
import app.sukoon.notifications.NotificationChannels
import app.sukoon.notifications.ReminderScheduler
import app.sukoon.security.AppLockManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SukoonApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var appLockManager: AppLockManager
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        appLockManager.start()
        appScope.launch {
            settingsRepository.settings
                .map { ReminderScheduler.Plan.from(it) }
                .distinctUntilChanged()
                .collect { reminderScheduler.apply(it) }
        }
    }
}
