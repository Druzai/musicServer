package com.react.musicServer.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
import java.io.IOException
import java.io.Reader
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.io.path.*


object Data {
    private val logger: Logger = LogManager.getLogger()
    const val MAX_FILE_SIZE = 104_857_600L
    const val folder: String = "musicFiles"
    const val processingFolder: String = "musicFiles/processing"
    const val configName: String = "config.json"
    var config: Config = Config()
    private val ffmpeg: FFmpeg = FFmpeg()
    private val ffprobe: FFprobe = FFprobe()

    @Throws(IOException::class)
    fun saveToJson(config: Config? = Data.config) {
        val writer: Writer = Files.newBufferedWriter(Paths.get(configName))
        GsonBuilder().setPrettyPrinting().create().toJson(config, writer)
        writer.close()
    }

    @Throws(IOException::class)
    fun readFromJson(): Config {
        if (!Files.exists(Paths.get(configName))) saveToJson(Config())
        val reader: Reader = Files.newBufferedReader(Paths.get(configName))
        val config: Config = Gson().fromJson(reader, Config::class.java)
        reader.close()
        return config
    }

    fun addToJson(fileName: String, uuid: UUID) = config.filesList.add(Entry(fileName, uuid))

    fun delFromJson(fileName: String) = config.filesList.remove(config.filesList.find { it.fileName == fileName })

    @Throws(IOException::class)
    private suspend fun doTranscoding(filePath: Path, audioSampleRate: Int, bitRate: Long, isMp3: Boolean): String? {
        val newFileName = filePath.name.substringBeforeLast(".").plus(".mp3")
        val setFilePath =
            if (isMp3)
                filePath.moveTo(Paths.get(processingFolder, filePath.name), true)
            else
                filePath

        val builder = FFmpegBuilder()
            .setInput(setFilePath.normalize().toString())
            .overrideOutputFiles(true)
            .addOutput(Paths.get(folder, newFileName).normalize().toString())
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
                        Files.deleteIfExists(Paths.get(folder, newFileName))
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

    suspend fun write(fileName: String, file: RemoteFile, dir: Path = Path(folder)): String? {
        val filepath: Path = Paths.get(dir.toString(), fileName)
        file.save(filepath)
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

    fun read(uuid: UUID, dir: Path = Path(folder)): Pair<ByteArray, String>? {
        val fileName = config.filesList.find { it.uuid == uuid }?.fileName
        return if (fileName != null) {
            val filepath: Path = Paths.get(dir.toString(), fileName)
            val byteArray: ByteArray
            Files.newInputStream(filepath).use { os -> byteArray = os.readAllBytes() }
            Pair(byteArray, fileName)
        } else
            null
    }

    fun delete(uuid: UUID, dir: Path = Path(folder)): String? {
        val fileName = config.filesList.find { it.uuid == uuid }?.fileName
        return if (fileName != null) {
            val filepath: Path = Paths.get(dir.toString(), fileName)
            if (filepath.deleteIfExists()) fileName else null
        } else
            null
    }
}
