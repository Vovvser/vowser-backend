package com.vowser.backend

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun mongoDbContainer(): MongoDBContainer =
        MongoDBContainer(DockerImageName.parse("mongo:7"))

    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer(): GenericContainer<*> =
        GenericContainer(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379)

    @Bean
    @ServiceConnection
    fun mariaDbContainer(): MariaDBContainer<*> =
        MariaDBContainer(DockerImageName.parse("mariadb:11"))
            .withDatabaseName("vowser")
            .withUsername("vowser")
            .withPassword("pass")
}