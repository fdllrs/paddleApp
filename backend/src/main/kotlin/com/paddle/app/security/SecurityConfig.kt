package com.paddle.app.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig (
    private val securityConverter: SecurityConverter
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {

        http.csrf { it.disable() }
        http.authorizeHttpRequests {
            it.requestMatchers("/api/users/onboard").authenticated()
            it.anyRequest().authenticated() }
        http.oauth2ResourceServer { it.jwt { customizer -> customizer.jwtAuthenticationConverter(securityConverter) } }
        return http.build()
    }
}