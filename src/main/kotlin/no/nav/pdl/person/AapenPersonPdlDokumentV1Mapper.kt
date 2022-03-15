package no.nav.pdl.person

import no.nav.pdl.aapenpersonpdldokumentv1.AapenPersonPdlDokumentV1
import no.nav.pdl.aapenpersonpdldokumentv1.Endringer
import no.nav.pdl.aapenpersonpdldokumentv1.Folkeregisteridentifikator
import no.nav.pdl.person.mqadapter.aapenPersonPdlDokument.AapenPersonPdlDokumentV1Mq
import no.nav.pdl.person.mqadapter.aapenPersonPdlDokument.EndringerTilMq
import no.nav.pdl.person.mqadapter.aapenPersonPdlDokument.FolkeregisteridentifikatorTilMq
import no.nav.pdl.person.mqadapter.aapenPersonPdlDokument.MetadataTilMq
import no.nav.pdl.person.mqadapter.aapenPersonPdlDokument.Navn


object AapenPersonPdlDokumentV1Mapper {
    fun mapAapenPersonPdlDokumentV1(aapenPersonPdlDokumentV1FraKafka: AapenPersonPdlDokumentV1): AapenPersonPdlDokumentV1Mq? {
        val aapenPersonPdlDokumentV1Mq = AapenPersonPdlDokumentV1Mq()
        aapenPersonPdlDokumentV1Mq.ident = aapenPersonPdlDokumentV1FraKafka._id
        aapenPersonPdlDokumentV1FraKafka.value.let {
            val navn = Navn()
            val navnMetadata = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.navn.get(0).metadata
            val navnEndringer: List<Endringer> = navnMetadata.endringer
            val endringerTilNavnMetadata: List<EndringerTilMq> = toEndringerTilMq(navnEndringer)
            val folkeregisteridentifikator: List<Folkeregisteridentifikator> = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.folkeregisteridentifikator
            val folkeregisteridentifikatorTilMq: List<FolkeregisteridentifikatorTilMq> =
                folkeregisteridentifikator.map {
                    val endringerTilFolkeregisteridentifikatorMetadata: List<EndringerTilMq> = toEndringerTilMq(it.metadata.endringer)

                    val withFolkeregisteridentifikatorTilMq = FolkeregisteridentifikatorTilMq()
                        .withIdentifikasjonsnummer(it.identifikasjonsnummer)
                        .withType(it.type)
                        .withStatus(it.status)
                        .withMetadata(
                            MetadataTilMq()
                                .withEndringer(endringerTilFolkeregisteridentifikatorMetadata)
                                .withHistorisk(it.metadata.historisk)
                        )
                    withFolkeregisteridentifikatorTilMq
                }

            navn.fornavn = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.navn.get(0).fornavn
            navn.mellomnavn = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.navn.get(0).mellomnavn
            navn.etternavn = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.navn.get(0).etternavn
            navn.forkortetNavn = aapenPersonPdlDokumentV1FraKafka.value.hentPerson.navn.get(0).forkortetNavn
            navn.metadata = MetadataTilMq().withEndringer(endringerTilNavnMetadata).withHistorisk(it.hentPerson.navn.get(0).metadata.historisk)
            aapenPersonPdlDokumentV1Mq.navn = navn
            aapenPersonPdlDokumentV1Mq.folkeregisteridentifikatorTilMq = folkeregisteridentifikatorTilMq

            return aapenPersonPdlDokumentV1Mq
        }
    }

     fun toEndringerTilMq(endringer: List<Endringer>): List<EndringerTilMq> {
        return endringer.map {
            val withEndringerTilMq = EndringerTilMq()
                .withType(it.type)
                .withRegistrert(it.registrert)
            withEndringerTilMq
        }
    }

}