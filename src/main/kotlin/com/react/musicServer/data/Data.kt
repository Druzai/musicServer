package com.react.musicServer.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.react.musicServer.data.filepart.RemoteFile
import java.io.IOException
import java.io.Reader
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.*


object Data {
    const val MAX_FILE_SIZE = 104_857_600L
    const val folder: String = "musicFiles"
    const val processingFolder: String = "musicFiles/processing"
    const val configName: String = "config.json"
    var config: Config = Config()

    @Throws(IOException::class)
    fun saveToJson(config: Config? = Data.config) {
        val writer: Writer = Files.newBufferedWriter(Paths.get(configName))
        GsonBuilder().setPrettyPrinting().create().toJson(config, writer)
        writer.close()
    }

    @Throws(IOException::class)
    fun readFromJson(): Config {
        if (!Files.exists(Paths.get(configName))) saveToJson(Config())
        val reader: Reader = Files.newBufferedReader(Paths.get(configName))
        val config: Config = Gson().fromJson(reader, Config::class.java)
        reader.close()
        return config
    }

    fun addToJson(fileName: String, uuid: UUID) = config.filesList.add(Entry(fileName, uuid))

    fun delFromJson(fileName: String) = config.filesList.remove(config.filesList.find { it.fileName == fileName })

    suspend fun write(fileName: String, file: RemoteFile, dir: Path = Path(folder)): Path {
        val filepath: Path = Paths.get(dir.toString(), fileName)
        // TODO: Change to save to processing folder
        //  Then rename to uuid
        file.save(filepath)
        return filepath
    }

    fun read(uuid: UUID, dir: Path = Path(folder)): Pair<ByteArray, String>? {
        val fileName = config.filesList.find { it.uuid == uuid }?.fileName
        return if (fileName != null) {
            val filepath: Path = Paths.get(dir.toString(), fileName)
            val byteArray: ByteArray
            Files.newInputStream(filepath).use { os -> byteArray = os.readAllBytes() }
            Pair(byteArray, fileName)
        } else
            null
    }

    fun delete(uuid: UUID, dir: Path = Path(folder)): String? {
        val fileName = config.filesList.find { it.uuid == uuid }?.fileName
        return if (fileName != null) {
            val filepath: Path = Paths.get(dir.toString(), fileName)
            if (filepath.deleteIfExists()) fileName else null
        } else
            null
    }
}
