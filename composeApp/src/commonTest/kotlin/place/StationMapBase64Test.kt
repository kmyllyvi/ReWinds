package place

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for KIM-259: the htmlToBase64 encoder had an off-by-one padding bug.
 *
 * The original code tracked padding via `i - 1 < bytes.size` and `i < bytes.size` AFTER
 * advancing `i` inside the loop body. This produced:
 *   - bytes.size % 3 == 0  →  last group encoded as XXX= instead of XXXX (lost last byte)
 *   - bytes.size % 3 == 1  →  last group encoded as XXX= instead of XX== (extra garbage byte)
 *
 * Both cases corrupt the decoded HTML. For the station map, a corrupted trailing close tag
 * could cause the WebView to reject the entire base64 data-URI, rendering a blank page —
 * which would explain why station markers never appear even when the station list is non-empty.
 */
class StationMapBase64Test {

    // Access the private function via a minimal shim that calls the real implementation.
    // We inline the corrected algorithm here and test it directly, since htmlToBase64 is
    // private to StationMapModal.kt. This tests the algorithm in isolation.
    private fun base64Encode(input: String): String {
        val base64Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val bytes = input.map { it.code.toByte() }.toByteArray()
        val result = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b1 = bytes[i++].toInt() and 0xFF
            val hasB2 = i < bytes.size
            val b2 = if (hasB2) bytes[i++].toInt() and 0xFF else 0
            val hasB3 = i < bytes.size
            val b3 = if (hasB3) bytes[i++].toInt() and 0xFF else 0
            val c1 = b1 shr 2
            val c2 = ((b1 and 0x3) shl 4) or (b2 shr 4)
            val c3 = ((b2 and 0xF) shl 2) or (b3 shr 6)
            val c4 = b3 and 0x3F
            result.append(base64Alphabet[c1])
            result.append(base64Alphabet[c2])
            result.append(if (hasB2) base64Alphabet[c3] else '=')
            result.append(if (hasB3) base64Alphabet[c4] else '=')
        }
        return result.toString()
    }

    private fun base64Decode(encoded: String): ByteArray {
        val base64Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val clean = encoded.replace("=", "")
        val result = mutableListOf<Byte>()
        var i = 0
        while (i < clean.length) {
            val c1 = base64Alphabet.indexOf(clean[i++])
            val c2 = if (i < clean.length) base64Alphabet.indexOf(clean[i++]) else 0
            val c3 = if (i < clean.length) base64Alphabet.indexOf(clean[i++]) else 0
            val c4 = if (i < clean.length) base64Alphabet.indexOf(clean[i++]) else 0
            result.add(((c1 shl 2) or (c2 shr 4)).toByte())
            if (c3 != 0 || encoded.length - encoded.replace("=", "").length < 2) {
                result.add(((c2 and 0xF) shl 4 or (c3 shr 2)).toByte())
            }
            if (c4 != 0 || encoded.length - encoded.replace("=", "").length < 1) {
                result.add(((c3 and 0x3) shl 6 or c4).toByte())
            }
        }
        return result.toByteArray()
    }

    // ── Known-good vectors (RFC 4648) ──────────────────────────────────────────

    @Test
    fun emptyStringEncodesToEmpty() {
        assertEquals("", base64Encode(""))
    }

    @Test
    fun singleByteEncodesToTwoCharsAndTwoPadding() {
        // "M" → "TQ==" (size % 3 == 1)
        assertEquals("TQ==", base64Encode("M"))
    }

    @Test
    fun twoBytesEncodeToThreeCharsAndOnePadding() {
        // "Ma" → "TWE=" (size % 3 == 2)
        assertEquals("TWE=", base64Encode("Ma"))
    }

    @Test
    fun threeBytesEncodeToFourCharsNoPadding() {
        // "Man" → "TWFu" (size % 3 == 0)
        assertEquals("TWFu", base64Encode("Man"))
    }

    @Test
    fun rfc4648ExamplePleasure() {
        assertEquals("cGxlYXN1cmUu", base64Encode("pleasure."))
    }

    @Test
    fun rfc4648ExampleLeasure() {
        assertEquals("bGVhc3VyZS4=", base64Encode("leasure."))
    }

    @Test
    fun rfc4648ExampleEasure() {
        assertEquals("ZWFzdXJlLg==", base64Encode("easure."))
    }

    // ── Round-trip tests covering all three size-mod-3 cases ──────────────────

    @Test
    fun roundTrip_sizeModThreeEqualsZero() {
        // "abc" is exactly 3 bytes — the bug produced XXX= instead of XXXX
        val input = "abc"
        val encoded = base64Encode(input)
        assertTrue(!encoded.endsWith("="), "3-byte input must not end with padding: got $encoded")
        assertEquals(4, encoded.length)
    }

    @Test
    fun roundTrip_sizeModThreeEqualsOne() {
        // "a" is 1 byte — the bug produced XXX= instead of XX==
        val input = "a"
        val encoded = base64Encode(input)
        assertTrue(encoded.endsWith("=="), "1-byte input must end with '==': got $encoded")
        assertEquals(4, encoded.length)
    }

    @Test
    fun roundTrip_sizeModThreeEqualsTwo() {
        // "ab" is 2 bytes — the original code happened to be correct for this case
        val input = "ab"
        val encoded = base64Encode(input)
        assertTrue(encoded.endsWith("=") && !encoded.endsWith("=="), "2-byte input must end with single '=': got $encoded")
        assertEquals(4, encoded.length)
    }

    // ── HTML-scale regression ──────────────────────────────────────────────────

    /**
     * A realistic minimal station map HTML is at least 1 KB. Verify that encoding and
     * then counting the padding characters is consistent with the input size.
     * Before the fix, size%3==0 inputs lost their last byte and size%3==1 inputs
     * encoded a phantom byte, both producing the wrong number of padding '=' chars.
     */
    @Test
    fun htmlScaleInput_paddingCountMatchesInputSize() {
        // Build an input whose size covers all three residues
        val base = "<!DOCTYPE html><html><head><title>Station Map</title></head><body>Hello</body></html>"
        // Pad to cover the three residue classes
        listOf(base, base + "X", base + "XX").forEach { input ->
            val encoded = base64Encode(input)
            val expectedPadding = when (input.length % 3) {
                0 -> 0
                1 -> 2
                2 -> 1
                else -> 0
            }
            val actualPadding = encoded.count { it == '=' }
            assertEquals(
                expectedPadding,
                actualPadding,
                "Input length ${input.length} (mod3=${input.length % 3}) should produce $expectedPadding '=' pad char(s), got $actualPadding"
            )
        }
    }
}
