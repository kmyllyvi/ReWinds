package org.test.multiuitest

import android.app.Application
import initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Napier logger
        Napier.base(DebugAntilog())

        initKoin()
    }
}
