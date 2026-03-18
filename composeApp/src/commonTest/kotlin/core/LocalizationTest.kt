package core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests verifying that the localization framework (AppStrings + LanguageManager) is correct.
 */

class AppStringsCompletenessTest {

    @Test
    fun german_ok_isNotEmpty() {
        assertTrue(AppStrings.German.ok.isNotBlank(), "German 'ok' must not be blank")
    }

    @Test
    fun german_cancel_isNotEmpty() {
        assertTrue(AppStrings.German.cancel.isNotBlank(), "German 'cancel' must not be blank")
    }

    @Test
    fun german_delete_isNotEmpty() {
        assertTrue(AppStrings.German.delete.isNotBlank(), "German 'delete' must not be blank")
    }

    @Test
    fun german_back_isNotEmpty() {
        assertTrue(AppStrings.German.back.isNotBlank(), "German 'back' must not be blank")
    }

    @Test
    fun german_save_isNotEmpty() {
        assertTrue(AppStrings.German.save.isNotBlank(), "German 'save' must not be blank")
    }

    @Test
    fun german_savedLocations_isNotEmpty() {
        assertTrue(AppStrings.German.savedLocations.isNotBlank(), "German 'savedLocations' must not be blank")
    }

    @Test
    fun german_settingsTitle_isNotEmpty() {
        assertTrue(AppStrings.German.settingsTitle.isNotBlank(), "German 'settingsTitle' must not be blank")
    }

    @Test
    fun german_chatTitle_isNotEmpty() {
        assertTrue(AppStrings.German.chatTitle.isNotBlank(), "German 'chatTitle' must not be blank")
    }

    @Test
    fun german_monthlySummary_isNotEmpty() {
        assertTrue(AppStrings.German.monthlySummary.isNotBlank(), "German 'monthlySummary' must not be blank")
    }

    @Test
    fun german_monthJanuary_isNotEmpty() {
        assertTrue(AppStrings.German.monthJanuary.isNotBlank(), "German 'monthJanuary' must not be blank")
    }

    @Test
    fun german_monthDecember_isNotEmpty() {
        assertTrue(AppStrings.German.monthDecember.isNotBlank(), "German 'monthDecember' must not be blank")
    }

    @Test
    fun german_noWeatherData_isNotEmpty() {
        assertTrue(AppStrings.German.noWeatherData.isNotBlank(), "German 'noWeatherData' must not be blank")
    }

    @Test
    fun german_daysStored_singular_isNotEmpty() {
        val result = AppStrings.German.daysStored(1)
        assertTrue(result.isNotBlank(), "German daysStored(1) must not be blank")
    }

    @Test
    fun german_daysStored_plural_isNotEmpty() {
        val result = AppStrings.German.daysStored(5)
        assertTrue(result.isNotBlank(), "German daysStored(5) must not be blank")
    }

    @Test
    fun german_downloadMissingDays_singular_isNotEmpty() {
        val result = AppStrings.German.downloadMissingDays(1)
        assertTrue(result.isNotBlank(), "German downloadMissingDays(1) must not be blank")
    }

    @Test
    fun german_downloadMissingDays_plural_isNotEmpty() {
        val result = AppStrings.German.downloadMissingDays(3)
        assertTrue(result.isNotBlank(), "German downloadMissingDays(3) must not be blank")
    }

    @Test
    fun german_languageTitle_isNotEmpty() {
        assertTrue(AppStrings.German.languageTitle.isNotBlank(), "German 'languageTitle' must not be blank")
    }
}

class AppStringsGermanNotEnglishTest {

    @Test
    fun german_cancel_differFromEnglish() {
        assertNotEquals(
            AppStrings.English.cancel,
            AppStrings.German.cancel,
            "German cancel should differ from English"
        )
    }

    @Test
    fun german_delete_differFromEnglish() {
        assertNotEquals(
            AppStrings.English.delete,
            AppStrings.German.delete,
            "German delete should differ from English"
        )
    }

    @Test
    fun german_back_differFromEnglish() {
        assertNotEquals(
            AppStrings.English.back,
            AppStrings.German.back,
            "German back should differ from English"
        )
    }

