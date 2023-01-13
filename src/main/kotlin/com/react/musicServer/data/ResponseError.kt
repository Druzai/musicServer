package com.react.musicServer.data

import com.fasterxml.jackson.annotation.JsonFormat
import lombok.AllArgsConstructor
import lombok.Data
import java.time.LocalDateTime


@Data
@AllArgsConstructor
class ResponseError (
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    val timestamp: LocalDateTime? = null,
    val message: String? = null,
    val errors: List<*>? = null
)