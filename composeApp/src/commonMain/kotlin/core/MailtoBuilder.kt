package core

/**
 * KMP-safe builder for `mailto:` URLs with a percent-encoded `subject`/`body` query.
 *
 * Extracted here (rather than inline in the platform actuals) so the encoding is written once
 * and unit-tested on the JVM. `android.net.Uri` and `NSURL` both accept a fully-formed,
 * already-encoded string, so the actuals only need to hand this off to their URL type.
 */
object MailtoBuilder {

    /**
     * Builds `mailto:<recipient>?subject=<enc>&body=<enc>`. The recipient is assumed to be a
     * plain address and is not encoded; [subject] and [body] are percent-encoded per RFC 3986.
     */
    fun build(recipient: String, subject: String, body: String): String =
        "mailto:$recipient?subject=${encode(subject)}&body=${encode(body)}"

    /**
     * Percent-encodes a string for use in a URL query component. Unreserved characters
     * (RFC 3986 `A–Z a–z 0–9 - _ . ~`) pass through; everything else — including spaces and
     * newlines — is encoded as its UTF-8 bytes in `%HH` form.
     */
    fun encode(value: String): String {
        val builder = StringBuilder(value.length)
        for (byte in value.encodeToByteArray()) {
            val b = byte.toInt() and 0xFF
            val c = b.toChar()
            if (c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' ||
                c == '-' || c == '_' || c == '.' || c == '~'
            ) {
                builder.append(c)
            } else {
                builder.append('%')
                builder.append(HEX[b shr 4])
                builder.append(HEX[b and 0x0F])
            }
        }
        return builder.toString()
    }

    private val HEX = "0123456789ABCDEF".toCharArray()
}
