package com.react.musicServer.controllers

import com.react.musicServer.data.message.ResponseError
import com.react.musicServer.exceptions.ConvertedFileSizeLimitException
import com.react.musicServer.exceptions.YoutubeUploadException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler
import java.io.FileNotFoundException
import java.nio.file.NoSuchFileException
import java.time.LocalDateTime


@ControllerAdvice
class FileExceptionAdvice : ResponseEntityExceptionHandler() {
    @ExceptionHandler(FileNotFoundException::class)
    fun handleFileNotFoundException(exc: FileNotFoundException): ResponseEntity<Any> {
        val details = arrayListOf(exc.message)
        val err = ResponseError(LocalDateTime.now(), "Файл не найден", details)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err)
    }

    @ExceptionHandler(NoSuchFileException::class)
    fun handleFileNotFoundException(exc: NoSuchFileException): ResponseEntity<Any> {
        val details = arrayListOf(exc.message)
        val err = ResponseError(LocalDateTime.now(), "Файл не найден (нет такого файла)", details)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxSizeException(exc: MaxUploadSizeExceededException): ResponseEntity<Any> {
        val details = arrayListOf(exc.message)
        val err = ResponseError(
            LocalDateTime.now(),
            String.format("Превышен размер файла (%.0f% МБ)", exc.maxUploadSize / (1024 * 1024)),
            details
        )
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(err)
    }

    @ExceptionHandler(ConvertedFileSizeLimitException::class)
    fun handleConvertedFileSizeLimitException(exc: ConvertedFileSizeLimitException): ResponseEntity<Any> {
        val details = arrayListOf(exc.message)
        val err = ResponseError(
            LocalDateTime.now(),
            String.format("Превышен размер перекодированного файла (%.0f% МБ)", exc.maxUploadSize / (1024 * 1024)),
            details
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err)
    }

    @ExceptionHandler(FileAlreadyExistsException::class)
    fun handleFileAlreadyExistsException(exc: FileAlreadyExistsException): ResponseEntity<Any> {
        val details = arrayListOf(exc.message)
        val err = ResponseError(LocalDateTime.now(), "Файл уже существует", details)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err)
    }

    @ExceptionHandler(YoutubeUploadException::class)
    fun handleYoutubeUploadException(exc: YoutubeUploadException): ResponseEntity<Any> {
        val details = arrayListOf(exc.message)
        val err = ResponseError(LocalDateTime.now(), "Не удалось загрузить звук из видео", details)
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(err)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exc: Exception): ResponseEntity<Any> {
        val details = arrayListOf(exc.message)
        val err = ResponseError(LocalDateTime.now(), "Что-то пошло не так >:(", details)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err)
    }
}