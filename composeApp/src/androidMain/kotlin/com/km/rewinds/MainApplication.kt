package com.km.rewinds

import android.app.Application
import core.DatabaseDriverFactory
import core.initializeDatabaseExportImport
import initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Napier logger
        Napier.base(DebugAntilog())

        // Initialize database export/import with context
        initializeDatabaseExportImport(this)

        initKoin(DatabaseDriverFactory(this))
    }
}
