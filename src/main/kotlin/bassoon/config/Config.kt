package bassoon.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.FileSystems
import java.nio.file.Files

object Config {
    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    val config: ConfigDto = loadFromFile()

    private fun loadFromFile(): ConfigDto {
        val configPath = System.getProperty("config")
        val path = FileSystems.getDefault().getPath(configPath)

        return Files.newBufferedReader(path).use {
            mapper.readValue(it, ConfigDto::class.java)
        }
    }
}
