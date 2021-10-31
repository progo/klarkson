package klarksonmainframe

import org.junit.Test
import java.util.*
import kotlin.test.assertTrue

class QueryParseTest {

    private val queries = listOf(
        "\\a punk freud \\b timeless",
        "\\b timeless",
        "\\a \"pink floyd\" \\b dark side",
        "\\a \"pink floyd\" \\b dark side \\r 42min",
        "foobar fez",
        "whats this gonna \\a do here"
    )

    private fun printout(block : (String) -> Iterable<String>) {
        queries.forEach { q ->
            println("")
            println("\"$q\"")
            println("----------------------------------------------")
            block(q).forEachIndexed { ind, value ->
                println("$ind => $value")
            }
        }
    }

    @Test
    fun testStringTokenizer() {

        printout { s ->
            val st = StringTokenizer(s, "\\", true)
            val s = st.toList().map { it.toString() }
            s
        }

    }


    @Test
    fun testRegex() {

    }


    @Test
    fun testSubstringOperations() {
        fun parse(s: String) : List<String> {
            if (s.isBlank())
                return listOf()

            val pos1 = s.indexOf("\\")
            if (pos1 < 0) {
                return listOf(s)
            }

            val pos2 = s.indexOf("\\", pos1 + 1)
            if (pos2 < 0) {
                return listOf(s.substring(pos1))
            }

            return listOf(s.substring(pos1, pos2)) + parse(s.substring(pos2))
        }

        printout { s -> parse(s) }
    }

    @Test
    fun testSplit() {
        printout { s -> s.splitWithDelims("\\") }
    }
}