package oblitusnumen.notemd.ui.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import oblitusnumen.notemd.impl.DataManager
import oblitusnumen.notemd.impl.MdFile
import oblitusnumen.notemd.impl.ViewType
import oblitusnumen.notemd.impl.conditional
import oblitusnumen.notemd.ui.MarkdownView

class MdView(private val dataManager: DataManager, var mdFile: MdFile) {
    var content by mutableStateOf(TextFieldValue(mdFile.content))
    var hack by mutableStateOf(false)

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Compose(modifier: Modifier = Modifier) {
        hack
        val viewType = mdFile.viewType
        Column(
            modifier = modifier.fillMaxSize().conditional(viewType == ViewType.SOURCE) { imePadding() }
        ) {
            remember { content }
            val previewScroll = rememberScrollState()
            val md = viewType == ViewType.MD_WITH_SOURCE || viewType == ViewType.MD
            Box(
                (if (md) Modifier.weight(1f) else Modifier)
                    .verticalScroll(previewScroll)
            ) {
                if (md) {
                    MarkdownView(markdown = content.text, appContext = dataManager.context)
                }
            }

            HorizontalDivider()

            if (viewType == ViewType.MD_WITH_SOURCE || viewType == ViewType.SOURCE) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    TextField(
                        value = content,
                        onValueChange = { newValue ->
                            // TODO: undo history
                            content = newValue
                            mdFile.content = content.text
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp),
                        modifier = Modifier
                            .fillMaxSize(),
                        singleLine = false,
                        maxLines = Int.MAX_VALUE,
                    )
                }
            }

            if (viewType == ViewType.CHAT) {
                // TODO: ViewType.CHAT
            }
        }
    }

    fun onBackPress() {
        mdFile.content = content.text
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBar(backPress: () -> Unit) {
        val onBackPress = {
            onBackPress()
            backPress()
        }
        var settingsDialogShown by remember { mutableStateOf(false) }
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .9f),
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            title = { Text(mdFile.name, maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = onBackPress) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Localized description"
                    )
                }
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
                        text = { Text("MD View") },
                        onClick = {
                            mdFile.viewType = ViewType.MD
                            settingsDialogShown = false
                            hack = !hack
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Source View") },
                        onClick = {
                            mdFile.viewType = ViewType.SOURCE
                            settingsDialogShown = false
                            hack = !hack
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("MD with Source View") },
                        onClick = {
                            mdFile.viewType = ViewType.MD_WITH_SOURCE
                            settingsDialogShown = false
                            hack = !hack
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Chat View") },
                        onClick = {
                            mdFile.viewType = ViewType.CHAT
                            settingsDialogShown = false
                            hack = !hack
                        }
                    )
                }
            }
        )
//        if (settingsDialogShown)
//            ShowSettingsDialog { settingsDialogShown = false }
    }

//    @Composable
//    private fun ShowSettingsDialog(onClose: () -> Unit) {
//
//    }
}
