package com.anim.where.am.i.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.anim.where.am.i.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val STARTED_AT = longPreferencesKey("tracking_started_at")

/**
 * When the current tracking session began, so the home screen can show a
 * running timer.
 *
 * The SDK's State has no notion of a session - it only knows enabled and
 * paused - and the timer has to survive the app being killed while tracking
 * continues in the foreground service, so the timestamp is persisted rather
 * than held in a ViewModel.
 */
@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    fun observeStartedAt(): Flow<Long?> =
        dataStore.data.map { prefs -> prefs[STARTED_AT]?.takeIf { it > 0L } }

    /** No-op when a session is already running, so the timer is not reset. */
    suspend fun markStarted(now: Long = System.currentTimeMillis()) = withContext(io) {
        dataStore.edit { prefs ->
            if (prefs[STARTED_AT] == null || prefs[STARTED_AT] == 0L) prefs[STARTED_AT] = now
        }
        Unit
    }

    suspend fun markStopped() = withContext(io) {
        dataStore.edit { prefs -> prefs.remove(STARTED_AT) }
        Unit
    }
}
