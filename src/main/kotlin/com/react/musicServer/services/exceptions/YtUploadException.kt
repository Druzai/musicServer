package com.react.musicServer.services.exceptions

sealed class YtUploadException(override val message: String) : RuntimeException(message)