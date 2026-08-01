@file:Suppress("unused")

package androidx.compose.foundation.layout

/**
 * Temporary Alpha 25A source-compatibility symbol.
 *
 * PracticalEntryActivity imports `androidx.compose.foundation.layout.weight`, while actual
 * Modifier.weight calls are resolved from RowScope/ColumnScope. Newer Compose exposes an
 * internal package symbol with the same import name, so this public no-argument marker keeps
 * the import legal without replacing or changing RowScope.weight behavior.
 *
 * Remove this file when the stale explicit import is removed from PracticalEntryActivity.
 */
fun weight(): Unit = Unit
