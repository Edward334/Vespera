package dev.vespera.player.download

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual object PlatformFileStore {
    actual fun save(fileName: String, bytes: ByteArray): String {
        val directory = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        val path = "$directory/$fileName"
        check(bytes.toNSData().writeToFile(path, true)) { "无法保存下载文件" }
        return path
    }

    private fun ByteArray.toNSData(): NSData = usePinned { NSData.dataWithBytes(it.addressOf(0), size.toULong()) }
}
