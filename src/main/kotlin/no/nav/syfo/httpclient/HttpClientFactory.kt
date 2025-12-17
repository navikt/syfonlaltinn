package no.nav.syfo.httpclient

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.serialization.jackson.jackson
import no.nav.syfo.securelog
import javax.naming.ServiceUnavailableException

class HttpClientFactory private constructor() {
    companion object {
        fun getHttpClient(): HttpClient {
            val config: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
                install(ContentNegotiation) {
                    jackson {
                        registerKotlinModule()
                        registerModule(JavaTimeModule())
                        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    }
                }
                install(HttpRequestRetry) {
                    exponentialDelay(2.0, baseDelayMs = 1000, maxDelayMs = 20_000)
                    retryOnExceptionIf(5) { request, throwable ->
                        securelog.warn("Caught exception ${throwable.message}, for url ${request.url}")
                        true
                    }
                    retryIf(5) { request, response -> !response.status.isSuccess() }
                    retryIf(5) { _, response ->
                        when {
                            response.status == HttpStatusCode.Conflict -> false
                            response.status.isSuccess() -> false
                            else -> true
                        }
                    }
                }

                install(HttpTimeout) {
                    connectTimeoutMillis = 40000
                    requestTimeoutMillis = 40000
                    socketTimeoutMillis = 40000
                }
                HttpResponseValidator {
                    handleResponseExceptionWithRequest { exception, _ ->
                        when (exception) {
                            is SocketTimeoutException ->
                                throw ServiceUnavailableException(exception.message)
                        }
                    }
                }
            }
            return HttpClient(Apache, config)
        }
    }
}
