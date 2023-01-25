package com.react.musicServer.services

import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.downloader.YoutubeCallback
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo
import com.github.kiulian.downloader.model.videos.VideoInfo
import com.github.kiulian.downloader.model.videos.formats.AudioFormat
import com.react.musicServer.data.Data
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.io.File
import java.net.URI
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


@Service
class YtUploadService {
    private val downloader = YoutubeDownloader()
    private val logger = LogManager.getLogger()

    suspend fun upload(url: String) {
        val videoId = extractId(url)
        val videoInfo = retrieveVideoInfo(videoId)
        val audio = videoInfo.bestAudioFormat() ?: throw InvalidLinkException("Unable to find audio format")
        if (audio.contentLength() > Data.MAX_FILE_SIZE)
            throw UploadSizeExceededException("Audio track presented is too long (${audio.contentLength()})")
        val file = downloadFromYt(audio)
        logger.debug("Saved ${file.absolutePath}")
    }

    private suspend fun downloadFromYt(format: AudioFormat) = suspendCancellableCoroutine { continuation ->
        val outputDir = File(Data.processingFolder)
        logger.debug("Starting downloading from ${format.url()}")
        val request = RequestVideoFileDownload(format)
            .saveTo(outputDir)
            .callback(object : YoutubeProgressCallback<File?> {
                override fun onDownloading(progress: Int) {
                    logger.debug("Download: $progress%")
                }

                override fun onFinished(data: File?)
                    = if (data != null)
                        continuation.resume(data)
                    else
                        continuation.resumeWithException(
                            DownloadFailedException("No file found on download finish")
                        )

                override fun onError(throwable: Throwable) {
                    continuation.resumeWithException(throwable)
                }
            }).async()
        downloader.downloadVideoFile(request)
    }

    private suspend fun retrieveVideoInfo(videoId: String) =
        suspendCancellableCoroutine { continuation ->
            val request = RequestVideoInfo(videoId)
                .callback(object : YoutubeCallback<VideoInfo?> {
                    override fun onFinished(videoInfo: VideoInfo?)
                        = if (videoInfo != null)
                            continuation.resume(videoInfo)
                        else
                            continuation.resumeWithException(InvalidLinkException("Unable to get video info"))

                    override fun onError(throwable: Throwable) {
                        logger.error("Failed to load video info (id: $videoId) : ${throwable.message}")
                        continuation.resumeWithException(throwable)
                    }
                }).async()
            downloader.getVideoInfo(request)
        }

    private fun extractId(url: String): String {
        val parsedUrl = URI.create(url).toURL()
        val videoId = parsedUrl.getQueryParameter("v") ?: parsedUrl.rootPath
        if (videoId == "watch" || videoId.isEmpty())
            throw InvalidLinkException("Video id not found")
        return videoId
    }

    private fun URL.getQueryParameter(key: String) = query?.splitToSequence('&')?.map {
        val parts = it.split('=')
        val name = parts.firstOrNull() ?: ""
        val value = parts.getOrNull(1) ?: ""
        name to value
    }?.firstOrNull { it.first == key }?.second

    private val URL.rootPath get() = path.split("/").getOrNull(1) ?: ""

    sealed class YtUploadException(override val message: String) : RuntimeException(message)
    class UploadSizeExceededException(message: String) : YtUploadException(message)
    class InvalidLinkException(message: String) : YtUploadException(message)
    class DownloadFailedException(message: String) : YtUploadException(message)
}
