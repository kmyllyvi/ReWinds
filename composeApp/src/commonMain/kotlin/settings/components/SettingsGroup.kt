package settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.theme.rewinds

/**
 * A labelled settings group: an uppercase tertiary section label above a [surface]
 * card whose rows are separated by 1 dp [border]-coloured dividers.
 *
 * Pure presentation — rows are supplied by the caller and carry their own behaviour.
 */
@Composable
fun SettingsGroup(
    label: String,
    modifier: Modifier = Modifier,
    rows: List<@Composable () -> Unit>
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.rewinds.textTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.rewinds.surface)
        ) {
            rows.forEachIndexed { index, row ->
                row()
                if (index < rows.lastIndex) RowDivider()
            }
        }
    }
}

/** 1 dp divider in the [border] tone, used between rows inside a [SettingsGroup]. */
@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.rewinds.border)
    )
}

/** Shared row scaffold: a [surface] row with a primary label and trailing content. */
@Composable
private fun SettingsRowScaffold(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(horizontal = 14.dp, vertical = 12.dp)
    Row(
        modifier = base,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

/**
 * A row with a [label] on the left and a [value] plus chevron on the right.
 * Tapping the row invokes [onClick] when provided.
 */
@Composable
fun SettingsValueRow(
    label: String,
    value: String? = null,
    showChevron: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    SettingsRowScaffold(onClick = onClick, modifier = modifier) {
        Text(
            text = label,
            color = MaterialTheme.rewinds.textPrimary,
            fontSize = 14.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value,
                    color = MaterialTheme.rewinds.textSecondary,
                    fontSize = 13.sp
                )
            }
            if (showChevron) {
                Text(
                    text = "  ›",
                    color = MaterialTheme.rewinds.textTertiary,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * A row with a [label] on the left and a [Switch] on the right.
 * The switch reflects [checked]; toggling calls [onCheckedChange]. State is owned by
 * the caller's ViewModel, never the composable.
 */
@Composable
fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRowScaffold(onClick = { onCheckedChange(!checked) }) {
        Text(
            text = label,
            color = MaterialTheme.rewinds.textPrimary,
            fontSize = 14.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.rewinds.pageBg,
                checkedTrackColor = MaterialTheme.rewinds.accentBlue,
                uncheckedThumbColor = MaterialTheme.rewinds.textTertiary,
                uncheckedTrackColor = MaterialTheme.rewinds.surface,
                uncheckedBorderColor = MaterialTheme.rewinds.border
            )
        )
    }
}

/**
 * An API-key row: a [title] with a smaller [subLabel] beneath, and a status chip on
 * the right driven by [configured]. Tapping the row invokes [onClick].
 */
@Composable
fun SettingsKeyRow(
    title: String,
    subLabel: String,
    configured: Boolean,
    configuredChipText: String,
    notSetChipText: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    SettingsRowScaffold(onClick = onClick, modifier = modifier) {
        Column {
            Text(
                text = title,
                color = MaterialTheme.rewinds.textPrimary,
                fontSize = 14.sp
            )
            Text(
                text = subLabel,
                color = MaterialTheme.rewinds.textTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        KeyStatusChip(
            configured = configured,
            configuredText = configuredChipText,
            notSetText = notSetChipText
        )
    }
}

/**
 * Status chip for an API key. "Configured" uses accentBlue; "Not set" uses the
 * attention (amber) token to flag an actionable gap. Colours come from theme tokens.
 */
@Composable
private fun KeyStatusChip(
    configured: Boolean,
    configuredText: String,
    notSetText: String
) {
    val color = if (configured) MaterialTheme.rewinds.accentBlue else MaterialTheme.rewinds.attention
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (configured) configuredText else notSetText,
            color = color,
            fontSize = 11.sp
        )
    }
}

/**
 * A destructive row, e.g. "Delete all data": label and chevron rendered in [error].
 */
@Composable
fun SettingsDestructiveRow(
    label: String,
    onClick: () -> Unit
) {
    SettingsRowScaffold(onClick = onClick) {
        Text(
            text = label,
            color = MaterialTheme.rewinds.error,
            fontSize = 14.sp
        )
        Text(
            text = "›",
            color = MaterialTheme.rewinds.error,
            fontSize = 16.sp
        )
    }
}
