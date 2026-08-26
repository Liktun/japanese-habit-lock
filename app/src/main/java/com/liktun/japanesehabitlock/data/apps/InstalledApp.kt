package com.liktun.japanesehabitlock.data.apps

/**
 * One app the user could choose to block.
 *
 * The two fields serve different masters: [label] is the human-readable name shown in the
 * picker and may change between locales or app updates, while [packageName] is the stable
 * identifier that gets persisted in the blocked list and later compared against the
 * foreground package. Never persist [label].
 */
data class InstalledApp(val packageName: String, val label: String)
