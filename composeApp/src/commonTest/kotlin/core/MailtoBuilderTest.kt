package core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [MailtoBuilder] percent-encoding (KIM-334). The iOS actual hands the built
 * string straight to NSURL, so a malformed encoding would silently fail to open the mail client
 * on device — hence the RFC-3986 coverage here.
 */
class MailtoBuilderTest {

    @Test
    fun encode_leavesUnreservedCharactersUntouched() {
        assertEquals("AZaz09-_.~", MailtoBuilder.encode("AZaz09-_.~"))
    }

    @Test
    fun encode_percentEncodesSpaceAsUppercaseHex() {
        assertEquals("ReWinds%20Feedback", MailtoBuilder.encode("ReWinds Feedback"))
    }

    @Test
    fun encode_encodesNewlineAndReservedChars() {
        // Newline -> %0A, '&' -> %26, '=' -> %3D, '?' -> %3F, '/' -> %2F
        assertEquals("a%0Ab%26c%3Dd%3Fe%2Ff", MailtoBuilder.encode("a\nb&c=d?e/f"))
    }

    @Test
    fun encode_encodesMultibyteUtf8AsBytes() {
        // "ü" is U+00FC -> UTF-8 bytes C3 BC.
        assertEquals("%C3%BC", MailtoBuilder.encode("ü"))
    }

    @Test
    fun build_producesRecipientAndEncodedQuery() {
        val url = MailtoBuilder.build(
            recipient = "apps@goaheadand.dev",
            subject = "ReWinds Feedback",
            body = "Hello & welcome"
        )
        assertEquals(
            "mailto:apps@goaheadand.dev?subject=ReWinds%20Feedback&body=Hello%20%26%20welcome",
            url
        )
    }

    @Test
    fun build_recipientIsNotEncoded() {
        // A plain address must remain readable; only subject/body get encoded.
        val url = MailtoBuilder.build("a@b.dev", "s", "b")
        assertTrue(url.startsWith("mailto:a@b.dev?"))
    }
}
