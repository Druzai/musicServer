package com.react.musicServer.services

import com.react.musicServer.data.Data
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.time.Instant
import java.util.*

@Service
class MainService {
    suspend fun download(uuid: UUID): Pair<ByteArray, String>? = Data.read(uuid)

    suspend fun delete(uuid: UUID): String? {
        val fileName = Data.delete(uuid)
        return if (fileName != null){
            Data.delFromJson(fileName)
            Data.saveToJson()
            fileName
        } else
            null
    }

    suspend fun upload(file: FilePart): Pair<String, UUID> {
        val fileName = file.filename().ifEmpty { "unknown-".plus(Instant.now().epochSecond) }
        val uuid = UUID.nameUUIDFromBytes(fileName.toByteArray())
        if (Data.config.filesList.stream().anyMatch { it.uuid == uuid || it.fileName == fileName })
            throw FileAlreadyExistsException(Path.of(".").toFile())
        Data.write(fileName, file)
        Data.addToJson(fileName, uuid)
        Data.saveToJson()
        return Pair(fileName, uuid)
    }

//    suspend fun upload(file: FilePart): Pair<String, UUID> {
//        val fileName = file.filename().length != 0 ? file.filename() : "unknown-".plus(Instant.now().epochSecond)
//        val uuid = UUID.nameUUIDFromBytes(fileName.toByteArray())
//        Data.write(fileName, file.bytes)
//        Data.addToJson(fileName, uuid)
//        Data.saveToJson()
//        return Pair(fileName, uuid)
//    }
}