package com.futura.audio.data.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.securityDataStore by preferencesDataStore(name = "futura_security_prefs")

/** Whether the stealth PIN has been read from storage yet, and if so what it is. */
sealed interface PinState {
    /** Storage has not been read yet. Do not pick a start screen in this state. */
    data object Loading : PinState
    data object Unset : PinState
    data class Set(val hash: String) : PinState
}

class SecurityPreferences(context: Context) {

    private val dataStore = context.applicationContext.securityDataStore

    companion object {
        private val PIN_HASH_KEY = stringPreferencesKey("correct_pin_hash")
    }

    val pinState: Flow<PinState> = dataStore.data.map { prefs ->
        val hash = prefs[PIN_HASH_KEY]
        if (hash.isNullOrEmpty()) PinState.Unset else PinState.Set(hash)
    }

    suspend fun savePinHash(pinHash: String) {
        dataStore.edit { prefs ->
            prefs[PIN_HASH_KEY] = pinHash
        }
    }
}
