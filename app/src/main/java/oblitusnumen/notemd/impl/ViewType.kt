package oblitusnumen.notemd.impl

enum class ViewType {
    CHAT,
    MD,
    SOURCE,
    MD_WITH_SOURCE;

    // Convert enum to string
    fun toStringValue(): String = when (this) {
        ViewType.CHAT -> "CHAT"
        ViewType.MD -> "MD"
        ViewType.SOURCE -> "SOURCE"
        ViewType.MD_WITH_SOURCE -> "MD_WITH_SOURCE"
    }
}

// Convert string to enum
fun viewTypeFromString(value: String): ViewType? = when (value.uppercase()) {
    "CHAT" -> ViewType.CHAT
    "MD" -> ViewType.MD
    "SOURCE" -> ViewType.SOURCE
    "MD_WITH_SOURCE" -> ViewType.MD_WITH_SOURCE
    else -> null // return null if string doesn't match
}
