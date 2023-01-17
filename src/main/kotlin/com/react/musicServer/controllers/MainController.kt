package com.react.musicServer.controllers

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class MainController {
    @GetMapping("/")
    suspend fun index(model: Model): String{
        return "index";
    }

    @GetMapping("/music")
    suspend fun indexMusic(model: Model): String{
        return "music";
    }

    @GetMapping("/settings")
    suspend fun indexSettings(model: Model): String{
        return "settings";
    }
}