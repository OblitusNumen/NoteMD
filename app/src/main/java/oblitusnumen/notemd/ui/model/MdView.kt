package oblitusnumen.notemd.ui.model

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yazantarifi.compose.library.MarkdownConfig
import com.yazantarifi.compose.library.MarkdownViewComposable
import oblitusnumen.notemd.impl.DataManager
import oblitusnumen.notemd.impl.MdFile

class MdView(private val dataManager: DataManager, private var mdFile: MdFile) {
    @Composable
    fun Compose(modifier: Modifier = Modifier) {
        var content by remember {
            mutableStateOf(
                """
                # Demo
    
                Emphasis, aka italics, with *asterisks* or _underscores_. Strong emphasis, aka bold, with **asterisks** or __underscores__. Combined emphasis with **asterisks and _underscores_**. [Links with two blocks, text in square-brackets, destination is in parentheses.](https://www.example.com). Inline `code` has `back-ticks around` it.
                
                1. First ordered list item
                2. Another item
                    * Unordered sub-list.
                3. And another item.
                    You can have properly indented paragraphs within list items. Notice the blank line above, and the leading spaces (at least one, but we'll use three here to also align the raw Markdown).
                
                * Unordered list can use asterisks
                - Or minuses
                + Or pluses
                ---
                
                ```javascript
                var s = "code blocks use monospace font";
                alert(s);
                ```
                
                Markdown | Table | Extension
                --- | --- | ---
                *renders* | `beautiful images` | ![random image](https://picsum.photos/seed/picsum/400/400 "Text 1")
                1 | 2 | 3
                
                > Blockquotes are very handy in email to emulate reply text.
                > This line is part of the same quote.
            """.trimIndent()
            )
        }

        val editorScroll = rememberScrollState()
        val previewScroll = rememberScrollState()

        Column(modifier = modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .verticalScroll(previewScroll)
            ) {
                MarkdownViewComposable(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    content = content,
                    config = MarkdownConfig(
                        isLinksClickable = true,
                        isImagesClickable = true,
                        isScrollEnabled = false,
                        colors = HashMap<String, Color>().apply {
                            this[MarkdownConfig.CHECKBOX_COLOR] = Color.Black
                            this[MarkdownConfig.LINKS_COLOR] = Color.Blue
                            this[MarkdownConfig.TEXT_COLOR] = Color.Gray
                            this[MarkdownConfig.HASH_TEXT_COLOR] = Color.Black
                            this[MarkdownConfig.CODE_BACKGROUND_COLOR] = Color.Gray
                            this[MarkdownConfig.CODE_BLOCK_TEXT_COLOR] = Color.White
                        }
                    )
                ) { link, type ->
                    when (type) {
                        MarkdownConfig.IMAGE_TYPE -> {} // Image Clicked
                        MarkdownConfig.LINK_TYPE -> {} // Link Clicked
                    }
                }
            }

            HorizontalDivider()

            Box(
                Modifier
                    .weight(1f)
                    .verticalScroll(editorScroll)
            ) {
                TextField(
                    value = content,
                    onValueChange = {
                        // TODO: undo history
                        content = it
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBar(backPress: () -> Unit) {
        var settingsDialogShown by remember { mutableStateOf(false) }
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .9f),
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            title = { Text(mdFile.name, maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = backPress) {
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
