package oblitusnumen.notemd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import oblitusnumen.notemd.impl.DataManager
import oblitusnumen.notemd.impl.MdFile
import oblitusnumen.notemd.ui.model.MainScreen
import oblitusnumen.notemd.ui.model.MdView
import oblitusnumen.notemd.ui.parseMarkdown
import oblitusnumen.notemd.ui.theme.NoteMDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            NoteMDTheme {
                val dataManager = remember { DataManager(this) }
//                val mdFile: MdFile? = remember {
//                    intent?.data?.let { uri ->
//                        MdFile(dataManager, File(uri.path!!))
//                    }
//                }
                val mainScreen = remember { MainScreen(dataManager) }
                var mdView: MdView? by remember { mutableStateOf(null) }
                val openMd: (mdFile: MdFile) -> Unit = {
                    mdView = MdView(dataManager, it)
                }

                // opening external MD
                var mdContent by remember {
                    mutableStateOf(intent?.data?.let { uri ->
                        contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                    })
                }
//                intent?.data?.let { uri ->
//                    contentResolver.takePersistableUriPermission(
//                        uri,
//                        Intent.FLAG_GRANT_READ_URI_PERMISSION
//                    )
//                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (mdContent != null) {
                            // FIXME:
                            mainScreen.TopBar()
                        } else {
                            if (mdView == null)
                                mainScreen.TopBar()
                            else
                                mdView!!.TopBar { mdView = null }
                        }
                    },
                    floatingActionButton = {
                        if (mdContent == null) {
                            if (mdView == null)
                                mainScreen.FunctionButton(openMd)
                        }
                    }
                ) { innerPadding ->
                    if (mdContent != null) {
                        Row {
                            LazyColumn(
                                Modifier.padding(innerPadding).padding(horizontal = 10.dp)
                            ) {
                                items(
                                    parseMarkdown(
                                        this@MainActivity,
                                        mdContent!!
                                    )
                                ) {
                                    it()
                                }
                            }
                        }
//                        val activity = (LocalContext.current as? Activity)
                        BackHandler {
                            mdContent = null
//                            activity?.finishAffinity()
                        }
                    } else {
                        if (mdView == null)
                            mainScreen.Compose(Modifier.padding(innerPadding), openMd)
                        else {
                            BackHandler {
                                mdView!!.onBackPress()
                                mdView = null
                            }
                            mdView!!.Compose(Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }
}
