package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val OPACITY = floatPreferencesKey("opacity")
    val SIZE = intPreferencesKey("size")
    val IS_LOCKED = booleanPreferencesKey("is_locked")
    val SHAPE = stringPreferencesKey("shape") // "circle", "square", "rounded"
    val IMAGE_URI = stringPreferencesKey("image_uri")
    val VIDEO_URI = stringPreferencesKey("video_uri")
    val POS_X = intPreferencesKey("pos_x")
    val POS_Y = intPreferencesKey("pos_y")
    val GLOW_EFFECT = booleanPreferencesKey("glow_effect")
    val BORDER_EFFECT = booleanPreferencesKey("border_effect")
    val LOOP_VIDEO = booleanPreferencesKey("loop_video")
    val AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
}
