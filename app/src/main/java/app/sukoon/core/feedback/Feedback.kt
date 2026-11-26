package app.sukoon.core.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import app.sukoon.R
import app.sukoon.domain.model.HapticStrength

/** Haptics for key actions. All calls are no-ops when the strength is OFF. */
class Haptics(private val view: View, context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun tick(strength: HapticStrength) {
        val constant = when (strength) {
            HapticStrength.OFF -> return
            HapticStrength.LIGHT -> HapticFeedbackConstants.CLOCK_TICK
            HapticStrength.MEDIUM -> HapticFeedbackConstants.VIRTUAL_KEY
            HapticStrength.STRONG -> HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }

    fun confirm() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.VIRTUAL_KEY
        view.performHapticFeedback(constant)
    }

    fun complete() {
        val v = vibrator ?: return confirm()
        if (!v.hasVibrator()) return confirm()
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 80), intArrayOf(0, 180, 0, 255), -1))
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view) { Haptics(view, context) }
}

/** Soft tick sound for the counter, loaded once and released with the composition. */
class TickSound(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val tickId = pool.load(context, R.raw.counter_tick, 1)
    private val doneId = pool.load(context, R.raw.counter_done, 1)

    fun tick() { pool.play(tickId, 0.5f, 0.5f, 1, 0, 1f) }
    fun done() { pool.play(doneId, 0.7f, 0.7f, 1, 0, 1f) }
    fun release() = pool.release()
}

@Composable
fun rememberTickSound(): TickSound {
    val context = LocalContext.current
    val sound = remember { TickSound(context.applicationContext) }
    DisposableEffect(sound) { onDispose { sound.release() } }
    return sound
}
