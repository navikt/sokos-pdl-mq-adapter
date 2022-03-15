package no.nav.pdl.aapenpersonpdldokumentv1

data class Identitet(
    val ident: String,
    val aktiv: Boolean,
    val identifikatorType: IdentifikatorType
)