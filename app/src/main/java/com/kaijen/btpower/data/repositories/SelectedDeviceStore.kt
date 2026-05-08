package com.kaijen.btpower.data.repositories

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SelectedDeviceStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyMac = stringPreferencesKey("selected_device_mac")
    private val keyName = stringPreferencesKey("selected_device_name")

    val selectedDevice: Flow<SelectedDevice?> = context.dataStore.data.map { prefs ->
        val mac = prefs[keyMac] ?: return@map null
        val name = prefs[keyName] ?: mac
        SelectedDevice(mac, name)
    }

    suspend fun select(mac: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[keyMac] = mac
            prefs[keyName] = name
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(keyMac)
            prefs.remove(keyName)
        }
    }
}

data class SelectedDevice(val macAddress: String, val name: String)
