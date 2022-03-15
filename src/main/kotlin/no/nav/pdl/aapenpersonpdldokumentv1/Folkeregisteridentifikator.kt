package no.nav.pdl.aapenpersonpdldokumentv1

data class Folkeregisteridentifikator(
    val identifikasjonsnummer: String,
    val type: String,
    val status: String,
    val metadata: Metadata
)
