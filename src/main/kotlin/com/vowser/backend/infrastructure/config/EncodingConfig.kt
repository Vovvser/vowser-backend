package com.vowser.backend.infrastructure.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.filter.CharacterEncodingFilter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.charset.StandardCharsets

@Configuration
class EncodingConfig : WebMvcConfigurer {

    @Bean
    @Primary
    fun characterEncodingFilter(): CharacterEncodingFilter {
        val filter = CharacterEncodingFilter()
        filter.encoding = "UTF-8"
        filter.setForceEncoding(true)
        filter.setForceRequestEncoding(true)
        filter.setForceResponseEncoding(true)
        return filter
    }

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        }
    }

    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        val iterator = converters.iterator()
        while (iterator.hasNext()) {
            val converter = iterator.next()
            if (converter is MappingJackson2HttpMessageConverter) {
                iterator.remove()
            }
        }
        
        val jsonConverter = MappingJackson2HttpMessageConverter(objectMapper())
        jsonConverter.defaultCharset = StandardCharsets.UTF_8
        converters.add(0, jsonConverter)
        
        val stringConverter = StringHttpMessageConverter(StandardCharsets.UTF_8)
        stringConverter.setWriteAcceptCharset(false)
        converters.add(0, stringConverter)
    }
}