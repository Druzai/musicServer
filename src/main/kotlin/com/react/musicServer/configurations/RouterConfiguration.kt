package com.react.musicServer.configurations

import com.react.musicServer.controllers.ApiHandler
import kotlinx.coroutines.FlowPreview
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.coRouter


@Configuration
class RouterConfiguration {
    @FlowPreview
    @Bean
    fun productRoutes(apiHandler: ApiHandler) = coRouter {
        GET("/api/download/{uuid:.+}", apiHandler::download)
        HEAD("/api/download/{uuid:.+}", apiHandler::downloadHead)
//        GET("/api/upload", apiHandler::upload)
//        DELETE("/api/delete/{uuid:.+}", apiHandler::delete)
        GET("/api/getMusic", apiHandler::getMusic)
    }
}