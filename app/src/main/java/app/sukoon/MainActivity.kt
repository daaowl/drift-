package app.sukoon

import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.sukoon.core.designsystem.component.ErrorState
import app.sukoon.core.designsystem.theme.SukoonTheme
import app.sukoon.core.designsystem.theme.isAppInDarkTheme
import app.sukoon.domain.model.UserSettings
import app.sukoon.security.AppLockManager
import app.sukoon.ui.lock.LockScreen
import app.sukoon.ui.main.MainUiState
import app.sukoon.ui.main.MainViewModel
import app.sukoon.ui.main.SukoonAppUi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    @Inject lateinit var appLock: AppLockManager

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition {
            viewModel.uiState.value is MainUiState.Loading || appLock.requiresUnlock.value == null
        }
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState
                    .filterIsInstance<MainUiState.Ready>()
                    .map { it.settings.secureScreen to it.settings.hideFromRecents }
                    .distinctUntilChanged()
                    .collect { (secure, hideRecents) -> applyPrivacy(secure, hideRecents) }
            }
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val requiresUnlock by appLock.requiresUnlock.collectAsStateWithLifecycle()
            val windowSize = calculateWindowSizeClass(this)
            val settings = (state as? MainUiState.Ready)?.settings ?: UserSettings()

            SukoonTheme(settings) {
                val dark = isAppInDarkTheme(settings.themeMode)
                val scrim = MaterialTheme.colorScheme.background.copy(alpha = 0f).toArgb()
                androidx.compose.runtime.LaunchedEffect(dark) {
                    enableEdgeToEdge(
                        statusBarStyle = if (dark) SystemBarStyle.dark(scrim) else SystemBarStyle.light(scrim, scrim),
                        navigationBarStyle = if (dark) SystemBarStyle.dark(scrim) else SystemBarStyle.light(scrim, scrim),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .semantics { testTagsAsResourceId = true },
                ) {
                    when (val s = state) {
                        MainUiState.Loading -> Unit
                        is MainUiState.Error -> ErrorState(
                            icon = Icons.Outlined.CloudOff,
                            title = stringResource(R.string.error_seed_title),
                            body = stringResource(R.string.error_seed_body),
                            onRetry = viewModel::seed,
                        )
                        is MainUiState.Ready -> SukoonAppUi(
                            settings = s.settings,
                            windowSizeClass = windowSize,
                            mainViewModel = viewModel,
                        )
                    }
                    if (requiresUnlock == true) {
                        LockScreen(
                            biometricAllowed = appLock.biometricAllowed.collectAsStateWithLifecycle().value,
                            onUnlocked = appLock::unlock,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        viewModel.openDua(intent?.getStringExtra(EXTRA_DUA_ID))
    }

    private fun applyPrivacy(secure: Boolean, hideFromRecents: Boolean) {
        if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(!secure)
        runCatching {
            getSystemService(ActivityManager::class.java)?.appTasks?.forEach { it.setExcludeFromRecents(hideFromRecents) }
        }
    }

    companion object {
        const val EXTRA_DUA_ID = "dua_id"
    }
}
