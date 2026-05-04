package com.matrix.synapse.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.matrix.synapse.core.ui.theme.SynapseShapes
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme

/**
 * Confirmation dialog for destructive actions (deactivate user, purge media,
 * reset federation, sign out). Title in Danger, primary button uses the
 * Danger variant of [SynapseButton], dismiss is Ghost.
 */
@Composable
fun DestructiveDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel",
) {
    val c = SynapseTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SynapseShapes.large,
        containerColor = c.surface,
        titleContentColor = c.danger,
        textContentColor = c.text,
        title = { Text(title, style = SynapseText.Title, color = c.danger) },
        text = { Text(body, style = SynapseText.BodyM, color = c.text) },
        confirmButton = {
            SynapseButton(
                text = confirmLabel,
                onClick = onConfirm,
                variant = SynapseButtonVariant.Danger,
            )
        },
        dismissButton = {
            SynapseButton(
                text = dismissLabel,
                onClick = onDismiss,
                variant = SynapseButtonVariant.Ghost,
            )
        },
    )
}
