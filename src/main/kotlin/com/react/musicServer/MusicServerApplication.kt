package com.react.musicServer

import com.react.musicServer.data.Config
import com.react.musicServer.data.Data
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import kotlin.io.path.Path
import kotlin.io.path.createDirectory


@SpringBootApplication
class MusicServerApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MusicServerApplication::class.java, *args)
        }
    }

    @Bean
    fun demoData(): CommandLineRunner? {
        return CommandLineRunner {
            if (!Path(Data.configName).toFile().exists()) {
                Data.saveToJson(Config())
                println("Initialized config!")
            } else
                Data.config = Data.readFromJson()
            val filesPath = Path(Data.folder)
            try {
                if (!filesPath.toFile().exists()) {
                    filesPath.createDirectory()
                    println("Created subdir!")
                }
            } finally {
                println("Done preparing!")
            }
        }
    }
}
