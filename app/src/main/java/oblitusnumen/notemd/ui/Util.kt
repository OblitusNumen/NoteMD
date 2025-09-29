package oblitusnumen.notemd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import oblitusnumen.notemd.impl.DataManager
import oblitusnumen.notemd.impl.MdFile

@Composable
fun Table(
    headers: List<@Composable () -> Unit>,
    rows: List<List<@Composable () -> Unit>>,
    minColumnWidth: Dp = 80.dp,
    maxColumnWidth: Dp = 200.dp,
    headerCellModifier: Modifier = Modifier
        .border(0.5.dp, MaterialTheme.colorScheme.surfaceBright)
        .background(MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5f))
        .padding(8.dp),
    cellModifier: Modifier = Modifier
        .border(0.5.dp, MaterialTheme.colorScheme.surfaceBright)
        .padding(8.dp),
    modifier: Modifier = Modifier
) {
    LocalDensity.current

    Layout(
        content = {
            headers.forEach { Box(headerCellModifier) { it() } }
            rows.flatten().forEach { Box(cellModifier) { it() } }
        },
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.surfaceBright)
            .horizontalScroll(rememberScrollState())
    ) { measurables, constraints ->

        val columnCount = headers.size
        val rowCount = rows.size + 1

        val minPx = minColumnWidth.roundToPx()
        val maxPx = maxColumnWidth.roundToPx()

        // --- decide column widths using intrinsics ---
        val columnWidths = IntArray(columnCount) { col ->
            val colMeasurables = (0 until rowCount).map { row ->
                measurables[row * columnCount + col]
            }
            val intrinsic = colMeasurables.maxOf {
                it.minIntrinsicWidth(constraints.maxHeight)
            }
            intrinsic.coerceIn(minPx, maxPx)
        }

        // --- decide row heights using intrinsics ---
        val rowHeights = IntArray(rowCount) { row ->
            val rowMeasurables = (0 until columnCount).map { col ->
                measurables[row * columnCount + col] to columnWidths[col]
            }
            rowMeasurables.maxOf { (measurable, colWidth) ->
                measurable.maxIntrinsicHeight(colWidth)
            }
        }

        // --- measure each child once with fixed width/height ---
        val placeables = mutableListOf<Placeable>()
        var index = 0
        for (row in 0 until rowCount) {
            for (col in 0 until columnCount) {
                val measurable = measurables[index++]
                val placeable = measurable.measure(
                    Constraints.fixed(
                        columnWidths[col],
                        rowHeights[row]
                    )
                )
                placeables += placeable
            }
        }

        val tableWidth = columnWidths.sum()
        val tableHeight = rowHeights.sum()

        layout(tableWidth, tableHeight) {
            var i = 0
            var y = 0
            for (row in 0 until rowCount) {
                var x = 0
                for (col in 0 until columnCount) {
                    placeables[i++].placeRelative(x, y)
                    x += columnWidths[col]
                }
                y += rowHeights[row]
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
