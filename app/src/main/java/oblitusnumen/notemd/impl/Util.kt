package oblitusnumen.notemd.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(stripWidth, size.height)
        )
    }
)

@Composable
fun Table(
    headers: List<String>,
    rows: List<List<String>>
) {
    Column(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.surfaceBright) // outer border
    ) {
        // Header row
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5F)) // header background
                .fillMaxWidth()
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .weight(1f) // equal column width
                        .border(0.5.dp, MaterialTheme.colorScheme.surfaceBright)
                        .padding(8.dp)
                )
            }
        }

        // Data rows
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, MaterialTheme.colorScheme.surfaceBright)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        this.modifier()
    } else {
        this
    }
}
