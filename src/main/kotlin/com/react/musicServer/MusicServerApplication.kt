package com.react.musicServer

import com.react.musicServer.data.CertChain
import com.react.musicServer.data.Config
import com.react.musicServer.data.Data
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import kotlin.io.path.Path
import kotlin.io.path.createDirectory


@SpringBootApplication
class MusicServerApplication {
    private val logger: Logger = LogManager.getLogger()
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MusicServerApplication::class.java, *args)
        }
    }

    @Bean
    fun demoData(): CommandLineRunner? {
        return CommandLineRunner {
            CertChain()
                .load("DigiCertGlobalRootCA.crt")
                .load("AmazonRootCA1.cer")
                .load("Comodo_AAA_Certificate_Services.crt")
                .done()
            logger.info("Injected certificates!")

            if (!Path(Data.configName).toFile().exists()) {
                Data.saveToJson(Config())
                logger.info("Initialized config!")
            } else
                Data.config = Data.readFromJson()
            arrayOf(Data.folder, Data.processingFolder).forEach {
                val filesPath = Path(it)
                try {
                    if (!filesPath.toFile().exists()) {
                        filesPath.createDirectory()
                        logger.info("Created subdir ${it}!")
                    }
                } catch (_: Exception){
                }
            }
            logger.info("Done preparing!")
        }
    }
}
