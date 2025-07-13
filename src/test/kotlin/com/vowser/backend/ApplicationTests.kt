package com.vowser.backend

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.cloud.compatibility-verifier.enabled=false"])
@EnableAutoConfiguration(exclude = [SpringDataWebAutoConfiguration::class])
class ApplicationTests {

	@Test
	fun contextLoads() {
	}

}
