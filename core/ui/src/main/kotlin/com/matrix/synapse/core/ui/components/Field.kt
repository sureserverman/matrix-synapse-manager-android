package com.matrix.synapse.core.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.matrix.synapse.core.ui.theme.SynapseShapes
import com.matrix.synapse.core.ui.theme.SynapseText
import com.matrix.synapse.core.ui.theme.SynapseTheme

/**
 * Themed `OutlinedTextField`: Surface3 fill, Line border, Md radius, BodyM type.
 * Focus ring is 2dp Accent.
 */
@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation =
        if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
    enabled: Boolean = true,
) {
    val c = SynapseTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = SynapseText.BodyS) },
        placeholder = placeholder?.let { { Text(it, style = SynapseText.BodyM, color = c.textDim) } },
        textStyle = SynapseText.BodyM,
        singleLine = singleLine,
        enabled = enabled,
        isError = error != null,
        supportingText = error?.let { { Text(it, style = SynapseText.BodyS, color = c.danger) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        shape = SynapseShapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = c.surface3,
            unfocusedContainerColor = c.surface3,
            disabledContainerColor = c.surface3,
            errorContainerColor = c.surface3,
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.line,
            disabledBorderColor = c.line,
            errorBorderColor = c.danger,
            focusedTextColor = c.text,
            unfocusedTextColor = c.text,
            disabledTextColor = c.textDim,
            errorTextColor = c.text,
            focusedLabelColor = c.accent,
            unfocusedLabelColor = c.textMuted,
            errorLabelColor = c.danger,
            cursorColor = c.accent,
        ),
        modifier = modifier,
    )
}
