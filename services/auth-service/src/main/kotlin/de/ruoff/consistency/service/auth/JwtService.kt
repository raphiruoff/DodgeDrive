package de.ruoff.consistency.service.auth

import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.Claims
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtService {


    private val secretKey: SecretKey = Keys.hmacShaKeyFor(
        "my-super-secure-key-1234567890123456".toByteArray()
    )
    private val expirationTime = 60 * 60 * 1000L // 1 Stunde

    fun generateToken(username: String): String {
        return Jwts.builder()
            .setSubject(username) //für wen ist token
            .setIssuedAt(Date()) // wird erstellt
            .setExpiration(Date(System.currentTimeMillis() + expirationTime))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact() // als String zurückgebne
    }





}
