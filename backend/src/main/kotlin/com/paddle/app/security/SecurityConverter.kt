package com.paddle.app.security

import com.paddle.app.repository.UserRepository
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class SecurityConverter (
    private val userRepository: UserRepository
): Converter<Jwt, AbstractAuthenticationToken>
{
    override fun convert(source: Jwt): AbstractAuthenticationToken {
        val firebaseUid = source.subject

        val user = userRepository.findUserByFirebaseUid(firebaseUid) ?: return JwtAuthenticationToken(source)

        return UsernamePasswordAuthenticationToken(user, source, emptyList())
    }


}


