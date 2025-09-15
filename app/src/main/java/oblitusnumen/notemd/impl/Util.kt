package oblitusnumen.notemd.impl

fun String.countLeadingWhitespaces(): Int = this.takeWhile { it.isWhitespace() }.length

fun String.countLeading(char: Char): Int = this.takeWhile { it == char }.length

fun String.trimLeadingWhitespaces(): String = this.dropWhile { it.isWhitespace() }

fun String.trimLeading(char: Char): String = this.dropWhile { it == char }

fun String.normalizeWhitespaces(): String = this.replace(Regex(" +"), " ")

fun String.addLeadingSpaces(count: Int): String = " ".repeat(count) + this

fun String.countTrailing(char: Char): Int = this.reversed().takeWhile { it == char }.length

data class LinkSplitResult(
    val around: String,
    val index: Int?,
    val linkSequence: String?
)

fun removeFirstMarkdownLink(input: String): LinkSplitResult {
    // Regex to match [text](text)
    val regex = Regex("""\[[^\]]*]\([^)]*\)""")

    val match = regex.find(input)
    return if (match != null) {
        val newString = input.removeRange(match.range)
        // return Triple of (newString, index, matched text)
        LinkSplitResult(newString, match.range.first, match.value)
    } else {
        // no match found
        LinkSplitResult(input, null, null)
    }
}

fun getTrailingSymbols(input: String): String {
    // Regex: match one or more of _*~ at the END of the string
    val regex = Regex("""[_*~]+$""")
    return regex.find(input)?.value ?: ""
}

data class BacktickSplitResult(
    val before: String,
    val inside: String?,
    val after: String
)

fun splitBacktickSequence(input: String): BacktickSplitResult {
    // Regex explanation:
    // (`+)    → group 1 = one or more backticks
    // (.*?)   → group 2 = minimal content inside
    // \1      → same number of backticks as group 1 (backreference)
    val regex = Regex("(`+)(.*?)\\1", setOf(RegexOption.DOT_MATCHES_ALL))

    val match = regex.find(input) ?: return BacktickSplitResult(input, null, "")

    val before = input.substring(0, match.range.first)
    val inside = match.groupValues[2]
    val after = input.substring(match.range.last + 1)

    return BacktickSplitResult(before, inside, after)
}