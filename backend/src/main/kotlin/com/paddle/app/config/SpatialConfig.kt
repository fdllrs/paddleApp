package com.paddle.app.config

import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SpatialConfig {
    @Bean
    fun geometryFactory(): GeometryFactory {
        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

        return geometryFactory
    }
}