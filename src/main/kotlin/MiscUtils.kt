package klarksonmainframe

// From https://stackoverflow.com/a/23088000
fun Double.format(digits: Int) = "%.${digits}f".format(this)