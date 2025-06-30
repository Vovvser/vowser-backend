package com.vowser.backend

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val dotenv = Dotenv.configure()
        .directory(System.getProperty("user.dir"))
        .filename(".env")
        .ignoreIfMissing()
        .load()

    dotenv.entries().forEach { entry ->
        System.setProperty(entry.key, entry.value)
    }

    runApplication<Application>(*args)
}