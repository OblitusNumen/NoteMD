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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import oblitusnumen.notemd.ui.AddDialog
import oblitusnumen.notemd.impl.DataManager
import oblitusnumen.notemd.impl.MdFile
import oblitusnumen.notemd.impl.ViewType
import oblitusnumen.notemd.ui.model.MainScreen
import oblitusnumen.notemd.ui.model.MdView
import oblitusnumen.notemd.ui.parseMarkdown
import oblitusnumen.notemd.ui.theme.NoteMDTheme
import java.io.File

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            NoteMDTheme {
                val dataManager = remember { DataManager(this) }
                val mdFile: MdFile? = remember {
                    intent?.data?.let { uri ->
                        MdFile(dataManager, File(uri.path!!))
                    }
                }
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
                            var settingsDialogShown by remember { mutableStateOf(false) }
                            var addDialogShown by remember { mutableStateOf(false) }
                            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                            CenterAlignedTopAppBar(
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .9f),
                                    titleContentColor = MaterialTheme.colorScheme.primary,
                                ),
                                title = { Text(mdFile!!.name, maxLines = 1) },
                                navigationIcon = {
                                },
                                scrollBehavior = scrollBehavior,
                                actions = {
                                    IconButton(onClick = { settingsDialogShown = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = null
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = settingsDialogShown,
                                        onDismissRequest = { settingsDialogShown = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Save in editor") },
                                            onClick = {
                                                addDialogShown = true
                                                settingsDialogShown = false
                                            }
                                        )
                                    }
                                }
                            )
                            if (addDialogShown)
                                AddDialog(dataManager, {
                                    it.viewType = ViewType.MD_WITH_SOURCE
                                    it.content = mdContent!!
                                    mdContent = null
                                    openMd(it)
                                }) {
                                    addDialogShown = false
                                }
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
                                    if (mdContent != null) {
                                        parseMarkdown(
                                            this@MainActivity,
                                            mdContent!!
                                        )
                                    } else listOf()
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
                        if (mdView == null) {
                            remember {
                                mainScreen.update()
                                null
                            }
                            mainScreen.Compose(Modifier.padding(innerPadding), openMd)
                        } else {
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
