package com.anim.where.am.i

import android.app.Application
import com.anim.where.am.i.quickactions.ShortcutManagerHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WhereAmIApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ShortcutManagerHelper.register(this)
    }
}
