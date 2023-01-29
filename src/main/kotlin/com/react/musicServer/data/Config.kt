package com.react.musicServer.data

import java.util.*

data class Config (val filesList: ArrayList<Entry> = arrayListOf())

data class Entry(val fileName: String, val uuid: UUID)