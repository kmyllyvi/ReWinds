package core

import com.km.rewinds.db.AppDatabase

/**
 * Minimal key/value settings abstraction. Extracted so consumers (e.g. ViewModels)
 * can depend on a small interface and be unit-tested with an in-memory fake instead
 * of a real SQLDelight database.
 */
interface AppSettingsStore {
    fun getString(key: String): String?
    fun setString(key: String, value: String)
}

class AppSettingsRepository(private val db: AppDatabase) : AppSettingsStore {
    override fun getString(key: String): String? =
        db.appDatabaseQueries.getAppSetting(key).executeAsOneOrNull()

    override fun setString(key: String, value: String) {
        db.appDatabaseQueries.setAppSetting(key, value)
    }
}
