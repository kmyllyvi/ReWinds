package core

import io.github.aakira.napier.Napier

// abstracting Logger
object Log {
    fun d(message: String, tag: String? = null) {
        Napier.d(message, tag=tag)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Napier.e(message, throwable)
    }
}