package core

/**
 * Thrown when a network request fails.
 * [httpStatus] is set for HTTP-level failures (e.g. 401, 403, 429) so callers
 * can distinguish auth errors from generic connectivity problems.
 */
class NetworkException(
    message: String,
    cause: Throwable? = null,
    val httpStatus: Int? = null
) : Exception(message, cause)
