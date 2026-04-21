package com.km.rewinds

import android.app.Application
import core.DatabaseDriverFactory
import core.initializeDatabaseExportImport
import core.loadWeatherApiKeyFromPreferences
import core.provideAndroidContextForLanguage
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

        // Initialize language preference storage with context
        provideAndroidContextForLanguage(this)

        // Load persisted Visual Crossing API key into memory
        loadWeatherApiKeyFromPreferences()

        initKoin(DatabaseDriverFactory(this))
    }
}
