package core

/**
 * Injectable seam over the Anthropic API-key configuration check.
 *
 * Production reads the platform key store via [isAnthropicApiKeyConfigured]; tests substitute a
 * fixed value so the "key present" / "key missing" branches (KIM-293 journeys J7, J10) are
 * deterministic and don't depend on the developer's local `gradle.properties` / Keychain state.
 */
fun interface ApiKeyChecker {
    fun isAnthropicKeyConfigured(): Boolean
}

/** Default checker bound in production: delegates to the platform key lookup. */
object PlatformApiKeyChecker : ApiKeyChecker {
    override fun isAnthropicKeyConfigured(): Boolean = isAnthropicApiKeyConfigured()
}
