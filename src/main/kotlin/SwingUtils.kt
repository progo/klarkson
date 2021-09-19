package klarksonmainframe

import javax.swing.Timer

/**
 * Build a Swing Timer that runs after [delayMs] ms and runs the function body.
 */
fun swingDelay(delayMs: Int, func: () -> Unit) {
    Timer(delayMs) { _ -> func() } . apply {
        isRepeats = false
        start()
    }
}
// Kotlin has some trouble with the types and override if we should use both forms
// fun swingDelay(delayMs: Int, func: () -> Unit) = swingDelay(delayMs) { _ -> func() }

