package core

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

// iOS
// Write initialize code in your kotlin mpp project.
fun initLogger() {
    Napier.base(DebugAntilog())
}