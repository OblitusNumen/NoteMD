package oblitusnumen.notemd.impl

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

class DataManager(val context: Context) {
    val mdDir: File
//    var config: Config = Config.ofString(getSharedPrefs(context).getString(CONFIG_PREF_NAME, "")!!)
//        set(config) {
//            getSharedPrefs(context).edit().putString(CONFIG_PREF_NAME, config.toString()).apply()
//            field = config
//        }

    init {
        val mdDirectory = File(context.filesDir, PROJECTS_DIRECTORY)
        if (!mdDirectory.exists() && !mdDirectory.mkdirs()) throw IOException("Could not create directory")
        mdDir = mdDirectory
    }

    fun getMdProjects(): List<MdFile> {
        val result: MutableList<MdFile> = ArrayList()
        for (mdFile in mdDir.listFiles()?.filter { it.name.endsWith(".md") }
            ?.sortedBy { Files.readAttributes(it.toPath(), BasicFileAttributes::class.java).creationTime() }
            ?: throw IOException("Could not list files from directory")) {
            result.add(MdFile(this, mdFile))
        }
        return result
    }

    fun cleanupLinkedFiles() {
        // TODO: delete associated files
    }

    fun toast(string: String) {
        Toast.makeText(context, string, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val PROJECTS_DIRECTORY: String = "md-projects"
        private const val SHARED_PREFERENCES_NAME: String = "md_preferences"
        private const val CONFIG_PREF_NAME: String = "config"

        fun getSharedPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        }
    }
}
