package oblitusnumen.notemd.impl

fun String.countLeadingWhitespaces(): Int = this.takeWhile { it.isWhitespace() }.length

fun String.countLeading(char: Char): Int = this.takeWhile { it == char }.length

fun String.trimLeadingWhitespaces(): String = this.dropWhile { it.isWhitespace() }

fun String.trimLeading(char: Char): String = this.dropWhile { it == char }

fun String.normalizeWhitespaces(): String = this.replace(Regex("\\s+"), " ")

fun String.addLeadingSpaces(count: Int): String = " ".repeat(count) + this

fun String.countTrailing(char: Char): Int = this.reversed().takeWhile { it == char }.length