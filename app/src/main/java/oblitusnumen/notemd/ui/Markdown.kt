package oblitusnumen.notemd.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import oblitusnumen.notemd.impl.*

@Composable
fun MarkdownView(modifier: Modifier = Modifier, markdown: String) {
    val blocks: MutableList<Pair<Context, @Composable () -> Unit>> = mutableListOf()
    val split = markdown.split('\n')
    var context: Context = Context.NONE
    var cache: String = ""
    var prevListSymbol: Char = ' '
    var previousLevel: Int = -1
    var leadingWhitespace: Int = 0
    var previousWhitespaces: Int = 0
    var previousSpace: Int = 0
    var checkbox: Int = -1
    var language: String = ""
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
                            Text(text = "text")
                            Text(text = cacheC.replace('\n', ' '), style = MaterialTheme.typography.bodyMedium)
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
            }
            if (current.trim().countTrailing('`') == previousLevel) {
                current = current.trimEnd().dropLast(previousLevel)
                cache += (if (cache.isEmpty()) "" else "\n") + current
                pushBlock()
                return@repeat
            }
            cache += "\n" + current
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
            }
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
                cache += "\n" + current.addLeadingSpaces(leadingWhitespace - 4)
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
            && current.trimEnd().lastOrNull() == '|' && symbol == '|' && split.size >= index + 1) {
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
        if (current.firstOrNull() == '>') {
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