package com.anim.where.am.i.quickactions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.anim.where.am.i.domain.model.TrackerAction
import com.anim.where.am.i.domain.usecase.RunTrackerAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutActivity : ComponentActivity() {

    @Inject lateinit var runTrackerAction: RunTrackerAction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(ShortcutManagerHelper.EXTRA_ACTION)
        val action = when (id) {
            "start" -> TrackerAction.START
            "stop" -> TrackerAction.STOP
            "sos" -> TrackerAction.SOS
            else -> null
        }
        if (action != null) {
            lifecycleScope.launch {
                try { runTrackerAction(action) } catch (_: IllegalStateException) {}
                finish()
            }
        } else finish()
    }
}
