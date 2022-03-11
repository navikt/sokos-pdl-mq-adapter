package no.nav.pdl.person

import com.google.gson.Gson
import no.nav.pdl.aapenpersonpdldokumentv1.AapenPersonPdlDokumentV1
import no.nav.pdl.person.mqadapter.aapenPersonPdlDokument.AapenPersonPdlDokumentV1Mq
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.io.File

internal class AapenPersonPdlDokumentV1MapperTest {

    @Test
    fun mapAapenPersonPdlDokumentV1() {
        val jsonString: String = File("src/test/resources/kafka_response.json").readText(Charsets.UTF_8)

        assertNotNull(jsonString)

        var aapenPersonPdlDokumentV1: AapenPersonPdlDokumentV1 = Gson().fromJson<AapenPersonPdlDokumentV1>(jsonString, AapenPersonPdlDokumentV1::class.java)

        val aapenPersonPdlDokumentV1Mq: AapenPersonPdlDokumentV1Mq? =AapenPersonPdlDokumentV1Mapper.mapAapenPersonPdlDokumentV1(aapenPersonPdlDokumentV1)

        assertNotNull(aapenPersonPdlDokumentV1Mq)

        val jsonMessageToMq: String = Gson().toJson(aapenPersonPdlDokumentV1Mq)

        assertNotNull(jsonMessageToMq)
    }
}