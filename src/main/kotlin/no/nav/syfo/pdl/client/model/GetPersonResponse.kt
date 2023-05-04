package no.nav.syfo.pdl.client.model

import java.lang.RuntimeException

data class GetPersonResponse(
    val data: ResponseData,
)

fun GetPersonResponse.toFnr(): String {
    return data.identer?.identer?.first()?.ident
        ?: throw RuntimeException("Fant ikke fnr i PDL")
}

data class ResponseData(
    val identer: IdentResponse?,
)

data class IdentResponse(
    val identer: List<Ident>,
)

data class Ident(
    val ident: String,
)
