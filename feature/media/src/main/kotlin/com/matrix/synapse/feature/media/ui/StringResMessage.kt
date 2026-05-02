package com.matrix.synapse.feature.media.ui

import androidx.annotation.StringRes

/**
 * User-facing text as a string resource id plus optional [formatArgs] for [android.content.Context.getString].
 */
data class StringResMessage(
    @StringRes val resId: Int,
    val formatArgs: List<Any> = emptyList(),
)
