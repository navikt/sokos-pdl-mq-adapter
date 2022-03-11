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
            personInfo.fornavn = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.navn.get(0).fornavn
            personInfo.mellomnavn = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.navn.get(0).mellomnavn
            personInfo.etternavn = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.navn.get(0).etternavn
            personInfo.forkortetNavn = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.navn.get(0).forkortetNavn

            aapenPersonPdlDokumentV1Mq.personInfo = personInfo
            aapenPersonPdlDokumentV1Mq.identer = aapenPersonPdlDokumentV1FraKafka.value.hentIdenter
                .identer.map {
                    var withIdentifikatorType: Ident = Ident()
                        .withIdent(it.ident)
                        .withAktiv(it.aktiv)
                        .withIdentifikatorType(it.identifikatorType?.name)
                    withIdentifikatorType
                }
            return aapenPersonPdlDokumentV1Mq
        }
    }
}