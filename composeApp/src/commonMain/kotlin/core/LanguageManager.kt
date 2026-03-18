package core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch")
}

object LanguageManager {
    private val _currentLanguage = MutableStateFlow(loadSavedLanguage())
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    fun setLanguage(lang: Language) {
        _currentLanguage.value = lang
        saveLanguagePreference(lang.code)
    }

    private fun loadSavedLanguage(): Language {
        val code = loadLanguagePreference() ?: "en"
        return Language.values().find { it.code == code } ?: Language.ENGLISH
    }
}
