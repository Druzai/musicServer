package com.react.musicServer.exceptions

sealed class YoutubeUploadException(override val message: String) : RuntimeException(message)