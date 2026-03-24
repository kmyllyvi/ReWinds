package core

import com.km.rewinds.db.AppDatabase

class AppSettingsRepository(private val db: AppDatabase) {
    fun getString(key: String): String? =
        db.appDatabaseQueries.getAppSetting(key).executeAsOneOrNull()

    fun setString(key: String, value: String) =
        db.appDatabaseQueries.setAppSetting(key, value)
}
