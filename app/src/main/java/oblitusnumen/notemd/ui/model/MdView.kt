package oblitusnumen.notemd.ui.model

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import oblitusnumen.notemd.impl.DataManager
import oblitusnumen.notemd.impl.MdFile
import oblitusnumen.notemd.impl.ViewType
import oblitusnumen.notemd.ui.conditional
import oblitusnumen.notemd.ui.parseMarkdown

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
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()
            var headingLinks: Map<String, Int> = remember { mapOf() }
            val itemPositions = remember { mutableStateMapOf<Int, Int>() }
            val positionsWrapped = remember {
                {
                    itemPositions
                }
            }
            val parseResult =
                remember(content) {
                    parseMarkdown(appContext = dataManager.context, markdown = content.text, headingLinksHandler = { link ->
                        coroutineScope.launch {
                            headingLinks[link].let {
                                positionsWrapped()[it]?.let { y ->
                                    scrollState.animateScrollTo(y)
                                }
                            }
                        }
                    })
                }
            headingLinks = remember(parseResult) { parseResult.headings }
            Box(
                (if (md) Modifier.weight(1f) else Modifier)
            ) {
                if (md) {
                    val items = parseResult.blocks

                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp)
                                .verticalScroll(scrollState)
                        ) {
                            items.forEachIndexed { index, block ->
                                Column(
                                    modifier = Modifier
                                        .onGloballyPositioned { layoutCoordinates ->
                                            // Save the Y position of this item relative to the column
                                            itemPositions[index] = layoutCoordinates.positionInParent().y.toInt()
                                        }
                                ) {
                                    block()
                                }
                            }
                        }
                    }
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

    fun onBackPress(then: () -> Unit) {
        if (mdFile.viewType == ViewType.MD_WITH_SOURCE || mdFile.viewType == ViewType.SOURCE) {
            mdFile.viewType = ViewType.MD
            hack = ! hack
            return
        }
        mdFile.content = content.text
        then()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBar(backPress: () -> Unit) {
        val onBackPress = {
            mdFile.content = content.text
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

                val saveLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/markdown")
                ) { uri: Uri? ->
                    uri?.let {
                        try {
                            dataManager.context.contentResolver.openOutputStream(it)?.use { output ->
                                output.write(content.text.toByteArray())
                            }
                        } catch (e: Exception) {
                            dataManager.toast("Saving failed: ${e.message}")
                        }
                    }
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
                    DropdownMenuItem(
                        text = { Text("Export File") },
                        onClick = {
                            saveLauncher.launch(mdFile.name)
                            settingsDialogShown = false
                            hack = !hack
                        }
                    )
                }
            }
        )
    }
}
