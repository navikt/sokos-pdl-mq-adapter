package no.nav.pdl.person

import no.nav.pdl.aapenpersonpdldokumentv1.AapenPersonPdlDokumentV1
import no.nav.pdl.person.mqadapter.aapenPersonPdlDokument.AapenPersonPdlDokumentV1Mq
import no.nav.pdl.person.mqadapter.aapenPersonPdlDokument.Ident
import no.nav.pdl.person.mqadapter.aapenPersonPdlDokument.PersonInfo


object AapenPersonPdlDokumentV1Mapper {
    fun mapAapenPersonPdlDokumentV1(aapenPersonPdlDokumentV1FraKafka: AapenPersonPdlDokumentV1): AapenPersonPdlDokumentV1Mq? {
        val aapenPersonPdlDokumentV1Mq = AapenPersonPdlDokumentV1Mq()
        aapenPersonPdlDokumentV1Mq.ident = aapenPersonPdlDokumentV1FraKafka._id
        aapenPersonPdlDokumentV1FraKafka.value.let {
            val personInfo = PersonInfo()
            personInfo.fornavn = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.navn.fornavn
            personInfo.mellomnavn = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.navn.mellomnavn
            personInfo.etternavn = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.navn.etternavn
            personInfo.forkortetNavn = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.navn.forkortetNavn

            aapenPersonPdlDokumentV1Mq.personInfo = personInfo
            aapenPersonPdlDokumentV1Mq.identer = aapenPersonPdlDokumentV1FraKafka.value.hentIdenter
                .identer.map {
                    Ident()
                        .withIdent(it.ident)
                        .withAktiv(it.aktiv)
                        .withIdentifikatorType(it.identifikatorType.name)
                }
            return aapenPersonPdlDokumentV1Mq
        }
    }
}