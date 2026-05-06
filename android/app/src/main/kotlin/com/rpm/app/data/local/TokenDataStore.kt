package com.rpm.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "rpm_prefs")

@Singleton
class TokenDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ACCESS_TOKEN  = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val USER_ID       = stringPreferencesKey("user_id")
    private val USER_ROLE     = stringPreferencesKey("user_role")
    private val USER_NAME     = stringPreferencesKey("user_name")

    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val userRole: Flow<String?> = context.dataStore.data.map { it[USER_ROLE] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }

    suspend fun getAccessToken(): String? = context.dataStore.data.first()[ACCESS_TOKEN]

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        role: String,
        name: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN]  = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
            prefs[USER_ID]       = userId
            prefs[USER_ROLE]     = role
            prefs[USER_NAME]     = name
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
