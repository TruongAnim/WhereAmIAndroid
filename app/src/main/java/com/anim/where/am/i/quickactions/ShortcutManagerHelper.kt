package com.anim.where.am.i.quickactions

import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import com.anim.where.am.i.R

object ShortcutManagerHelper {
    const val EXTRA_ACTION = "shortcut_action"

    fun register(context: Context) {
        val manager = context.getSystemService<ShortcutManager>() ?: return
        manager.dynamicShortcuts = listOf(
            shortcut(context, "start", R.string.start_action),
            shortcut(context, "stop", R.string.stop_action),
            shortcut(context, "sos", R.string.sos_action),
        )
    }

    private fun shortcut(context: Context, id: String, labelRes: Int): ShortcutInfo {
        val intent = Intent(context, ShortcutActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_ACTION, id)
        }
        return ShortcutInfo.Builder(context, id)
            .setShortLabel(context.getString(labelRes))
            .setIntent(intent)
            .build()
    }
}
