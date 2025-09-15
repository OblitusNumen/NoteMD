package oblitusnumen.notemd.ui.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import oblitusnumen.notemd.ui.MarkdownView

class MdView(private val dataManager: DataManager, var mdFile: MdFile) {
    var content by mutableStateOf(TextFieldValue(mdFile.content))

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Compose(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            remember { content }
            val previewScroll = rememberScrollState()
            Box(
                Modifier
                    .weight(1f)
                    .verticalScroll(previewScroll)
            ) {
                MarkdownView(markdown = content.text, appContext = dataManager.context)
            }

            HorizontalDivider()

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
            }
        )
//        if (settingsDialogShown)
//            showSettingsDialog { settingsDialogShown = false }
    }
}
