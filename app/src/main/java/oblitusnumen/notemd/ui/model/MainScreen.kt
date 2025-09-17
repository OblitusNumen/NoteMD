package oblitusnumen.notemd.ui.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oblitusnumen.notemd.ui.AddDialog
import oblitusnumen.notemd.impl.DataManager
import oblitusnumen.notemd.impl.MdFile

class MainScreen(private val dataManager: DataManager) {
    private var mdFiles: List<MdFile> by mutableStateOf(dataManager.getMdProjects())

    @Composable
    fun Compose(modifier: Modifier = Modifier, openMd: (MdFile) -> Unit) {
        LazyColumn(modifier = modifier) {
            items(mdFiles) {
                DrawMdProject(it) { openMd(it) }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DrawMdProject(mdFile: MdFile, openMd: () -> Unit) {
        var deleteDialogShown by remember { mutableStateOf(false) }
        Row(
            Modifier.defaultMinSize(minHeight = 100.dp).fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)
                .combinedClickable(onLongClick = { deleteDialogShown = true }, onClick = { openMd() })
//                .border(2.dp, Color.Gray, shape = RoundedCornerShape(4.dp))
        ) {
            Text(
                modifier = Modifier.weight(1.0f).padding(start = 8.dp, end = 8.dp).align(Alignment.CenterVertically),
                text = mdFile.name,
                style = MaterialTheme.typography.headlineSmall,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
        if (deleteDialogShown)
            DeleteDialog(mdFile) { deleteDialogShown = false }
    }

    @Composable
    fun DeleteDialog(mdFile: MdFile, onClose: () -> Unit) {
        AlertDialog(
            onDismissRequest = onClose,
            dismissButton = {
                TextButton(onClick = onClose) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onClose()
                    mdFile.delete()
                    mdFiles -= mdFile
                }) {
                    Text("OK")
                }
            },
            text = {
                Column {
                    Text("Delete ${mdFile.name}?")
                }
            }
        )
    }

    @Composable
    fun FunctionButton(openMd: (MdFile) -> Unit) {
        var addDialogShown by remember { mutableStateOf(false) }
        if (addDialogShown)
            AddDialog(dataManager, openMd) { addDialogShown = false }
        FloatingActionButton(onClick = { addDialogShown = true }) {
            Icon(Icons.Filled.Add, "Add md")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBar() {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .9f),
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            title = { Text("NoteMD", maxLines = 1) },
            scrollBehavior = scrollBehavior,
        )
    }

    fun update() {
        mdFiles = dataManager.getMdProjects()
    }
}
