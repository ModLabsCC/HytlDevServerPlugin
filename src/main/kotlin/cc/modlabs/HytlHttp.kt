package cc.modlabs

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json

object HytlHttp {
    fun create(config: HytlPluginConfig): HttpClient {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = config.httpTimeoutMs
                connectTimeoutMillis = config.httpTimeoutMs
                socketTimeoutMillis = config.httpTimeoutMs
            }

            defaultRequest {
                header(HttpHeaders.Authorization, "Bearer ${config.serverSecret}")
                header(HttpHeaders.ContentType, "application/json")
            }
        }
    }
}

