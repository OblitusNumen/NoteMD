package oblitusnumen.notemd.impl

import java.io.File

class MdFile {
    private val dataManager: DataManager
    private val contentFile: File
    val name: String get() = contentFile.name
    var viewType: ViewType = ViewType.CHAT // FIXME:
        set(value) {
            field = value
            save()
        }
        get() {
            if (!loaded)
                load()
            return field
        }
    var content: String = ""
        set(value) {
            field = value
            save()
        }
        get() {
            if (!loaded)
                load()
            return field
        }
    private var loaded = false

    constructor(dataManager: DataManager, contentFile: File) {
        this.dataManager = dataManager
        this.contentFile = contentFile
    }

    constructor(dataManager: DataManager, name: String) {
        this.dataManager = dataManager
        this.contentFile = File(dataManager.mdDir, "${name}.md")
    }

    private fun load() {
        synchronized(this) {
            if (loaded) return
            loaded = true
            val lines = contentFile.readLines()
            require(lines.isNotEmpty()) { "File is empty" }
            viewType = viewTypeFromString(lines.first()) ?: ViewType.CHAT
            content = if (lines.size > 1) {
                lines.drop(1).joinToString("\n")
            } else {
                ""
            }
        }
    }

    fun save() {
        synchronized(this) {
            val text = buildString {
                appendLine(viewType.toStringValue())
                if (content.isNotEmpty()) {
                    append(content)
                }
            }
            contentFile.writeText(text)
        }
    }

    fun create(name: String): Boolean {
        synchronized(this) {
            if (!nameValid(dataManager, name) || !contentFile.createNewFile())
                return false
            contentFile.writeText(ViewType.CHAT.toStringValue())
            return true
        }
    }

    fun delete() {
        synchronized(this) {
            contentFile.delete()
            dataManager.cleanupLinkedFiles()
        }
    }

    companion object {
        fun nameValid(dataManager: DataManager, name: String): Boolean {
            return name.isNotEmpty() && !File(dataManager.mdDir, "$name.md").exists()
        }
    }
}