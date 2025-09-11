package oblitusnumen.notemd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import oblitusnumen.notemd.impl.DataManager
import oblitusnumen.notemd.impl.MdFile
import oblitusnumen.notemd.ui.model.MainScreen
import oblitusnumen.notemd.ui.model.MdView
import oblitusnumen.notemd.ui.theme.NoteMDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoteMDTheme {
                val dataManager = remember { DataManager(this) }
                val mainScreen = remember { MainScreen(dataManager) }
                var mdView: MdView? by remember { mutableStateOf(null) }
                val openMd: (mdFile: MdFile) -> Unit = {
                    mdView = MdView(dataManager, it)
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (mdView == null)
                            mainScreen.TopBar()
                        else
                            mdView!!.TopBar { mdView = null }
                    },
                    floatingActionButton = {
                        if (mdView == null)
                            mainScreen.FunctionButton(openMd)
                    }
                ) { innerPadding ->
                    if (mdView == null)
                        mainScreen.Compose(Modifier.padding(innerPadding), openMd)
                    else {
                        BackHandler {
                            mdView = null
                        }
                        mdView!!.Compose(Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}
