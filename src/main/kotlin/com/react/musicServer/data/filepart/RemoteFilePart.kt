package com.react.musicServer.data.filepart

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.codec.multipart.FilePart
import java.nio.file.Path

class RemoteFilePart(
    private val filePart: FilePart
): RemoteFile {
    override val name get() = filePart.filename()
    override suspend fun save(filepath: Path) {
        filePart.transferTo(filepath).awaitSingleOrNull()
    }
}