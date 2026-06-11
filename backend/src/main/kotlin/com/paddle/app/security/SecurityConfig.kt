package com.paddle.app.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.web.cors.CorsConfiguration

@Configuration
@EnableWebSecurity
class SecurityConfig (
    private val securityConverter: SecurityConverter
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
        http.cors { corsCustomizer ->
            corsCustomizer.configurationSource {
                val config = CorsConfiguration()
                config.allowedOrigins = listOf("*")
                config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                config.allowedHeaders = listOf("*")
                config
            }
        }
        http.authorizeHttpRequests {
            it.requestMatchers("/api/users/onboard").authenticated()
            it.requestMatchers("/api/users/me").authenticated()
            it.requestMatchers("/api/matches/nearby").authenticated()
            it.requestMatchers("/api/matches/*/players").authenticated()
            it.anyRequest().access { authentication, _ ->
                val principal = authentication.get().principal
                AuthorizationDecision(principal is com.paddle.app.model.User)
            }
        }
        http.oauth2ResourceServer { it.jwt { customizer -> customizer.jwtAuthenticationConverter(securityConverter) } }
        return http.build()
    }
}