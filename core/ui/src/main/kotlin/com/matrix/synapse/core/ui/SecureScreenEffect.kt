package com.matrix.synapse.core.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Marks the hosting window FLAG_SECURE while this composable is in the tree, then clears it
 * on dispose. Use on screens that show secrets — the login/token-entry screen and the PIN
 * lock screen — to keep server-admin tokens, passwords, and PINs out of the recents
 * thumbnail and screen recordings (CWE-200).
 *
 * Scoped per-screen rather than set globally on the Activity so unrelated content screens
 * (used for store/F-Droid screenshot generation) remain capturable.
 */
@Composable
fun SecureScreenEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
