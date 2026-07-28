package com.anim.where.am.i.domain.usecase

import com.anim.where.am.i.domain.model.TrackerAction
import com.anim.where.am.i.domain.model.TrackingStatus
import com.anim.where.am.i.domain.repository.TrackerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTrackingStatus @Inject constructor(private val repo: TrackerRepository) {
    operator fun invoke(): Flow<TrackingStatus> = repo.observeStatus()
}

class StartTracking @Inject constructor(private val repo: TrackerRepository) {
    suspend operator fun invoke() = repo.start()
}

class StopTracking @Inject constructor(private val repo: TrackerRepository) {
    suspend operator fun invoke() = repo.stop()
}

class RequestSos @Inject constructor(private val repo: TrackerRepository) {
    suspend operator fun invoke(): Boolean = repo.requestPosition(alarm = "sos")
}

class RunTrackerAction @Inject constructor(
    private val start: StartTracking,
    private val stop: StopTracking,
    private val sos: RequestSos,
) {
    suspend operator fun invoke(action: TrackerAction) = when (action) {
        TrackerAction.START -> start()
        TrackerAction.STOP -> stop()
        TrackerAction.SOS -> { sos(); Unit }
    }
}
