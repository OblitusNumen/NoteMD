package oblitusnumen.notemd.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import oblitusnumen.notemd.impl.*

@Composable
fun MarkdownView(modifier: Modifier = Modifier, markdown: String) {
    val blocks: MutableList<Pair<Context, @Composable () -> Unit>> = mutableListOf()
    val split = markdown.split('\n')
    var context: Context = Context.NONE
    var cache = ""
    var prevListSymbol = ' '
    var previousLevel: Int = -1
    var leadingWhitespace = 0
    var previousWhitespaces = 0
    var previousSpace = 0
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
                        {
                            var text = cacheC
                                .replace("  \n", "\n\n")
                                .normalizeWhitespaces()
                                .replace(" \n", "\n")
                                .replace('\n', ' ')
                                .replace("  ", " \n")
                            val annotations = mutableListOf<Pair<TextAnnotationType, String>>()
                            val pointers = Pointers()
                            while (text.isNotEmpty()) {
                                val splitResult = splitBacktickSequence(text)
                                var newCurrent = splitResult.before
                                while (true) {
                                    val result = removeFirstMarkdownLink(newCurrent)
                                    newCurrent = result.around
                                    val index = result.index
                                    val link = result.linkSequence
                                    if (index == null) {
                                        parseText(annotations, newCurrent, pointers)
                                        break
                                    }
                                    val isPic = index != 0 && newCurrent[index - 1] == '!'
                                    var part: String
                                    if (isPic) {
                                        part = newCurrent.substring(0, index - 1)
                                    } else {
                                        part = newCurrent.substring(0, index)
                                    }

                                    // parse text before link
                                    parseText(annotations, part, pointers)

                                    val linkIndex = link!!.lastIndexOf("](")
                                    annotations.add(
                                        (
                                                if (isPic)
                                                    TextAnnotationType.BEGIN_PIC
                                                else
                                                    TextAnnotationType.BEGIN_LINK
                                                ) to link.substring(linkIndex + 2, link.length - 1)
                                    )
                                    val linkText = link.substring(1, linkIndex)

                                    // parse link text
                                    parseTextWithCode(annotations, linkText, pointers)

                                    annotations.add(TextAnnotationType.END_LINK to "")
                                    newCurrent = newCurrent.substring(index)
                                }
                                if (splitResult.inside == null)
                                    break
                                annotations.add(TextAnnotationType.BEGIN_CODE to "")
                                annotations.add(
                                    TextAnnotationType.TEXT to splitResult.inside.replace(" \n", " ").replace('\n', ' ')
                                )
                                annotations.add(TextAnnotationType.END_CODE to "")
                                text = splitResult.after
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

                            // rendering
                            val annotated = buildAnnotated(annotations)
                            val uriHandler = LocalUriHandler.current
                            Text(text = "text")
                            ClickableText(
                                text = annotated,
                                onClick = { offset ->
                                    annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            uriHandler.openUri(annotation.item)
                                        }
                                    annotated.getStringAnnotations(tag = "PIC", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            uriHandler.openUri(annotation.item)
                                        }
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Context.HEADING -> {
                        {
                            Text(text = "heading")
                            Text(text = cacheC, style = MaterialTheme.typography.titleLarge)
                        }
                    }

                    Context.QUOTE -> {
                        {
                            Text(text = "quote")
                            MarkdownView(markdown = cacheC)
                        }
                    }

                    Context.TEXT_BLOCK -> {
                        {
                            Text(text = "text block")
                            Text(text = cacheC, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Context.LIST_ELEMENT -> {
                        {
                            Text(text = "list element")
                            Row {
                                Text(
                                    text = "- $cacheC",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = (4 * previousLevelC).dp)
                                )
                            }
                        }
                    }

                    Context.SPACER -> {
                        {
                            Text(text = "spacer")
                            Spacer(Modifier.fillMaxWidth().height(16.dp))
                        }
                    }

                    Context.DIVIDER -> {
                        {
                            Text(text = "divider")
                            HorizontalDivider(Modifier.padding(4.dp))
                        }
                    }

                    Context.TABLE -> {
                        {
                            Text(text = "table")
                            cacheC.split('\n').forEach {
                                Row {
                                    for (string in it.trim().split('|')) {
                                        Text(string, style = MaterialTheme.typography.bodyMedium)
                                        VerticalDivider(modifier = Modifier.padding(4.dp))
                                    }
                                }
                                HorizontalDivider(Modifier.padding(4.dp))
                            }
                        }
                    }

                    Context.CODE_BLOCK -> {
                        {
                            Text(text = "code: $languageC")
                            Text(text = cacheC, style = MaterialTheme.typography.bodyMedium)
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
        current = current.trimLeadingWhitespaces()
        if (current.trim().isEmpty()) {
            pushBlock()
            return@repeat
        }

        // todo numbered, checklist
        // bullet list
        val symbol = current.firstOrNull()
        if (symbol == '+' || symbol == '-' || symbol == '*') {// -|
            var currentForList = current.substring(1)// "     [ ] text"
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

        // text block
        if (leadingWhitespace >= 4) {
            if (context == Context.TEXT_BLOCK) {
                cache += "\n" + current.addLeadingSpaces(leadingWhitespace - 4)
                return@repeat
            } else if (context != Context.TEXT/* && context != Context.QUOTE*/) {
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
        if (context != Context.TEXT && context != Context.LIST_ELEMENT// && context != Context.QUOTE
            && current.trimEnd().lastOrNull() == '|' && symbol == '|' && split.size >= index + 1
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
                if (pattern.matches(split[index + 1])) {
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

    // FIXME:  
    Column(modifier) {
        blocks.forEach {
            it.second()
        }
    }
//    LazyColumn(modifier) {
//        items(blocks) {
//            it.second()
//        }
//    }
}

class Pointers {
    var lastStrikethrough = -1
    var lastItalic_ = -1
    var lastItalicStar = -1
    var lastBold_ = -1
    var lastBoldStar = -1
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
            TextAnnotationType.TEXT to splitResult.inside.replace(" \n", " ").replace('\n', ' ')
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
fun buildAnnotated(annotations: List<Pair<TextAnnotationType, String>>): AnnotatedString {
    val builder = AnnotatedString.Builder()
    builder.pushStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface))
    val active = mutableSetOf<TextAnnotationType>() // currently active styles

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
                                annotation = value,
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
                                annotation = value,
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

            TextAnnotationType.BEGIN_LINK -> active.add(TextAnnotationType.BEGIN_LINK)
            TextAnnotationType.END_LINK -> {
                active.remove(TextAnnotationType.BEGIN_LINK)
                active.remove(TextAnnotationType.BEGIN_PIC)
            }

            TextAnnotationType.BEGIN_PIC -> active.add(TextAnnotationType.BEGIN_PIC)
            // no explicit END_PIC? same handling as END_LINK if you want
        }
    }

    return builder.toAnnotatedString()
}
