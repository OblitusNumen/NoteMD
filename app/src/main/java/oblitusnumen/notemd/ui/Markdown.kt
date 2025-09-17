package oblitusnumen.notemd.ui

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import oblitusnumen.notemd.impl.*

fun parseMarkdown(appContext: android.content.Context, markdown: String): List<@Composable () -> Unit> {
    val blocks: MutableList<Pair<Context, @Composable () -> Unit>> = mutableListOf()
    val split = markdown.split('\n')
    var context: Context = Context.NONE
    var cache = ""
    var prevListSymbol = ' '
    var previousLevel: Int = -1
    var leadingWhitespace = 0
    var previousWhitespaces = 0
    var previousSpace = 0 // space before the actual text (for nested blocks)
    var checkbox: Int = -1
    var language = ""
    val pushBlock = {
        if (context != Context.NONE) {
            val contextC: Context = context
            val cacheC: String = cache
            val prevListSymbolC: Char = prevListSymbol
            val previousLevelC: Int = previousLevel
            val leadingWhitespaceC: Int = leadingWhitespace
            val previousWhitespacesC: Int = previousWhitespaces
            val previousSpaceC: Int = previousSpace
            val checkboxC: Int = checkbox
            val languageC: String = language
            // FIXME:
            val composable: @Composable () -> Unit =
                when (context) {
                    Context.NONE -> throw RuntimeException("unreachable")
                    Context.TEXT -> {
                        val annotations = generateTextAnnotations(cacheC);
                        {
                            Spacer(modifier = Modifier.height(10.dp))
                            RenderAnnotatedText(
                                appContext,
                                annotations,
                                MaterialTheme.typography.bodyMedium.toParagraphStyle(),
                                MaterialTheme.typography.bodyMedium.toSpanStyle()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    Context.HEADING -> {
                        val textAnnotations = generateTextAnnotations(cacheC)
                        val fontSize = (16 + 8 * (6 - previousLevelC))
                        {
                            Spacer(modifier = Modifier.height(20.dp))
                            RenderAnnotatedText(
                                appContext,
                                textAnnotations,
                                paragraphStyle = MaterialTheme.typography.headlineLarge.copy(
                                    lineHeight = fontSize.sp,
                                    fontSize = fontSize.sp
                                ).toParagraphStyle(),
                                spanStyle = MaterialTheme.typography.headlineLarge.copy(
                                    lineHeight = fontSize.sp,
                                    fontSize = fontSize.sp
                                ).toSpanStyle()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    Context.QUOTE -> {
                        val mdBlocks = parseMarkdown(markdown = cacheC, appContext = appContext);
                        {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                Modifier.background(color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5F))
                                    .fillMaxWidth()
                                    .leftSideColor(Color.Cyan, 4.dp)
                            ) {
                                RenderMarkdownBlocks(modifier = Modifier.padding(10.dp), mdBlocks)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    Context.TEXT_BLOCK -> {
                        {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                Modifier.background(
                                    color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5F),
                                    shape = RoundedCornerShape(4.dp)
                                ).fillMaxWidth()
                            ) {
                                Text(
                                    modifier = Modifier.padding(10.dp),
                                    text = cacheC,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    Context.LIST_ELEMENT -> {
                        val mdBlocks = parseMarkdown(markdown = cacheC, appContext = appContext);
                        {
                            Row(Modifier.padding(start = (24 * previousLevelC).dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .conditional(previousLevelC == 1) {
                                            this.border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                shape = CircleShape
                                            )
                                        }
                                        .conditional(previousLevelC != 1) {
                                            this.background(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                shape = if (previousLevelC == 0) CircleShape else RectangleShape
                                            )
                                        }
                                        .padding(4.dp)
                                )
                                RenderMarkdownBlocks(markdownBlocks = mdBlocks)
                            }
                        }
                    }

                    Context.SPACER -> {
                        {
                            Spacer(Modifier.fillMaxWidth().height(10.dp))
                        }
                    }

                    Context.DIVIDER -> {
                        {
                            HorizontalDivider(Modifier.padding(4.dp))
                        }
                    }

                    Context.TABLE -> {
                        val rows = mutableListOf<List<@Composable () -> Unit>>()
                        var maxInRow = 0
                        cacheC.split('\n').forEach {
                            var row = it.trim()
                            if (row.firstOrNull() == '|')
                                row = row.substring(1)
                            if (row.lastOrNull() == '|')
                                row = row.substring(0, row.lastIndex)
                            val cells: MutableList<@Composable () -> Unit> = mutableListOf()
                            for (string in row.split('|')) {
                                val textAnnotations = generateTextAnnotations(string.trim())
                                cells.add({ RenderAnnotatedText(appContext, textAnnotations) })
                            }
                            rows.add(cells)
                            if (maxInRow < cells.size) {
                                maxInRow = cells.size
                            }
                        }
                        val header = rows.removeAt(0);
                        {
                            Spacer(modifier = Modifier.height(10.dp))
                            Table(header, rows)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    Context.CODE_BLOCK -> {
                        {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                Modifier.background(
                                    color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5F),
                                    shape = RoundedCornerShape(4.dp)
                                ).fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().background(
                                        color = MaterialTheme.colorScheme.surfaceBright,
                                        shape = RoundedCornerShape(4.dp)
                                    ).padding(5.dp)
                                ) {
                                    Text(
                                        text = languageC,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    // TODO: copy button
                                }
                                Text(
                                    modifier = Modifier.padding(5.dp),
                                    text = cacheC,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            blocks.add(context to composable)
            context = Context.NONE
            cache = ""
            prevListSymbol = ' '
            previousLevel = -1
            previousWhitespaces = 0
            previousSpace = 0
            checkbox = -1
            language = ""
        }
    }
    repeat(split.size) { index ->
        var current = split[index]

        // code block
        if (context == Context.CODE_BLOCK) {
            if (current.countLeadingWhitespaces() >= previousWhitespaces) {
                current = current.substring(previousWhitespaces)
            } else
                current = current.trimLeadingWhitespaces()
            if (current.trim().countLeading('`') == previousLevel && current.trim().substring(previousLevel)
                    .isEmpty()
            ) {
                pushBlock()
                return@repeat
            }
            cache += (if (cache.isEmpty()) "" else "\n") + current
            return@repeat
        }

        // trim
        leadingWhitespace = current.countLeadingWhitespaces()
        current = current.trimLeadingWhitespaces() // FIXME: might be not efficient
        if (current.trim().isEmpty()) {
            pushBlock()
            return@repeat
        }

        // todo numbered, checklist
        // bullet list start
        val symbol = current.firstOrNull()
        if (symbol == '+' || symbol == '-' || symbol == '*') {
            var currentForList = current.substring(1)// |- "     [ ] text"| _OR_ |- "     text"|
            var listWhitespaces = currentForList.countLeadingWhitespaces()
            var curSpace = leadingWhitespace + 2
            val checkChecklist = currentForList.substring(listWhitespaces)// "[ ] text"
            var currentcb = -1
            if (checkChecklist.startsWith("[ ] ") || checkChecklist.startsWith("[x] ", true)) {
                currentcb = if (currentForList.startsWith('x', true)) 1 else 0
                currentForList = currentForList.substring(4)// "text"
                curSpace += leadingWhitespace + 5 + listWhitespaces // "|   |-|    |[ ] |"
                listWhitespaces = currentForList.countLeadingWhitespaces() + 1
            }
            if (listWhitespaces != 0) {// - |
                val curLevel = leadingWhitespace / 4
                if (context == Context.LIST_ELEMENT) {
                    if (curLevel > previousLevel) {
                        if (leadingWhitespace - previousWhitespaces > 5) {
                            cache += "\n" + currentForList
                        } else {
                            val curLevel = previousLevel + 1
                            pushBlock()
                            checkbox = currentcb
                            previousLevel = curLevel
                            context = Context.LIST_ELEMENT
                            previousSpace = curSpace
                            cache = currentForList//"text"
                            previousWhitespaces = leadingWhitespace
                            return@repeat
                        }
                    } else if (curLevel == previousLevel) {
                        if (curLevel == 0) {
                            val prevSymbol = prevListSymbol
                            pushBlock()
                            if (prevSymbol != symbol || leadingWhitespace != previousWhitespaces) {
                                context = Context.SPACER
                                pushBlock()
                            }
                            checkbox = currentcb
                            previousLevel = 0
                            prevListSymbol = symbol
                            context = Context.LIST_ELEMENT
                            previousSpace = curSpace
                            cache = currentForList//"text"
                            previousWhitespaces = leadingWhitespace
                            return@repeat
                        }
                        if (leadingWhitespace == previousWhitespaces) {
                            pushBlock()
                            checkbox = currentcb
                            previousLevel = curLevel
                            context = Context.LIST_ELEMENT
                            previousSpace = curSpace
                            cache = currentForList//"text"
                            previousWhitespaces = leadingWhitespace
                            return@repeat
                        } else {
                            cache += "\n" + currentForList
                            return@repeat
                        }
                    } else {
                        cache += "\n" + currentForList
                        return@repeat
                    }
                } else if (curLevel == 0) {
                    val prevSymbol = prevListSymbol
                    pushBlock()
                    if (prevSymbol != symbol) {
                        context = Context.SPACER
                        pushBlock()
                    }
                    checkbox = currentcb
                    previousLevel = 0
                    prevListSymbol = symbol
                    context = Context.LIST_ELEMENT
                    previousSpace = curSpace
                    cache = currentForList//"text"
                    previousWhitespaces = leadingWhitespace
                    return@repeat
                }
            }// else it is continuation of text
        }

        // quote
        if (context == Context.QUOTE) {
            if (current.startsWith('>')) {
                current = current.substring(1)
            } else
                current = " ".repeat(leadingWhitespace) + current
            cache += "\n" + current
            return@repeat
        }

        // list
        if (context == Context.LIST_ELEMENT) {
            if (leadingWhitespace - previousSpace > 0)
                current.addLeadingSpaces(leadingWhitespace - previousSpace)
            cache += "\n" + current
            return@repeat
        }

        // text block
        if (leadingWhitespace >= 4) {
            if (context == Context.TEXT_BLOCK) {
                cache += "\n" + current.addLeadingSpaces(leadingWhitespace - 4)
                return@repeat
            } else if (context != Context.TEXT) {
                pushBlock()
                context = Context.TEXT_BLOCK
                cache += current.addLeadingSpaces(leadingWhitespace - 4)
                return@repeat
            }
        } else if (context == Context.TEXT_BLOCK) pushBlock()

        // heading
        val leadingHashtags = current.countLeading('#')
        if (1 <= leadingHashtags && leadingHashtags <= 6) {
            val heading = current.trimLeading('#')
            if (heading.countLeadingWhitespaces() != 0) {
                if (context != Context.NONE) {
                    pushBlock()
                }
                cache = heading.trimLeadingWhitespaces()
                context = Context.HEADING
                previousLevel = leadingHashtags
                pushBlock()
                return@repeat
            }
        }

        // divider
        if (symbol == '_' || symbol == '-' || symbol == '*') {
            if (current.countLeading(symbol) >= 3 && current.trimLeading(symbol).trim().isEmpty()) {
                if (context == Context.DIVIDER) {
                    if (symbol == '-' && !cache.contains('\n')) {
                        context = Context.HEADING
                        previousLevel = 2
                    }
                    pushBlock()
                    return@repeat
                }
                if (context == Context.TEXT && symbol == '-' && !cache.contains('\n')) {
                    context = Context.HEADING
                    previousLevel = 2
                    pushBlock()
                    return@repeat
                }
                context = Context.DIVIDER
                cache = current
                return@repeat
            } // else not divider
        }
        if (context == Context.DIVIDER) {
            pushBlock()
            return@repeat
        }

        // table
        // FIXME: it might be formatted like
        // a|a
        // -|-
        if (context != Context.TEXT && context != Context.LIST_ELEMENT// && context != Context.QUOTE
            && current.trimEnd().lastOrNull() == '|' && symbol == '|'
        ) {
            if (context == Context.TABLE) {
                if (prevListSymbol == '-') {
                    prevListSymbol = ' '
                    return@repeat
                }
                val curLevel = current.count { it == '|' } - 1
                if (curLevel >= 1) {
                    cache += "\n" + current
                    return@repeat
                }
            } else {
                val pattern = Regex("^\\s*\\|([-]+\\|)+\\s*$")
                if (split.size > index + 1 && pattern.matches(split[index + 1])) {
                    val level = split[index + 1].count { it == '|' } - 1
                    val curLevel = current.count { it == '|' } - 1
                    if (curLevel == level) {
                        pushBlock()
                        previousLevel = level
                        context = Context.TABLE
                        cache = current
                        prevListSymbol = '-'
                        return@repeat
                    }
                }
            }
        }
        if (context == Context.TABLE) {
            pushBlock()
        }

        // quote start
        if (symbol == '>') {
            pushBlock()
            context = Context.QUOTE
            cache = current.substring(1)
            return@repeat
        }

        // code block start
        val level = current.countLeading('`')
        if (level >= 3) {
            pushBlock()
            previousLevel = level
            context = Context.CODE_BLOCK
            language = current.trim().substring(level)
            previousWhitespaces = leadingWhitespace
            return@repeat
        }

        // text
        if (context == Context.NONE) {
            context = Context.TEXT
            cache = current
            return@repeat
        }

        // other handle
        cache += "\n" + current
    }
    pushBlock()
    return blocks.map { it.second }
}

@Composable
fun RenderMarkdownBlocks(modifier: Modifier = Modifier, markdownBlocks: List<@Composable (() -> Unit)>) {
    // FIXME:
    Column(modifier.padding(horizontal = 10.dp)) {
        markdownBlocks.forEach {
            it()
        }
    }
//    LazyColumn(modifier) {
//        items(blocks) {
//            it.second()
//        }
//    }
}

@Composable
fun MarkdownView(modifier: Modifier = Modifier, appContext: android.content.Context, markdown: String) {
    RenderMarkdownBlocks(modifier, parseMarkdown(appContext, markdown))
}

class Pointers {
    var lastStrikethrough = -1
    var lastItalic_ = -1
    var lastItalicStar = -1
    var lastBold_ = -1
    var lastBoldStar = -1
}

fun generateTextAnnotations(text: String): List<Pair<TextAnnotationType, String>> {
    var text = text
        .replace("  \n", "\n\n")
        .normalizeWhitespaces()
        .replace(" \n", "\n")
        .replace('\n', ' ')
        .replace("  ", " \n")
    val annotations = mutableListOf<Pair<TextAnnotationType, String>>()
    val pointers = Pointers()
    while (text.isNotEmpty()) {
        val codeInsertSearchResult = splitBacktickSequence(text)
        val linkSearchResult = parseMarkdownLink(text)
        if (linkSearchResult.before.length == codeInsertSearchResult.before.length) {
            // no links, no backticks
            parseText(annotations, text, pointers)
            break
        }
        if (linkSearchResult.before < codeInsertSearchResult.before) {
            // link is before code
            // parse text before link
            parseText(annotations, linkSearchResult.before, pointers)
            annotations.add((if (linkSearchResult.isPic!!) TextAnnotationType.BEGIN_PIC else TextAnnotationType.BEGIN_LINK) to linkSearchResult.link)
            // parse link text
            parseTextWithCode(annotations, linkSearchResult.linkText, pointers)
            annotations.add(TextAnnotationType.END_LINK to "")
            text = linkSearchResult.after
        } else {
            // code is before link
            parseText(annotations, codeInsertSearchResult.before, pointers)
            annotations.add(TextAnnotationType.BEGIN_CODE to "")
            annotations.add(
                TextAnnotationType.TEXT to codeInsertSearchResult.inside!!.replace('\n', ' ')
                    .replace("  ", " ")
            )
            annotations.add(TextAnnotationType.END_CODE to "")
            text = codeInsertSearchResult.after
        }
    }
    if (pointers.lastStrikethrough != -1) {
        annotations[pointers.lastStrikethrough] =
            TextAnnotationType.TEXT to annotations[pointers.lastStrikethrough].second
        pointers.lastStrikethrough = -1
    }
    if (pointers.lastItalic_ != -1) {
        annotations[pointers.lastItalic_] =
            TextAnnotationType.TEXT to annotations[pointers.lastItalic_].second
        pointers.lastItalic_ = -1
    }
    if (pointers.lastItalicStar != -1) {
        annotations[pointers.lastItalicStar] =
            TextAnnotationType.TEXT to annotations[pointers.lastItalicStar].second
        pointers.lastItalicStar = -1
    }
    if (pointers.lastBold_ != -1) {
        annotations[pointers.lastBold_] =
            TextAnnotationType.TEXT to annotations[pointers.lastBold_].second
        pointers.lastBold_ = -1
    }
    if (pointers.lastBoldStar != -1) {
        annotations[pointers.lastBoldStar] =
            TextAnnotationType.TEXT to annotations[pointers.lastBoldStar].second
        pointers.lastBoldStar = -1
    }
    return annotations
}

fun parseTextWithCode(annotations: MutableList<Pair<TextAnnotationType, String>>, text: String, pointers: Pointers) {
    var part = text
    while (text.isNotEmpty()) {
        val splitResult = splitBacktickSequence(part)
        parseText(annotations, splitResult.before, pointers)
        if (splitResult.inside == null)
            break
        annotations.add(TextAnnotationType.BEGIN_CODE to "")
        annotations.add(
            TextAnnotationType.TEXT to splitResult.inside.replace('\n', ' ').replace("  ", " ")
        )
        annotations.add(TextAnnotationType.END_CODE to "")
        part = splitResult.after
    }
}

fun parseText(annotations: MutableList<Pair<TextAnnotationType, String>>, text: String, pointers: Pointers) {
    val words = text.split(' ')
    val changePreviousAnnotation = { text: String ->
        val lastAnnotation = annotations.lastOrNull()
        if (lastAnnotation != null && lastAnnotation.first == TextAnnotationType.TEXT) {
            annotations[annotations.size - 1] =
                TextAnnotationType.TEXT to lastAnnotation.second + text
        } else {
            annotations.add(TextAnnotationType.TEXT to text)
        }
    }
    repeat(words.size) {
        var word = words[it]
        if (it != 0)
            changePreviousAnnotation(" ")
        if (word.isEmpty()) {
            return@repeat
        }
        var symbols = getTrailingSymbols(word)
        word = word.substring(0, word.length - symbols.length)
        if (word.isEmpty()) {
            val s = symbols.first()
            val countLeading = symbols.countLeading(s)
            if (countLeading != symbols.length) {
                word = symbols.substring(0, countLeading)
                symbols = symbols.substring(countLeading)
            }
        }
        if (word.isNotEmpty()) {
            var firstLoop = true
            while (true) {
                val symbol = word.first()
                val countLeading = word.countLeading(symbol)
                if (word.length > countLeading) {
                    if (symbol == '*') {
                        if (countLeading == 1) {// italic
                            word = word.substring(1)
                            if (pointers.lastItalicStar != -1) {
                                annotations[pointers.lastItalicStar] =
                                    TextAnnotationType.TEXT to annotations[pointers.lastItalicStar].second
                            }
                            annotations.add(TextAnnotationType.BEGIN_ITALIC to "$symbol")
                            pointers.lastItalicStar = annotations.size - 1
                        } else if (countLeading == 2) {// bold
                            word = word.substring(2)
                            if (pointers.lastBoldStar != -1) {
                                annotations[pointers.lastBoldStar] =
                                    TextAnnotationType.TEXT to annotations[pointers.lastBoldStar].second
                            }
                            annotations.add(TextAnnotationType.BEGIN_BOLD to "$symbol$symbol")
                            pointers.lastBoldStar = annotations.size - 1
                        } else {// FIXME: symbols may differ
                            if (countLeading > 3)
                                changePreviousAnnotation(
                                    "$symbol".repeat(
                                        countLeading - 3
                                    )
                                )
                            word = word.substring(countLeading)
                            if (pointers.lastItalicStar != -1) {
                                annotations[pointers.lastItalicStar] =
                                    TextAnnotationType.TEXT to annotations[pointers.lastItalicStar].second
                            }
                            annotations.add(TextAnnotationType.BEGIN_ITALIC to "$symbol")
                            pointers.lastItalicStar = annotations.size - 1
                            if (pointers.lastBoldStar != -1) {
                                annotations[pointers.lastBoldStar] =
                                    TextAnnotationType.TEXT to annotations[pointers.lastBoldStar].second
                            }
                            annotations.add(TextAnnotationType.BEGIN_BOLD to "$symbol$symbol")
                            pointers.lastBoldStar = annotations.size - 1
                        }
                    } else if (symbol == '_') {
                        if (countLeading == 1) {// italic
                            word = word.substring(1)
                            if (pointers.lastItalic_ != -1) {
                                annotations[pointers.lastItalic_] =
                                    TextAnnotationType.TEXT to annotations[pointers.lastItalic_].second
                            }
                            annotations.add(TextAnnotationType.BEGIN_ITALIC to "$symbol")
                            pointers.lastItalic_ = annotations.size - 1
                        } else if (countLeading == 2) {// bold
                            word = word.substring(2)
                            if (pointers.lastBold_ != -1) {
                                annotations[pointers.lastBold_] =
                                    TextAnnotationType.TEXT to annotations[pointers.lastBold_].second
                            }
                            annotations.add(TextAnnotationType.BEGIN_BOLD to "$symbol$symbol")
                            pointers.lastBold_ = annotations.size - 1
                        } else {// FIXME: symbols may differ
                            if (countLeading > 3)
                                changePreviousAnnotation(
                                    "$symbol".repeat(
                                        countLeading - 3
                                    )
                                )
                            word = word.substring(countLeading)
                            if (pointers.lastItalic_ != -1) {
                                annotations[pointers.lastItalic_] =
                                    TextAnnotationType.TEXT to annotations[pointers.lastItalic_].second
                            }
                            annotations.add(TextAnnotationType.BEGIN_ITALIC to "$symbol")
                            pointers.lastItalic_ = annotations.size - 1
                            if (pointers.lastBold_ != -1) {
                                annotations[pointers.lastBold_] =
                                    TextAnnotationType.TEXT to annotations[pointers.lastBold_].second
                            }
                            annotations.add(TextAnnotationType.BEGIN_BOLD to "$symbol$symbol")
                            pointers.lastBold_ = annotations.size - 1
                        }
                    } else if (symbol == '~' && countLeading >= 2) {
                        if (countLeading > 2)
                            changePreviousAnnotation(
                                "$symbol".repeat(
                                    countLeading - 2
                                )
                            )
                        word = word.substring(countLeading)
                        if (pointers.lastStrikethrough != -1) {
                            annotations[pointers.lastStrikethrough] =
                                TextAnnotationType.TEXT to "~~"
                        }
                        annotations.add(TextAnnotationType.BEGIN_STRIKETHROUGH to "~~")
                        pointers.lastStrikethrough = annotations.size - 1
                    } else {
                        changePreviousAnnotation(word)
                        break
                    }
                } else {
                    changePreviousAnnotation(word)
                    break
                }
                firstLoop = false
            }
        }

        // handle end of word
        while (symbols.isNotEmpty()) {
            val symbol = symbols.first()
            var countLeading = symbols.countLeading(symbol)
            symbols = symbols.substring(countLeading)
            if (symbol == '*') {
                if (countLeading == 1) {// italic
                    if (pointers.lastItalicStar != -1) {
                        annotations.add(TextAnnotationType.END_ITALIC to "")
                        pointers.lastItalicStar = -1
                        if (pointers.lastItalic_ != -1) {
                            annotations[pointers.lastItalic_] =
                                TextAnnotationType.TEXT to annotations[pointers.lastItalic_].second
                            pointers.lastItalic_ = -1
                        }
                        countLeading -= 1
                    }
                } else if (countLeading == 2) {// bold
                    if (pointers.lastBoldStar != -1) {
                        annotations.add(TextAnnotationType.END_BOLD to "")
                        pointers.lastBoldStar = -1
                        if (pointers.lastBold_ != -1) {
                            annotations[pointers.lastBold_] =
                                TextAnnotationType.TEXT to annotations[pointers.lastBold_].second
                            pointers.lastBold_ = -1
                        }
                        countLeading -= 2
                    }
                } else {// FIXME: symbols may differ
                    if (pointers.lastBoldStar != -1) {
                        annotations.add(TextAnnotationType.END_BOLD to "")
                        pointers.lastBoldStar = -1
                        if (pointers.lastBold_ != -1) {
                            annotations[pointers.lastBold_] =
                                TextAnnotationType.TEXT to annotations[pointers.lastBold_].second
                            pointers.lastBold_ = -1
                        }
                        countLeading -= 2
                    }
                    if (pointers.lastItalicStar != -1) {
                        annotations.add(TextAnnotationType.END_ITALIC to "")
                        pointers.lastItalicStar = -1
                        if (pointers.lastItalic_ != -1) {
                            annotations[pointers.lastItalic_] =
                                TextAnnotationType.TEXT to annotations[pointers.lastItalic_].second
                            pointers.lastItalic_ = -1
                        }
                        countLeading -= 1
                    }
                }
            } else if (symbol == '_') {
                if (countLeading == 1) {// italic
                    if (pointers.lastItalic_ != -1) {
                        annotations.add(TextAnnotationType.END_ITALIC to "")
                        pointers.lastItalic_ = -1
                        if (pointers.lastItalicStar != -1) {
                            annotations[pointers.lastItalicStar] =
                                TextAnnotationType.TEXT to annotations[pointers.lastItalicStar].second
                            pointers.lastItalicStar = -1
                        }
                        countLeading -= 1
                    }
                } else if (countLeading == 2) {// bold
                    if (pointers.lastBold_ != -1) {
                        annotations.add(TextAnnotationType.END_BOLD to "")
                        pointers.lastBold_ = -1
                        if (pointers.lastBoldStar != -1) {
                            annotations[pointers.lastBoldStar] =
                                TextAnnotationType.TEXT to annotations[pointers.lastBoldStar].second
                            pointers.lastBoldStar = -1
                        }
                        countLeading -= 2
                    }
                } else {// FIXME: symbols may differ
                    if (pointers.lastBold_ != -1) {
                        annotations.add(TextAnnotationType.END_BOLD to "")
                        pointers.lastBold_ = -1
                        if (pointers.lastBoldStar != -1) {
                            annotations[pointers.lastBoldStar] =
                                TextAnnotationType.TEXT to annotations[pointers.lastBoldStar].second
                            pointers.lastBoldStar = -1
                        }
                        countLeading -= 2
                    }
                    if (pointers.lastItalic_ != -1) {
                        annotations.add(TextAnnotationType.END_ITALIC to "")
                        pointers.lastItalic_ = -1
                        if (pointers.lastItalicStar != -1) {
                            annotations[pointers.lastItalicStar] =
                                TextAnnotationType.TEXT to annotations[pointers.lastItalicStar].second
                            pointers.lastItalicStar = -1
                        }
                        countLeading -= 1
                    }
                }
            } else if (countLeading >= 2) {// ~
                if (pointers.lastStrikethrough != -1) {
                    annotations.add(TextAnnotationType.END_STRIKETHROUGH to "")
                    pointers.lastStrikethrough = -1
                    countLeading -= 2
                }
            }
            changePreviousAnnotation("$symbol".repeat(countLeading))
        }
    }
}

enum class Context(isText: Boolean) {
    NONE(false),
    TEXT(true),
    HEADING(true),
    QUOTE(true),
    TEXT_BLOCK(true),
    LIST_ELEMENT(true),
    SPACER(false),
    DIVIDER(false),
    TABLE(false),
    CODE_BLOCK(false),
}

enum class TextAnnotationType {
    TEXT,
    BEGIN_CODE,
    END_CODE,
    BEGIN_STRIKETHROUGH,
    END_STRIKETHROUGH,
    BEGIN_ITALIC,
    END_ITALIC,
    BEGIN_BOLD,
    END_BOLD,
    BEGIN_LINK,
    END_LINK,
    BEGIN_PIC,
}

@Composable
fun buildAnnotated(
    annotations: List<Pair<TextAnnotationType, String>>,
    paragraphStyle: ParagraphStyle?,
    spanStyle: SpanStyle?
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    builder.pushStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface))
    if (paragraphStyle != null) {
        builder.pushStyle(paragraphStyle)
    }
    if (spanStyle != null) {
        builder.pushStyle(spanStyle)
    }
    val active = mutableSetOf<TextAnnotationType>() // currently active styles
    var link = ""

    annotations.forEach { annotation ->
        val type = annotation.first
        val value = annotation.second
        when (type) {
            TextAnnotationType.TEXT -> {
                val start = builder.length
                builder.append(text = value)
                val end = builder.length

                // apply all currently active styles to this chunk
                active.forEach { style ->
                    when (style) {
                        TextAnnotationType.BEGIN_CODE -> {
                            builder.addStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = MaterialTheme.colorScheme.surfaceBright,
                                ),
                                start, end
                            )
                            // TODO: copy on click
                        }

                        TextAnnotationType.BEGIN_STRIKETHROUGH -> {
                            builder.addStyle(
                                SpanStyle(textDecoration = TextDecoration.LineThrough),
                                start, end
                            )
                        }

                        TextAnnotationType.BEGIN_ITALIC -> {
                            builder.addStyle(
                                SpanStyle(fontStyle = FontStyle.Italic),
                                start, end
                            )
                        }

                        TextAnnotationType.BEGIN_BOLD -> {
                            builder.addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold),
                                start, end
                            )
                        }

                        TextAnnotationType.BEGIN_LINK -> {
                            // You can style it visually
                            builder.addStyle(
                                SpanStyle(
                                    color = Color(0xFF1E88E5),
                                    textDecoration = TextDecoration.Underline
                                ),
                                start, end
                            )
                            // And also attach the URL as a StringAnnotation
                            builder.addStringAnnotation(
                                tag = "URL",
                                annotation = link,
                                start = start,
                                end = end
                            )
                        }

                        TextAnnotationType.BEGIN_PIC -> {
                            // For images, AnnotatedString alone won’t render them.
                            // You need `appendInlineContent` + `inlineContent` in your Text composable.
                            // Here we just mark a placeholder.
                            builder.addStringAnnotation(
                                tag = "PIC",
                                annotation = link,
                                start = start,
                                end = end
                            )
                        }

                        else -> Unit // unreachable cases are ignored
                    }
                }
            }

            TextAnnotationType.BEGIN_CODE -> active.add(TextAnnotationType.BEGIN_CODE)
            TextAnnotationType.END_CODE -> active.remove(TextAnnotationType.BEGIN_CODE)

            TextAnnotationType.BEGIN_STRIKETHROUGH -> active.add(TextAnnotationType.BEGIN_STRIKETHROUGH)
            TextAnnotationType.END_STRIKETHROUGH -> active.remove(TextAnnotationType.BEGIN_STRIKETHROUGH)

            TextAnnotationType.BEGIN_ITALIC -> active.add(TextAnnotationType.BEGIN_ITALIC)
            TextAnnotationType.END_ITALIC -> active.remove(TextAnnotationType.BEGIN_ITALIC)

            TextAnnotationType.BEGIN_BOLD -> active.add(TextAnnotationType.BEGIN_BOLD)
            TextAnnotationType.END_BOLD -> active.remove(TextAnnotationType.BEGIN_BOLD)

            TextAnnotationType.BEGIN_LINK -> {
                active.add(TextAnnotationType.BEGIN_LINK)
                link = value
            }

            TextAnnotationType.END_LINK -> {
                active.remove(TextAnnotationType.BEGIN_LINK)
                active.remove(TextAnnotationType.BEGIN_PIC)
            }

            TextAnnotationType.BEGIN_PIC -> {
                active.add(TextAnnotationType.BEGIN_PIC)
                link = value
            }
        }
    }

    return builder.toAnnotatedString()
}

@Composable
fun RenderAnnotatedText(
    appContext: android.content.Context,
    annotations: List<Pair<TextAnnotationType, String>>,
    paragraphStyle: ParagraphStyle? = null,
    spanStyle: SpanStyle? = null,
) {
    val uriHandler = LocalUriHandler.current
    val annotated = buildAnnotated(annotations, paragraphStyle, spanStyle)
    ClickableText(
        text = annotated,
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        val intent =
                            Intent(Intent.ACTION_VIEW, annotation.item.toUri())
                        appContext.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("BrowserIntent", "Error starting activity", e)
                        Toast.makeText(
                            appContext,
                            "Failed to open browser",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            annotated.getStringAnnotations(tag = "PIC", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    // TODO: implement pictures (but rm onclick)
                    Toast.makeText(appContext, "Picture", Toast.LENGTH_SHORT)
                        .show()
//                                            uriHandler.openUri(annotation.item)
                }
        },
        style = MaterialTheme.typography.bodyMedium
    )
}
