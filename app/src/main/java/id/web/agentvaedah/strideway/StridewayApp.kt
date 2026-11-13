package id.web.agentvaedah.strideway

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp
import id.web.agentvaedah.strideway.data.prefs.SettingsRepository
import id.web.agentvaedah.strideway.data.repo.ActivityRepository
import id.web.agentvaedah.strideway.di.AppScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre

@HiltAndroidApp
class StridewayApp : Application() {
    @Inject lateinit var activities: ActivityRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject @AppScope lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        // Personal records need best efforts for rides saved before they existed; fills in quietly.
        scope.launch(Dispatchers.IO) { runCatching { activities.backfillEfforts() } }
        // Climb used to be counted per GPS sample, which invented hundreds of metres. Recompute once.
        scope.launch(Dispatchers.IO) {
            runCatching { activities.backfillStats(settings.athlete.first()) }
        }
        val channel = NotificationChannel(
            RECORDING_CHANNEL,
            getString(R.string.recording_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.recording_channel_desc) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val RECORDING_CHANNEL = "recording"
    }
}
