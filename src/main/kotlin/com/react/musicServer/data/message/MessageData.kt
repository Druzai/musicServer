package com.react.musicServer.data.message

import lombok.AllArgsConstructor
import lombok.Data


@Data
@AllArgsConstructor
class MessageData(
    val fileName: String? = null,
    val fileUrl: String? = null,
    val fileUUID: String? = null,
    val fileType: String? = null,
    val fileTranscoded: Boolean? = null,
    val message: String? = null
)