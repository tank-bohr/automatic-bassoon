package bassoon.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.FileSystems
import java.nio.file.Files

class Config {

    private val mapper = ObjectMapper(YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .registerKotlinModule()

    private val config by lazy {
        val configPath = System.getProperty("config")
        val path = FileSystems.getDefault().getPath(configPath)
        Files.newBufferedReader(path).use { mapper.readValue(it, ConfigDto::class.java) }
    }

    val clients = config.clients
    val callback = config.callback
}
