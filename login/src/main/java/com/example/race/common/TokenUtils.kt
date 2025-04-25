package com.example.race.common

import android.util.Base64
import org.json.JSONObject

object TokenUtils {
    fun decodeUsername(token: String?): String? {
        if (token == null) return null
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE), Charsets.UTF_8)
            val json = JSONObject(payload)
            json.getString("sub")
        } catch (e: Exception) {
            null
        }
    }
}
