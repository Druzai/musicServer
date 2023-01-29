package com.react.musicServer.services

import com.react.musicServer.data.Data
import com.react.musicServer.data.filepart.RemoteFile
import com.react.musicServer.data.message.UploadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.job.FFmpegJob
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.probe.FFmpegStream
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.io.path.absolutePathString
import kotlin.io.path.moveTo
import kotlin.io.path.name


@Service
class MainService {
    private val logger: Logger = LogManager.getLogger()
    private val ffmpeg: FFmpeg = FFmpeg()
    private val ffprobe: FFprobe = FFprobe()

    companion object {
        val anonymousFilename get() = "unknown-${Instant.now().epochSecond}"
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
        val newFileName = checkForTranscoding(fileName, Data.write(fileName, file))
        if (newFileName != null) {
            fileName = newFileName
            uuid = UUID.nameUUIDFromBytes(newFileName.toByteArray())
        }
        Data.addToJson(fileName, uuid)
        Data.saveToJson()
        return UploadResult(
            uuid = uuid.toString(),
            filename = fileName,
            wasTranscoded = newFileName != null,
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

    @Throws(IOException::class)
    private suspend fun checkForTranscoding(fileName: String, filepath: Path): String? {
        val probeResult: FFmpegProbeResult
        try {
            probeResult = ffprobe.probe(filepath.absolutePathString())
        } catch (ex: IOException) {
            logger.error(ex.message)
            withContext(Dispatchers.IO) {
                Files.deleteIfExists(filepath)
            }
            logger.info("Deleted uploaded file $fileName!")
            throw ex
        }
        logger.info("Format: ${probeResult.format.format_name}")
        logger.info("Bitrate: ${probeResult.format.bit_rate / 1024} kb/s")
        if (probeResult.streams.none { it.codec_type == FFmpegStream.CodecType.AUDIO }) {
            logger.error("No audio streams found in file ${fileName}!")
            withContext(Dispatchers.IO) {
                Files.deleteIfExists(filepath)
            }
            logger.info("Deleted uploaded file $fileName!")
            throw IOException("No audio streams found in file ${fileName}!")
        }
        return if (
            probeResult.format.format_name in arrayOf("mp3", "ogg") &&
            probeResult.streams.filter { it.codec_type == FFmpegStream.CodecType.AUDIO }.any {
                it.channels == 2 && it.sample_rate >= FFmpeg.AUDIO_SAMPLE_32000
            }
        )
            null
        else {
            val bitRate = (
                    probeResult.streams
                        .filter { it.codec_type == FFmpegStream.CodecType.AUDIO && it.bit_rate != 0L }
                        .maxOfOrNull { it.bit_rate }
                        ?.coerceIn(32_768, 327_680)
                        ?: 0
                    ).coerceAtLeast(
                    probeResult.format.bit_rate
                        .coerceIn(32_768, 327_680)
                )
            val audioSampleRate = (
                    probeResult.streams
                        .filter { it.codec_type == FFmpegStream.CodecType.AUDIO && it.sample_rate != 0 }
                        .maxOfOrNull { it.sample_rate }
                        ?.coerceIn(FFmpeg.AUDIO_SAMPLE_32000, FFmpeg.AUDIO_SAMPLE_48000)
                        ?: 0
                    ).coerceAtLeast(FFmpeg.AUDIO_SAMPLE_32000)
            logger.info("Found non-compatible file type! Converting to 'mp3'...")
            logger.info("New bitrate: ${bitRate / 1024} kb/s")
            logger.info("New audio sample rate: $audioSampleRate Hz")
            doTranscoding(
                filepath,
                audioSampleRate,
                bitRate,
                probeResult.format.format_name == "mp3" || fileName.endsWith(".mp3")
            )
        } // TODO: Add new size check, after conversion (m4a -> mp3 uncompressed, eg)
    }

    @Throws(IOException::class)
    private suspend fun doTranscoding(
        filePath: Path,
        audioSampleRate: Int,
        bitRate: Long,
        isMp3: Boolean
    ): String? {
        val newFileName = filePath.name.substringBeforeLast(".").plus(".mp3")
        val setFilePath =
            if (isMp3)
                filePath.moveTo(Paths.get(Data.processingFolder, filePath.name), true)
            else
                filePath

        val builder = FFmpegBuilder()
            .setInput(setFilePath.normalize().toString())
            .overrideOutputFiles(true)
            .addOutput(Paths.get(Data.folder, newFileName).normalize().toString())
            .setFormat("mp3")
            .setAudioChannels(FFmpeg.AUDIO_STEREO)
            .setAudioCodec("mp3")
            .setAudioSampleRate(audioSampleRate)
            .setAudioBitRate(bitRate)
            .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
            .done()

        val executor = FFmpegExecutor(ffmpeg, ffprobe)
        val job = executor.createJob(builder)

        val future = CompletableFuture
            .runAsync {
                job.run()
            }
            .handle { _, ex ->
                return@handle if (null != ex) {
                    logger.error("Failed to handle a file!", ex)
                    ex
                } else {
                    null
                }
            }

        withContext(Dispatchers.IO) {
            val exception = future.get()
            if (exception != null) {
                if (filePath.name != newFileName) {
                    withContext(Dispatchers.IO) {
                        Files.deleteIfExists(Paths.get(Data.folder, newFileName))
                    }
                }
                withContext(Dispatchers.IO) {
                    Files.deleteIfExists(setFilePath)
                }
                logger.info("Deleted uploaded file ${filePath.name}!")
                throw exception
            }
        }

        return if (job.state == FFmpegJob.State.FINISHED) {
            if (filePath.name != newFileName || isMp3) {
                withContext(Dispatchers.IO) {
                    Files.deleteIfExists(setFilePath)
                }
            }
            logger.info("Done file handling!")
            newFileName
        } else {
            null
        }
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