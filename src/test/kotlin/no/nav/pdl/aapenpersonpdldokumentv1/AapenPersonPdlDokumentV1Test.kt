package no.nav.pdl.aapenpersonpdldokumentv1

import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

internal class AapenPersonPdlDokumentV1Test{
    @Test
    fun kotlinMessageDeserialseTest() {
        val jsonString: String = File("./kafka_response.json").readText(Charsets.UTF_8)

        assertNotNull(jsonString)
    }
}