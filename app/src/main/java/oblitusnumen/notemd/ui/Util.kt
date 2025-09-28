package oblitusnumen.notemd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import oblitusnumen.notemd.impl.DataManager
import oblitusnumen.notemd.impl.MdFile

@Composable
fun Table(
    headers: List<@Composable (() -> Unit)>,
    rows: MutableList<List<@Composable (() -> Unit)>>
) {
    Column(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.surfaceBright) // outer border
    ) {
        // Header row
        Row(
            modifier = Modifier.height(IntrinsicSize.Min)
                .background(MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5F)) // header background
                .fillMaxWidth()
        ) {
            headers.forEach { header ->
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .border(0.5.dp, MaterialTheme.colorScheme.surfaceBright)
                        .padding(8.dp)
                ) {
                    header()
                }
            }
        }

        // Data rows
        rows.forEach { row ->
            Row(modifier = Modifier.height(IntrinsicSize.Min).fillMaxWidth()) {
                row.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .border(0.5.dp, MaterialTheme.colorScheme.surfaceBright)
                            .padding(8.dp)
                    ) {
                        cell()
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.conditional(condition: Boolean, modifier: @Composable Modifier.() -> Modifier): Modifier {
    return if (condition) {
        this.modifier()
    } else {
        this
    }
}

@Composable
fun AddDialog(dataManager: DataManager, openMd: (MdFile) -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onClose() },
        dismissButton = {
            TextButton(onClick = { onClose() }) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val mdfile = MdFile(dataManager, name)
                if (mdfile.create(name)) {
                    onClose()
                    openMd(mdfile)
                } else {
                    // TODO:
                    dataManager.toast("Unable to create md with name $name")
                }
            }) {
                Text("OK")
            }
        },
        text = {
            Row {
                val focusRequester = remember { FocusRequester() }
                var laidOut by remember { mutableStateOf(false) }
                OutlinedTextField(// FIXME: ui paddings
                    isError = name.isEmpty(),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .weight(1f)
                        .onGloballyPositioned { laidOut = true }
                        .focusRequester(focusRequester),
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    label = { Text("Filename") }
                )
                LaunchedEffect(laidOut) {
                    focusRequester.requestFocus()
                }
            }
        }
    )
}
