package com.crk.timeforsalah.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// Single DataStore for the whole app (one file)
private const val DS_NAME = "time_for_salah_settings"

// Use this from ALL stores:
val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = DS_NAME)
