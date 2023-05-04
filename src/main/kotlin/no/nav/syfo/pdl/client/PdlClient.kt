package no.nav.syfo.pdl.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import no.nav.syfo.azuread.AccessTokenClient
import no.nav.syfo.log
import no.nav.syfo.pdl.client.exception.PersonNotFoundException
import no.nav.syfo.pdl.client.model.GetPersonRequest
import no.nav.syfo.pdl.client.model.GetPersonResponse
import no.nav.syfo.pdl.client.model.GetPersonVeriables
import no.nav.syfo.pdl.client.model.toFnr

class PdlClient(
    private val httpClient: HttpClient,
    private val basePath: String,
    private val pdlScope: String,
    private val accessTokenClient: AccessTokenClient,
    private val graphQlQuery: String = PdlClient::class.java.getResource("/graphql/getPerson.graphql").readText().replace(Regex("[\n\t]"), ""),
) {
    private val temaHeader = "TEMA"
    private val tema = "SYM"

    suspend fun getGjeldendeFnr(fnr: String): String {
        val token = accessTokenClient.getAccessToken(pdlScope)
        val getPersonRequest = GetPersonRequest(query = graphQlQuery, variables = GetPersonVeriables(ident = fnr))
        val pdlResponse = httpClient.post(basePath) {
            setBody(getPersonRequest)
            header(HttpHeaders.Authorization, "Bearer $token")
            header(temaHeader, tema)
            header(HttpHeaders.ContentType, "application/json")
        }.body<GetPersonResponse>()
        try {
            return pdlResponse.toFnr()
        } catch (e: Exception) {
            log.error("Error when getting pdlResponse", e)
            throw PersonNotFoundException(e.message)
        }
    }
}
