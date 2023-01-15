package com.react.musicServer.controllers

import com.react.musicServer.data.ResponseError
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
        val details: MutableList<String?> = ArrayList()
        details.add(exc.message)
        val err = ResponseError(LocalDateTime.now(), "File Not Found", details)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err)
    }

    @ExceptionHandler(NoSuchFileException::class)
    fun handleFileNotFoundException(exc: NoSuchFileException): ResponseEntity<Any> {
        val details: MutableList<String?> = ArrayList()
        details.add(exc.message)
        val err = ResponseError(LocalDateTime.now(), "File Not Found", details)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxSizeException(exc: MaxUploadSizeExceededException): ResponseEntity<Any> {
        val details: MutableList<String?> = ArrayList()
        details.add(exc.message)
        val err = ResponseError(LocalDateTime.now(), "File Size Exceeded", details)
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(err)
    }

    @ExceptionHandler(FileAlreadyExistsException::class)
    fun handleMaxSizeException(exc: FileAlreadyExistsException): ResponseEntity<Any> {
        val details: MutableList<String?> = ArrayList()
        details.add(exc.message)
        val err = ResponseError(LocalDateTime.now(), "File Already Exists", details)
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(err)
    }

    @ExceptionHandler(Exception::class)
    fun handleMaxSizeException(exc: Exception): ResponseEntity<Any> {
        val details: MutableList<String?> = ArrayList()
        details.add(exc.message)
        val err = ResponseError(LocalDateTime.now(), "Something went wrong >:(", details)
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(err)
    }
}