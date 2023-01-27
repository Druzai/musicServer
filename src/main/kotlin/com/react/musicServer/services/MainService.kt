package com.react.musicServer.services

import com.react.musicServer.data.Data
import com.react.musicServer.data.RemoteFile
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.time.Instant
import java.util.*


@Service
class MainService {
    companion object {
        val anonymousFilename get() = "unknown-${Instant.now().epochSecond}.wav"
        const val MAX_FILENAME_SIZE = 100
        val DENIED_SYMBOLS = "\\W+".toRegex()
    }

    suspend fun download(uuid: UUID): Pair<ByteArray, String>? = Data.read(uuid)

    suspend fun delete(uuid: UUID): String? {
        val fileName = Data.delete(uuid)
        return fileName?.let {
            Data.delFromJson(it)
            Data.saveToJson()
            fileName
        }
    }

    suspend fun upload(file: RemoteFile): UploadResult {
        var fileName = getValidTitle(file.name)
        var uuid = UUID.nameUUIDFromBytes(fileName.toByteArray())
        val newConvFileName = fileName.substringBeforeLast(".").plus(".mp3")
        val newConvUUID = UUID.nameUUIDFromBytes(newConvFileName.toByteArray())
        if (Data.config.filesList.any {
                it.uuid == uuid || it.fileName == fileName || it.uuid == newConvUUID || it.fileName == newConvFileName
        }) // TODO: What if content differs, but names are the same? Consider use hashes.
            throw FileAlreadyExistsException(Path.of(Data.folder, fileName).toFile())
        val newFileName = Data.write(fileName, file)
        if (newFileName != null) {
            fileName = newFileName
            uuid = UUID.nameUUIDFromBytes(newFileName.toByteArray())
        }
        Data.addToJson(fileName, uuid)
        Data.saveToJson()
        return UploadResult(
            id=uuid.toString(),
            filename=fileName,
            wasTranscoded=newFileName != null,
        )
    }

    private fun getValidTitle(rawTitle: String?): String {
        if (rawTitle.isNullOrBlank())
            return anonymousFilename
        val name = rawTitle.substringBeforeLast('.')
        val extension = rawTitle.substringAfterLast('.')
        // TODO: Save titles separately from filenames, because filenames have "limitations"
        return name.take(MAX_FILENAME_SIZE)
                   .replace(DENIED_SYMBOLS, " ")
                   .replace("\\s+".toRegex(), " ")
                   .trim() + ".$extension"
    }

//    suspend fun upload(file: FilePart): Pair<String, UUID> {
//        val fileName = file.filename().length != 0 ? file.filename() : "unknown-".plus(Instant.now().epochSecond)
//        val uuid = UUID.nameUUIDFromBytes(fileName.toByteArray())
//        Data.write(fileName, file.bytes)
//        Data.addToJson(fileName, uuid)
//        Data.saveToJson()
//        return Pair(fileName, uuid)
//    }
    data class UploadResult(
        val id: String,
        val filename: String,
        val wasTranscoded: Boolean,
    )
}