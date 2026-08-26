package com.example.japanesehabitlock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val STORE_NAME = "habit_lock"

/**
 * The one DataStore for the whole app.
 *
 * It is deliberately a single process-wide instance: the Accessibility Service
 * added in a later step runs in the same process as the UI, and DataStore throws
 * if two instances ever point at the same file. Both sides must go through this
 * property.
 */
val Context.habitLockDataStore: DataStore<Preferences> by preferencesDataStore(name = STORE_NAME)
