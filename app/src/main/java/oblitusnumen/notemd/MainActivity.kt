package oblitusnumen.notemd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import oblitusnumen.notemd.ui.theme.NoteMDTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoteMDTheme {
                var openFile: File? by remember { mutableStateOf(null) }
                Scaffold(modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (wordScreen == null)
                            mainScreen.topBar()
                        else
                            wordScreen!!.topBar { wordScreen = null }
                    },
                    floatingActionButton = {
                        if (wordScreen == null)
                            mainScreen.functionButton()
                    }) { innerPadding ->
                        val dataManager = remember { DataManager(this) }
                        val mainScreen = remember { MainScreen(dataManager) }
                        var wordScreen: WordScreen? by remember { mutableStateOf(null) }
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            if (wordScreen == null)
                                mainScreen.compose(Modifier.padding(innerPadding)) { wordScreen = WordScreen(dataManager, it) }
                            else {
                                BackHandler {
                                    wordScreen = null
                                }
                                wordScreen!!.compose(Modifier.padding(innerPadding))
                            }
                        }
                }
            }
        }
    }
}