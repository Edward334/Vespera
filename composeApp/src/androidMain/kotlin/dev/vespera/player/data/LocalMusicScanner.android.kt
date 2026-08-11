package dev.vespera.player.data

import android.content.Context
import android.provider.MediaStore
import dev.vespera.player.model.Song

private lateinit var scannerContext: Context
fun initializeLocalMusicScanner(context: Context) { scannerContext = context.applicationContext }

actual object LocalMusicScanner {
    actual fun scan(): List<Song> {
        if (!::scannerContext.isInitialized) return emptyList()
        val resolver = scannerContext.contentResolver
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION)
        return resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, "${MediaStore.Audio.Media.IS_MUSIC} = 1", null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID); val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE); val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST); val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION); buildList { while (cursor.moveToNext()) { val id = cursor.getLong(idIndex); add(Song(id, cursor.getString(titleIndex), listOf(cursor.getString(artistIndex)), durationMs = cursor.getLong(durationIndex), streamUrl = "${MediaStore.Audio.Media.EXTERNAL_CONTENT_URI}/$id")) } }
        } ?: emptyList()
    }
}
