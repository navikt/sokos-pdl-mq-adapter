package no.nav.pdl.aapenpersonpdldokumentv1

import com.google.gson.Gson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

internal class AapenPersonPdlDokumentV1Test{
    @Test
    fun kafkaMessageDeserialseTest() {
        val jsonString: String = File("src/test/resources/kafka_response.json").readText(Charsets.UTF_8)

        assertNotNull(jsonString)

        var aapenPersonPdlDokumentV1: AapenPersonPdlDokumentV1 = Gson().fromJson<AapenPersonPdlDokumentV1>(jsonString, AapenPersonPdlDokumentV1::class.java)

        assertEquals("1000005033396", aapenPersonPdlDokumentV1._id)
        assertEquals(1, aapenPersonPdlDokumentV1.value.hentPerson.folkeregisteridentifikator.size)
        assertEquals("01076502397", aapenPersonPdlDokumentV1.value.hentPerson.folkeregisteridentifikator.get(0).identifikasjonsnummer)
        assertEquals(1, aapenPersonPdlDokumentV1.value.hentPerson.navn.size)
        assertEquals("IDAR", aapenPersonPdlDokumentV1.value.hentPerson.navn.get(0).fornavn)
    }
}