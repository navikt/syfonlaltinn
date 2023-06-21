package no.nav.syfo.httpclient

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.serialization.jackson.jackson
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
