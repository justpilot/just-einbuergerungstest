package io.justpilot.justeinbuergerung.ui.common

import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import io.justpilot.justeinbuergerung.domain.model.FederalState

/** The 16 federal states, alphabetically by German name, as a single-choice list. */
val federalStatesByName: List<FederalState> = FederalState.entries.sortedBy { it.germanName }

@Composable
fun FederalStateItem(state: FederalState, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(state.germanName) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}
