package dev.vespera.player.download

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.io.File

private lateinit var appContext: Context

fun initializeFileStore(context: Context) { appContext = context.applicationContext }

actual object PlatformFileStore {
    actual fun save(fileName: String, bytes: ByteArray): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Vespera")
            }
            val uri = requireNotNull(appContext.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values))
            appContext.contentResolver.openOutputStream(uri).use { requireNotNull(it).write(bytes) }
            return uri.toString()
        }
        val directory = File(requireNotNull(appContext.getExternalFilesDir(null)), "Music/Vespera").apply { mkdirs() }
        return File(directory, fileName).apply { writeBytes(bytes) }.absolutePath
    }
}
