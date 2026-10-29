package io.justpilot.justeinbuergerung.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.ui.common.FederalStateItem
import io.justpilot.justeinbuergerung.ui.common.federalStatesByName

/** First start: the federal state decides which ten state questions are in the pool. */
@Composable
fun OnboardingScreen(onSelect: (FederalState) -> Unit) {
    Scaffold { padding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.padding(padding).selectableGroup(),
        ) {
            item {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.onboarding_text), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.onboarding_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(federalStatesByName) { state ->
                FederalStateItem(state, selected = false, onClick = { onSelect(state) })
            }
        }
    }
}
