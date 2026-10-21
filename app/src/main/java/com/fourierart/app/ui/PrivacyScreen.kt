package com.fourierart.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.fourierart.app.R

/** Full policy is available offline; its public HTML comes from the same source. */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val titles = stringArrayResource(R.array.privacy_section_titles)
    val bodies = stringArrayResource(R.array.privacy_section_bodies)
    WorkflowPage(stringResource(R.string.privacy_policy_title), onBack, tag = "screen_privacy") {
        Text("FOURIER ART · " + stringResource(R.string.privacy_policy_date), color = Muted)
        titles.indices.forEach { index ->
            SectionCard(titles[index]) {
                Text(bodies[index], color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
