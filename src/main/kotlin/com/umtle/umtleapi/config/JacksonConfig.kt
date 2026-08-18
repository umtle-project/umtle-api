package com.umtle.umtleapi.config

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.std.ToStringSerializer

@Configuration
class JacksonConfig {
    @Bean
    fun longToStringJsonCustomizer(): JsonMapperBuilderCustomizer =
        JsonMapperBuilderCustomizer { builder ->
            builder.addModule(
                SimpleModule()
                    .addSerializer(Long::class.javaObjectType, ToStringSerializer.instance)
                    .addSerializer(Long::class.javaPrimitiveType, ToStringSerializer.instance),
            )
        }
}