    @Test
    fun german_savedLocations_differFromEnglish() {
        assertNotEquals(
            AppStrings.English.savedLocations,
            AppStrings.German.savedLocations,
            "German savedLocations should differ from English"
        )
    }

    @Test
    fun german_settingsTitle_differFromEnglish() {
        assertNotEquals(
            AppStrings.English.settingsTitle,
            AppStrings.German.settingsTitle,
            "German settingsTitle should differ from English"
        )
    }

    @Test
    fun german_monthlySummary_differFromEnglish() {
        assertNotEquals(
            AppStrings.English.monthlySummary,
            AppStrings.German.monthlySummary,
            "German monthlySummary should differ from English"
        )
    }

    @Test
    fun german_monthJanuary_differFromEnglish() {
        assertNotEquals(
            AppStrings.English.monthJanuary,
            AppStrings.German.monthJanuary,
            "German monthJanuary should differ from English"
        )
    }

    @Test
    fun german_monthMarch_differFromEnglish() {
        assertNotEquals(
            AppStrings.English.monthMarch,
            AppStrings.German.monthMarch,
            "German monthMarch (März) should differ from English (March)"
        )
    }

    @Test
    fun german_searchPlaceholder_differFromEnglish() {
        assertNotEquals(
            AppStrings.English.searchPlaceholder,
            AppStrings.German.searchPlaceholder,
            "German searchPlaceholder should differ from English"
        )
    }

    @Test
    fun german_noWeatherData_differFromEnglish() {
        assertNotEquals(
            AppStrings.English.noWeatherData,
            AppStrings.German.noWeatherData,
            "German noWeatherData should differ from English"
        )
    }

    @Test
    fun german_daysStored_singular_differFromEnglish() {
        val german = AppStrings.German.daysStored(1)
        val english = AppStrings.English.daysStored(1)
        assertNotEquals(german, english, "German daysStored(1) should differ from English")
    }

    @Test
    fun german_deletePlaceMessage_containsPlaceName() {
        val message = AppStrings.German.deletePlaceMessage("TestOrt")
        assertTrue(message.contains("TestOrt"), "German deletePlaceMessage should contain the place name")
    }

    @Test
    fun english_deletePlaceMessage_containsPlaceName() {
        val message = AppStrings.English.deletePlaceMessage("TestPlace")
        assertTrue(message.contains("TestPlace"), "English deletePlaceMessage should contain the place name")
    }
}

class LanguageManagerTest {

    @Test
    fun languageEnum_english_hasCorrectCode() {
        assertEquals("en", Language.ENGLISH.code)
    }

    @Test
    fun languageEnum_german_hasCorrectCode() {
        assertEquals("de", Language.GERMAN.code)
    }

    @Test
    fun languageEnum_english_hasDisplayName() {
        assertTrue(Language.ENGLISH.displayName.isNotBlank())
    }

    @Test
    fun languageEnum_german_hasDisplayName() {
        assertTrue(Language.GERMAN.displayName.isNotBlank())
    }

    @Test
    fun languageManager_setLanguage_updatesStateFlow() {
        // Set to German
        LanguageManager.setLanguage(Language.GERMAN)
        assertEquals(Language.GERMAN, LanguageManager.currentLanguage.value)

        // Set back to English
        LanguageManager.setLanguage(Language.ENGLISH)
        assertEquals(Language.ENGLISH, LanguageManager.currentLanguage.value)
    }

    @Test
    fun languageManager_setLanguage_german_thenEnglish_isEnglish() {
        LanguageManager.setLanguage(Language.GERMAN)
        LanguageManager.setLanguage(Language.ENGLISH)
        assertEquals(Language.ENGLISH, LanguageManager.currentLanguage.value)
    }

    @Test
    fun languageManager_allLanguagesHaveCodes() {
        Language.values().forEach { lang ->
            assertTrue(lang.code.isNotBlank(), "Language ${lang.name} must have a non-blank code")
        }
    }

    @Test
    fun languageManager_allLanguagesHaveDisplayNames() {
        Language.values().forEach { lang ->
            assertTrue(lang.displayName.isNotBlank(), "Language ${lang.name} must have a non-blank displayName")
        }
    }
}
