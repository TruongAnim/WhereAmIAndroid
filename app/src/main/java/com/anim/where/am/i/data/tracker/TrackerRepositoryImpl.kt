@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.anim.where.am.i.data.tracker

import android.content.Context
import com.anim.where.am.i.di.IoDispatcher
import com.anim.where.am.i.domain.model.TrackingSettings
import com.anim.where.am.i.domain.model.TrackingStatus
import com.anim.where.am.i.domain.repository.TrackerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.traccar.client.Tracker
import org.traccar.client.requestPosition
import org.traccar.client.sharedTracker
import org.traccar.client.startTracking

class TrackerRepositoryImpl(
    private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val notificationText: String,
    private val bootstrapSettings: TrackingSettings,
) : TrackerRepository {

    private val trackerFlow = MutableStateFlow<Tracker?>(null)
    private val mutex = Mutex()

    private suspend fun tracker(): Tracker = mutex.withLock {
        trackerFlow.value ?: withContext(io) {
            sharedTracker(bootstrapSettings.toConfig(notificationText)).also { trackerFlow.value = it }
        }
    }

    override fun observeStatus(): Flow<TrackingStatus> =
        trackerFlow.flatMapLatest { t ->
            if (t == null) flow { emitAll(ensureThenState()) } else t.state.map { it.toStatus() }
        }

    private fun ensureThenState(): Flow<TrackingStatus> = flow {
        val t = tracker()
        emitAll(t.state.map { it.toStatus() })
    }

    override suspend fun start() {
        tracker().startTracking(context)
    }

    override suspend fun stop() {
        tracker().stop()
    }

    override suspend fun requestPosition(alarm: String?): Boolean =
        tracker().requestPosition(context, alarm)

    override suspend fun updateConfig(settings: TrackingSettings) = mutex.withLock {
        val current = trackerFlow.value ?: sharedTracker(settings.toConfig(notificationText))
        trackerFlow.value = current.updateConfig(settings.toConfig(notificationText))
    }
}
