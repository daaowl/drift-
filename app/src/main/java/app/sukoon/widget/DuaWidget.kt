package app.sukoon.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.sukoon.MainActivity
import app.sukoon.R
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.WidgetMode
import app.sukoon.domain.repository.DuaRepository
import app.sukoon.domain.repository.SettingsRepository
import app.sukoon.domain.usecase.GetDuaOfTheDayUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun duaRepository(): DuaRepository
    fun settingsRepository(): SettingsRepository
    fun duaOfTheDay(): GetDuaOfTheDayUseCase
}

class DuaWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val dua = runCatching {
            entry.duaRepository().ensureSeeded()
            val settings = entry.settingsRepository().settings.first()
            val pinned = settings.widgetPinnedDuaId
            if (settings.widgetMode == WidgetMode.PINNED && pinned != null) {
                entry.duaRepository().getDua(pinned) ?: entry.duaOfTheDay()(settings)
            } else {
                entry.duaOfTheDay()(settings)
            }
        }.getOrNull()
        val label = context.getString(R.string.widget_label)
        val empty = context.getString(R.string.widget_empty)
        provideContent { GlanceTheme { WidgetContent(dua, label, empty) } }
    }

    @Composable
    private fun WidgetContent(dua: Dua?, label: String, empty: String) {
        val ink = ColorProvider(day = Color(0xFFFBF7EE), night = Color(0xFF151B2B))
        val gold = ColorProvider(day = Color(0xFF8A6A2E), night = Color(0xFFD4AF6A))
        val text = ColorProvider(day = Color(0xFF1B1A17), night = Color(0xFFECE6D8))
        val muted = ColorProvider(day = Color(0xFF5F5849), night = Color(0xFFA8A496))
        val params = dua?.let { actionParametersOf(DUA_ID_KEY to it.id) } ?: actionParametersOf()
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ink)
                .cornerRadius(24.dp)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>(params)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = label.uppercase(), style = TextStyle(color = gold, fontSize = 11.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.height(8.dp))
            if (dua == null) {
                Text(empty, style = TextStyle(color = muted, fontSize = 13.sp, textAlign = TextAlign.Center))
            } else {
                Text(
                    text = dua.arabic,
                    maxLines = 3,
                    style = TextStyle(color = text, fontSize = 20.sp, textAlign = TextAlign.Center),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = dua.translation,
                    maxLines = 3,
                    style = TextStyle(color = muted, fontSize = 13.sp, textAlign = TextAlign.Center),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
                Spacer(GlanceModifier.height(6.dp))
                Text(dua.title, style = TextStyle(color = gold, fontSize = 12.sp, fontWeight = FontWeight.Medium))
            }
        }
    }

    companion object {
        val DUA_ID_KEY = ActionParameters.Key<String>(MainActivity.EXTRA_DUA_ID)
    }
}

class DuaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DuaWidget()
}
