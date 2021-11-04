package klarksonmainframe

import java.awt.Color
import java.awt.Font
import javax.swing.*

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

/**
 * Do the same as JList.locationToIndex but also check against empty.
 * Only gonna work well when fixedCellHeight defined.
 */
fun JList<*>.realIndexUnderPoint(p : java.awt.Point) : Int {
    // Check if pointer is really over an item.
    // If the list is fully populated, requiring scrollbars,
    // this logic is not valid, but luckily it works out.

    val contentsHeight = model.size * fixedCellHeight
    if (p.y >= contentsHeight) {
        return -1
    }

    return locationToIndex(p)
}


/**
 * Show a message popup that will go away after delay.
 */
fun showMessage(msg: String, timeMillis : Int = 2000) {

    val msgfont = Font("sans serif", Font.PLAIN, 20)

    val tip = JToolTip().apply {
        tipText = msg
        font = msgfont
        foreground = Color.BLACK
        background = Color.ORANGE
    }

    val apprxWidth = klarksonFrame
        .graphics
        .getFontMetrics(msgfont)
        .stringWidth(msg)

    val x = klarksonFrame.width - apprxWidth - 25
    val y = 80

    val p = PopupFactory.getSharedInstance().getPopup(klarksonFrame, tip, x, y)
    p.show()

    swingDelay(timeMillis) {
        p.hide()
    }
}