package com.lamesa.dominodominicano.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lamesa.dominodominicano.domain.GameConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("domino_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val GOAL = intPreferencesKey("goal")
        val COUNT_ALL = booleanPreferencesKey("count_all")
        val BONUS = intPreferencesKey("bonus_value")
        val CHUCHAZO = booleanPreferencesKey("chuchazo")
        val TRANQUE_IND = booleanPreferencesKey("tranque_individual")
    }

    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "es" }

    val gameConfig: Flow<GameConfig> = context.dataStore.data.map { prefs ->
        GameConfig(
            goal = prefs[Keys.GOAL] ?: 200,
            countAllTiles = prefs[Keys.COUNT_ALL] ?: true,
            bonusValue = prefs[Keys.BONUS] ?: 25,
            chuchazoEnabled = prefs[Keys.CHUCHAZO] ?: false,
            tranqueIndividual = prefs[Keys.TRANQUE_IND] ?: true
        )
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun saveGameConfig(config: GameConfig) {
        context.dataStore.edit {
            it[Keys.GOAL] = config.goal
            it[Keys.COUNT_ALL] = config.countAllTiles
            it[Keys.BONUS] = config.bonusValue
            it[Keys.CHUCHAZO] = config.chuchazoEnabled
            it[Keys.TRANQUE_IND] = config.tranqueIndividual
        }
    }
}
