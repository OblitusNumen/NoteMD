package oblitusnumen.notemd.impl

fun String.countLeadingWhitespaces(): Int = this.takeWhile { it.isWhitespace() }.length

fun String.countLeading(char: Char): Int = this.takeWhile { it == char }.length

fun String.trimLeadingWhitespaces(): String = this.dropWhile { it.isWhitespace() }

fun String.trimLeading(char: Char): String = this.dropWhile { it == char }

fun String.normalizeWhitespaces(): String = this.replace(Regex(" +"), " ")

fun String.addLeadingSpaces(count: Int): String = " ".repeat(count) + this

fun String.countTrailing(char: Char): Int = this.reversed().takeWhile { it == char }.length

fun removeFirstMarkdownLink(input: String): Triple<String, Int?, String?> {
    // Regex to match [text](text)
    val regex = Regex("""\[[^\]]*]\([^)]*\)""")

    val match = regex.find(input)
    return if (match != null) {
        val newString = input.removeRange(match.range)
        // return Triple of (newString, index, matched text)
        Triple(newString, match.range.first, match.value)
    } else {
        // no match found
        Triple(input, null, null)
    }
}

fun getTrailingSymbols(input: String): String {
    // Regex: match one or more of _*~ at the END of the string
    val regex = Regex("""[_*~]+$""")
    return regex.find(input)?.value ?: ""
}