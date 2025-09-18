package oblitusnumen.notemd.impl

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

fun String.countLeadingWhitespaces(): Int = this.takeWhile { it.isWhitespace() }.length

fun String.countLeading(char: Char): Int = this.takeWhile { it == char }.length

fun String.trimLeadingWhitespaces(): String = this.dropWhile { it.isWhitespace() }

fun String.trimLeading(char: Char): String = this.dropWhile { it == char }

fun String.normalizeWhitespaces(): String = this.replace(Regex(" +"), " ")

fun String.addLeadingSpaces(count: Int): String = " ".repeat(count) + this

fun String.countTrailing(char: Char): Int = this.reversed().takeWhile { it == char }.length

data class LinkSplitResult(
    val before: String,
    val isPic: Boolean?,
    val linkText: String,
    val link: String,
    val after: String
)

fun parseMarkdownLink(input: String): LinkSplitResult {
    // Regex to match [text](text)
    val regex = Regex("""(.*?)(!)?\[([^]]+]\([^)]+)\)(.*)""", setOf(RegexOption.DOT_MATCHES_ALL))

    val match = regex.find(input) ?: return LinkSplitResult(input, null, "", "", "")

    val (a, before, pic, link, after) = match.groupValues
    val linkIndex = link.lastIndexOf("](")
    return LinkSplitResult(
        before,
        pic.isNotEmpty(),
        link.substring(0, linkIndex),
        link.substring(linkIndex + 2), after
    )
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

fun Modifier.leftSideColor(color: Color, width: Dp) = this.then(
    Modifier.drawBehind {
        val stripWidth = width.toPx()
        drawRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(stripWidth, size.height)
        )
    }
)

data class ListElementParseResult(val number: Int?, val symbol: Char, val checked: Boolean?, val text: String, val textOffset: Int)

fun parseListElement(line: String): ListElementParseResult? {
    val regex = Regex("""^((([1-9]\d*)([.)])|([-*+]))(\s*\[([ xX])])? )(.*)$""")
    val match = regex.find(line)
    return match?.let {
        println(it.groupValues)
        ListElementParseResult(
            it.groupValues[3].ifEmpty { null }?.toInt(),
            it.groupValues[5].firstOrNull() ?: it.groupValues[4].first(),
            it.groupValues[7].ifEmpty { null }?.equals("x", true),
            it.groupValues[8],
            it.groupValues[1].length
        )
    }
}
