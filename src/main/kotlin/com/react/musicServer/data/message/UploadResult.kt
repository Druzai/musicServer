package com.react.musicServer.data.message

data class UploadResult(
    val uuid: String,
    val filename: String,
    val wasTranscoded: Boolean,
)
