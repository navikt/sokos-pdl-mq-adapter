package no.nav.sokos.pdladapter.person.mqadapter.kafka

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.sokos.pdladapter.person.mqadapter.ApplicationState
import no.nav.sokos.pdladapter.person.mqadapter.mq.MqProducer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration

internal class PdlPersonDokumentRouteTest {
    private val applicationState: ApplicationState = ApplicationState()
    private val kafkaTopic: String = "Ikke_Itressant"
    private val kafkaConsumer: KafkaConsumer<String, String> = mockk(relaxed = true)
    private val mqProducer: MqProducer = mockk(relaxed = true)
    private  val pdlPersonDokumentRoute : PdlPersonDokumentRoute = PdlPersonDokumentRoute(
        kafkaTopic,
        kafkaConsumer,
        mqProducer
    )

    @Test
    internal fun `når melding fra kafka consumeres så skal meldingen legges på to køer`() {
        val jsonString: String = File("src/test/resources/kafka_response.json").readText(Charsets.UTF_8)

        val consumerRecords: ConsumerRecords<String, String> = ConsumerRecords(
            mutableMapOf(
                TopicPartition(kafkaTopic, 1) to mutableListOf(
                    ConsumerRecord("1", 1, 0, "1", jsonString),
                )
            )
        )
        every { kafkaConsumer.poll(Duration.ofMillis(0)) } returns consumerRecords

        runBlocking { pdlPersonDokumentRoute.listen(applicationState) }

        coVerify(exactly = 1) { mqProducer.sendTilOs(jsonString) }
        coVerify(exactly = 1) { mqProducer.sendTilUr(jsonString) }
    }
}