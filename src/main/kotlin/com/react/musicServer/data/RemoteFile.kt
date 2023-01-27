package com.react.musicServer.data

import java.nio.file.Path

interface RemoteFile {
    val name: String
    suspend fun save(filepath: Path)
}
