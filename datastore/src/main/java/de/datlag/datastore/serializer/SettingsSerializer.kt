package de.datlag.datastore.serializer

import androidx.datastore.core.Serializer
import de.datlag.datastore.SettingsPreferences
import io.michaelrocks.paranoid.Obfuscate
import java.io.InputStream
import java.io.OutputStream

@Obfuscate
class SettingsSerializer(isTelevision: Boolean, defaultDarkMode: Boolean) : Serializer<SettingsPreferences> {

    override val defaultValue: SettingsPreferences = SettingsPreferences.newBuilder()
        .setAppearance(SettingsPreferences.Appearance.newBuilder()
            .setDarkMode(defaultDarkMode)
            .setImproveDialog(!isTelevision))
        .setVideo(SettingsPreferences.Video.newBuilder()
            .setPreferMp4(false)
            .setPreviewEnabled(!isTelevision)
            .setDefaultFullscreen(true))
        .setUser(SettingsPreferences.User.newBuilder()
            .setMalAuth(String())
            .setMalImages(true)
            .setGithubAuth(String()))
        .setUsage(SettingsPreferences.Usage.newBuilder()
            .setSpentTime(0L)
            .setSaveAmount(0)
            .setTimeEditAmount(0F))
        .build()

    override suspend fun readFrom(input: InputStream): SettingsPreferences {
        return try {
            return SettingsPreferences.parseDelimitedFrom(input)
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: SettingsPreferences, output: OutputStream) = t.writeDelimitedTo(output)
}