package core

/**
 * Data class holding all app strings for a given language.
 * Plurals are represented as lambdas that take a count parameter.
 *
 * Property names use camelCase. String format parameters are handled by the calling site.
 * Lambdas that take string parameters use typed arguments.
 */
data class AppStrings(
    // Common / Shared
    val ok: String,
    val cancel: String,
    val delete: String,
    val back: String,
    val save: String,
    val downloading: String,
    val appTitle: String,
    val dismiss: String,
    val download: String,

    // HomeView
    val savedLocations: String,
    val searchPlaceholder: String,
    val apiError: String,
    val deletePlaceTitle: String,
    val deletePlaceMessage: (String) -> String,
    val settingsIconDesc: String,
    val chatIconDesc: String,
    val daysStored: (Int) -> String,

    // ChatView
    val chatTitle: String,
    val messagePlaceholder: String,
    val sendMessageDesc: String,
    val apiKeyNotConfigured: String,
    val apiKeyNotConfiguredMessage: String,
    val warningIconDesc: String,

    // SettingsView
    val settingsTitle: String,
    val languageTitle: String,
    val anthropicKeyTitle: String,
    val anthropicKeyDescription: String,
    val apiKeyConfigured: String,
    val apiKeyNotConfiguredStatus: String,
    val apiKeySaved: String,
    val apiKeyPlaceholder: String,
    val apiKeyFieldLabel: String,
    val anthropicApiUrl: String,
    val saveKey: String,
    val visualCrossingKeyTitle: String,
    val visualCrossingKeyDescription: String,
    val visualCrossingApiUrl: String,
    val deleteKeyTitle: (String) -> String,
    val deleteKeyMessage: (String) -> String,

    // PlaceSummaryView
    val infoIconDesc: String,
    val downloadFullMonth: String,
    val loadingSummary: String,
    val errorLabel: String,
    val noStoredDays: String,
    val daysFraction: (Int, Int) -> String,
    val tempDisplay: (String) -> String,
    val missingDaysMessage: (String, Int, Int) -> String,
    val notDownloadedMessage: (String, Int) -> String,

    // Month Names - Short
    val monthShortJan: String,
    val monthShortFeb: String,
    val monthShortMar: String,
    val monthShortApr: String,
    val monthShortMay: String,
    val monthShortJun: String,
    val monthShortJul: String,
    val monthShortAug: String,
    val monthShortSep: String,
    val monthShortOct: String,
    val monthShortNov: String,
    val monthShortDec: String,

    // Month Names - Full
    val monthJanuary: String,
    val monthFebruary: String,
    val monthMarch: String,
    val monthApril: String,
    val monthMay: String,
    val monthJune: String,
    val monthJuly: String,
    val monthAugust: String,
    val monthSeptember: String,
    val monthOctober: String,
    val monthNovember: String,
    val monthDecember: String,

    // MonthlyStatisticsView
    val monthlySummary: String,
    val kiteableDays: (Int) -> String,
    val generalStats: String,
    val dailyBreakdown: String,
    val daysWithData: (String) -> String,
    val avgMinTemp: (String) -> String,
    val avgMaxTemp: (String) -> String,
    val overallAvgTemp: (String) -> String,
    val coldestDay: (String, String) -> String,
    val hottestDay: (String, String) -> String,
    val totalSolarEnergy: (String) -> String,
    val noWeatherData: String,
    val downloadMissingDays: (Int) -> String,

    // WeatherCards
    val maxTemp: String,
    val avgTemp: String,
    val minTemp: String,
    val avgWind: String,
    val sustWind: String,
    val maxWind: String,
    val noDate: String,

    // DaySummaryRow
    val noDetails: String,
    val solarEnergy: (String) -> String,
    val lowVisibilityDesc: String,
    val lowVisibilityHours: (Int) -> String,
    val collapse: String,
    val expand: String,

    // CalendarSelectors
    val selectYear: String,
    val selectMonth: String,
    val noDataAvailable: String,
    val monthFallbackNa: String,
    val monthFallbackNumber: (Int) -> String
) {
    companion object {
        val English = AppStrings(
            // Common / Shared
            ok = "OK",
            cancel = "Cancel",
            delete = "Delete",
            back = "Back",
            save = "Save",
            downloading = "Downloading...",
            appTitle = "ReWinds",
            dismiss = "Dismiss",
            download = "Download",

            // HomeView
            savedLocations = "Saved Locations",
            searchPlaceholder = "Search for new locations...",
            apiError = "API Error",
            deletePlaceTitle = "Delete Place",
            deletePlaceMessage = { name -> "Are you sure you want to delete '$name'? This action cannot be undone." },
            settingsIconDesc = "Settings",
            chatIconDesc = "AI Chat",
            daysStored = { count -> if (count == 1) "$count day stored data" else "$count days stored data" },

            // ChatView
            chatTitle = "Chat",
            messagePlaceholder = "Type your message...",
            sendMessageDesc = "Send message",
            apiKeyNotConfigured = "API Key Not Configured",
            apiKeyNotConfiguredMessage = "To use the AI Chat feature, you need to set your Anthropic API key. Run the app with:\n\nexport ANTHROPIC_API_KEY=sk-ant-<your-key>\n\nThen restart the app.",
            warningIconDesc = "Warning",

            // SettingsView
            settingsTitle = "Settings",
            languageTitle = "Language",
            anthropicKeyTitle = "Anthropic API Key",
            anthropicKeyDescription = "Enter your Anthropic API key to use the AI Chat feature. Your key will be securely stored locally on your device.",
            apiKeyConfigured = "\u2713 API key is configured",
            apiKeyNotConfiguredStatus = "\u26A0 API key not configured",
            apiKeySaved = "\u2713 API key saved successfully",
            apiKeyPlaceholder = "sk-ant-...",
            apiKeyFieldLabel = "API Key",
            anthropicApiUrl = "Get your API key from: https://console.anthropic.com/account/keys",
            saveKey = "Save Key",
            visualCrossingKeyTitle = "Visual Crossing API Key",
            visualCrossingKeyDescription = "Enter your Visual Crossing API key to enable weather data queries. Your key will be securely stored locally on your device.",
            visualCrossingApiUrl = "Get your API key from: https://www.visualcrossing.com/",
            deleteKeyTitle = { keyType -> "Delete $keyType?" },
            deleteKeyMessage = { keyType -> "This will remove your stored $keyType. You can add it again later from settings." },

            // PlaceSummaryView
            infoIconDesc = "Info",
            downloadFullMonth = "Download full month?",
            loadingSummary = "Loading weather summary...",
            errorLabel = "Error:",
            noStoredDays = "No stored days",
            daysFraction = { present, total -> "$present/$total days" },
            tempDisplay = { temp -> "Temp: ${temp}\u00B0C" },
            missingDaysMessage = { month, year, missing -> "$month $year is missing $missing day(s). Download missing data?" },
            notDownloadedMessage = { month, year -> "$month $year is not yet downloaded. Download now?" },

            // Month Names - Short
            monthShortJan = "Jan",
            monthShortFeb = "Feb",
            monthShortMar = "Mar",
            monthShortApr = "Apr",
            monthShortMay = "May",
            monthShortJun = "Jun",
            monthShortJul = "Jul",
            monthShortAug = "Aug",
            monthShortSep = "Sep",
            monthShortOct = "Oct",
            monthShortNov = "Nov",
            monthShortDec = "Dec",

            // Month Names - Full
            monthJanuary = "January",
            monthFebruary = "February",
            monthMarch = "March",
            monthApril = "April",
            monthMay = "May",
            monthJune = "June",
            monthJuly = "July",
            monthAugust = "August",
            monthSeptember = "September",
            monthOctober = "October",
            monthNovember = "November",
            monthDecember = "December",

            // MonthlyStatisticsView
            monthlySummary = "Monthly Summary",
            kiteableDays = { count -> "Kiteable Days: $count" },
            generalStats = "General Stats",
            dailyBreakdown = "Daily Breakdown",
            daysWithData = { count -> "Days with data: $count" },
            avgMinTemp = { temp -> "Average Min Temp: $temp" },
            avgMaxTemp = { temp -> "Average Max Temp: $temp" },
            overallAvgTemp = { temp -> "Overall Average Temp: $temp" },
            coldestDay = { temp, date -> "Coldest Day: $temp (on $date)" },
            hottestDay = { temp, date -> "Hottest Day: $temp (on $date)" },
            totalSolarEnergy = { energy -> "Total Solar Energy: $energy kWh/m\u00B2" },
            noWeatherData = "No detailed weather data available for calculations in this month, or data is still loading.",
            downloadMissingDays = { count -> if (count == 1) "Download $count missing day" else "Download $count missing days" },

            // WeatherCards
            maxTemp = "Max Temp",
            avgTemp = "Avg Temp",
            minTemp = "Min Temp",
            avgWind = "Avg Wind",
            sustWind = "Sust. Wind",
            maxWind = "Max Wind",
            noDate = "No date",

            // DaySummaryRow
            noDetails = "No details",
            solarEnergy = { value -> "Solar Energy: $value" },
            lowVisibilityDesc = "Low Visibility",
            lowVisibilityHours = { hours -> "Low visibility for $hours hour(s)" },
            collapse = "Collapse",
            expand = "Expand",

            // CalendarSelectors
            selectYear = "Select Year:",
            selectMonth = "Select Month:",
            noDataAvailable = "No data available.",
            monthFallbackNa = "N/A",
            monthFallbackNumber = { num -> "Month $num" }
        )

        val German = AppStrings(
            // Common / Shared
            ok = "OK",
            cancel = "Abbrechen",
            delete = "L\u00F6schen",
            back = "Zur\u00FCck",
            save = "Speichern",
            downloading = "Herunterladen...",
            appTitle = "ReWinds",
            dismiss = "Schlie\u00DFen",
            download = "Herunterladen",

            // HomeView
            savedLocations = "Gespeicherte Orte",
            searchPlaceholder = "Neue Orte suchen...",
            apiError = "API-Fehler",
            deletePlaceTitle = "Ort l\u00F6schen",
            deletePlaceMessage = { name -> "M\u00F6chten Sie '$name' wirklich l\u00F6schen? Diese Aktion kann nicht r\u00FCckg\u00E4ngig gemacht werden." },
            settingsIconDesc = "Einstellungen",
            chatIconDesc = "KI-Chat",
            daysStored = { count -> if (count == 1) "$count Tag gespeichert" else "$count Tage gespeichert" },

            // ChatView
            chatTitle = "Chat",
            messagePlaceholder = "Nachricht eingeben...",
            sendMessageDesc = "Nachricht senden",
            apiKeyNotConfigured = "API-Schl\u00FCssel nicht konfiguriert",
            apiKeyNotConfiguredMessage = "Um den KI-Chat zu nutzen, m\u00FCssen Sie Ihren Anthropic-API-Schl\u00FCssel festlegen. Starten Sie die App mit:\n\nexport ANTHROPIC_API_KEY=sk-ant-<Ihr-Schl\u00FCssel>\n\nStarten Sie die App dann neu.",
            warningIconDesc = "Warnung",

            // SettingsView
            settingsTitle = "Einstellungen",
            languageTitle = "Sprache",
            anthropicKeyTitle = "Anthropic API-Schl\u00FCssel",
            anthropicKeyDescription = "Geben Sie Ihren Anthropic-API-Schl\u00FCssel ein, um den KI-Chat zu nutzen. Ihr Schl\u00FCssel wird sicher lokal auf Ihrem Ger\u00E4t gespeichert.",
            apiKeyConfigured = "\u2713 API-Schl\u00FCssel ist konfiguriert",
            apiKeyNotConfiguredStatus = "\u26A0 API-Schl\u00FCssel nicht konfiguriert",
            apiKeySaved = "\u2713 API-Schl\u00FCssel erfolgreich gespeichert",
            apiKeyPlaceholder = "sk-ant-...",
            apiKeyFieldLabel = "API-Schl\u00FCssel",
            anthropicApiUrl = "Holen Sie sich Ihren API-Schl\u00FCssel unter: https://console.anthropic.com/account/keys",
            saveKey = "Schl\u00FCssel speichern",
            visualCrossingKeyTitle = "Visual Crossing API-Schl\u00FCssel",
            visualCrossingKeyDescription = "Geben Sie Ihren Visual Crossing API-Schl\u00FCssel ein, um Wetterdatenabfragen zu aktivieren. Ihr Schl\u00FCssel wird sicher lokal gespeichert.",
            visualCrossingApiUrl = "Holen Sie sich Ihren API-Schl\u00FCssel unter: https://www.visualcrossing.com/",
            deleteKeyTitle = { keyType -> "$keyType l\u00F6schen?" },
            deleteKeyMessage = { keyType -> "Dadurch wird Ihr gespeicherter $keyType entfernt. Sie k\u00F6nnen ihn sp\u00E4ter erneut \u00FCber die Einstellungen hinzuf\u00FCgen." },

            // PlaceSummaryView
            infoIconDesc = "Info",
            downloadFullMonth = "Ganzen Monat herunterladen?",
            loadingSummary = "Wetterzusammenfassung wird geladen...",
            errorLabel = "Fehler:",
            noStoredDays = "Keine gespeicherten Tage",
            daysFraction = { present, total -> "$present/$total Tage" },
            tempDisplay = { temp -> "Temp: ${temp}\u00B0C" },
            missingDaysMessage = { month, year, missing -> "$month $year fehlt $missing Tag(e). Fehlende Daten herunterladen?" },
            notDownloadedMessage = { month, year -> "$month $year wurde noch nicht heruntergeladen. Jetzt herunterladen?" },

            // Month Names - Short
            monthShortJan = "Jan",
            monthShortFeb = "Feb",
            monthShortMar = "M\u00E4r",
            monthShortApr = "Apr",
            monthShortMay = "Mai",
            monthShortJun = "Jun",
            monthShortJul = "Jul",
            monthShortAug = "Aug",
            monthShortSep = "Sep",
            monthShortOct = "Okt",
            monthShortNov = "Nov",
            monthShortDec = "Dez",

            // Month Names - Full
            monthJanuary = "Januar",
            monthFebruary = "Februar",
            monthMarch = "M\u00E4rz",
            monthApril = "April",
            monthMay = "Mai",
            monthJune = "Juni",
            monthJuly = "Juli",
            monthAugust = "August",
            monthSeptember = "September",
            monthOctober = "Oktober",
            monthNovember = "November",
            monthDecember = "Dezember",

            // MonthlyStatisticsView
            monthlySummary = "Monatliche Zusammenfassung",
            kiteableDays = { count -> "Drachen-Tage: $count" },
            generalStats = "Allgemeine Statistiken",
            dailyBreakdown = "Tages\u00FCbersicht",
            daysWithData = { count -> "Tage mit Daten: $count" },
            avgMinTemp = { temp -> "Durchschn. Mindesttemperatur: $temp" },
            avgMaxTemp = { temp -> "Durchschn. H\u00F6chsttemperatur: $temp" },
            overallAvgTemp = { temp -> "Gesamtdurchschnittstemperatur: $temp" },
            coldestDay = { temp, date -> "K\u00E4ltester Tag: $temp (am $date)" },
            hottestDay = { temp, date -> "W\u00E4rmster Tag: $temp (am $date)" },
            totalSolarEnergy = { energy -> "Gesamte Sonnenenergie: $energy kWh/m\u00B2" },
            noWeatherData = "Keine detaillierten Wetterdaten f\u00FCr Berechnungen in diesem Monat verf\u00FCgbar, oder die Daten werden noch geladen.",
            downloadMissingDays = { count -> if (count == 1) "$count fehlenden Tag herunterladen" else "$count fehlende Tage herunterladen" },

            // WeatherCards
            maxTemp = "Max Temp",
            avgTemp = "Durchschn. Temp",
            minTemp = "Min Temp",
            avgWind = "Durchschn. Wind",
            sustWind = "Anhalte. Wind",
            maxWind = "Max Wind",
            noDate = "Kein Datum",

            // DaySummaryRow
            noDetails = "Keine Details",
            solarEnergy = { value -> "Sonnenenergie: $value" },
            lowVisibilityDesc = "Geringe Sichtweite",
            lowVisibilityHours = { hours -> "Geringe Sichtweite f\u00FCr $hours Stunde(n)" },
            collapse = "Zuklappen",
            expand = "Ausklappen",

            // CalendarSelectors
            selectYear = "Jahr ausw\u00E4hlen:",
            selectMonth = "Monat ausw\u00E4hlen:",
            noDataAvailable = "Keine Daten verf\u00FCgbar.",
            monthFallbackNa = "N/V",
            monthFallbackNumber = { num -> "Monat $num" }
        )
    }
}
