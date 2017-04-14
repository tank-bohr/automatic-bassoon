package bassoon.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

class Config {
    fun loadFromResources(): ConfigDto {
        val configPath = javaClass.classLoader.getResource("config.yml").getFile()
        return loadFromFile(path = FileSystems.getDefault().getPath(configPath))
    }

    fun loadFromFile(path: Path): ConfigDto {
        val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
        mapper.registerModule(KotlinModule()) // Enable Kotlin support

        return Files.newBufferedReader(path).use {
            mapper.readValue(it, ConfigDto::class.java)
        }
    }
}
