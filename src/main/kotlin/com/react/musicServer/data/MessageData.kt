package com.react.musicServer.data

import lombok.AllArgsConstructor
import lombok.Data


@Data
@AllArgsConstructor
class MessageData(
    val fileName: String? = null,
    val fileUrl: String? = null,
    val fileUUID: String? = null,
    val fileType: String? = null,
    val message: String? = null
)