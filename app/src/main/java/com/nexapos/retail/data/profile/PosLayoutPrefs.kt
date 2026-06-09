package com.nexapos.retail.data.profile

import android.content.Context

/** Persists the cashier's custom POS product-tile order as a list of product ids. */
object PosLayoutPrefs {
    private const val PREFS = "nexapos_layout"
    private const val KEY_ORDER = "product_order"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The saved id order (empty when the cashier hasn't arranged anything). */
    fun order(context: Context): List<String> =
        prefs(context).getString(KEY_ORDER, "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

    fun setOrder(
        context: Context,
        ids: List<String>,
    ) {
        prefs(context).edit().putString(KEY_ORDER, ids.joinToString("\n")).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_ORDER).apply()
    }
}
