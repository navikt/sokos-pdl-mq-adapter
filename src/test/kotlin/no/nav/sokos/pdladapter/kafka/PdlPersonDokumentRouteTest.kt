package no.nav.sokos.pdladapter.kafka

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.sokos.pdladapter.ApplicationState
import no.nav.sokos.pdladapter.mq.MqProducer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

internal class PdlPersonDokumentRouteTest {
    private val kafkaTopic: String = "Ikke_interessant"
    private val kafkaConsumer: KafkaConsumer<String, String> = mockk(relaxed = true)
    private val mqProducer: MqProducer = mockk(relaxed = true)
    private val pdlPersonDokumentRoute = PdlPersonDokumentRoute(kafkaTopic, kafkaConsumer, mqProducer)

    @Test
    fun `når melding fra kafka consumeres så skal meldingen legges på to køer`() {
        val meldingen = "Dette er meldingen"
        every { kafkaConsumer.poll(Duration.ofMillis(0)) } returns enConsumerRecord(meldingen)

        runBlocking { pdlPersonDokumentRoute.listen(ApplicationState()) }

        coVerify(exactly = 1) { mqProducer.sendTilOs(meldingen) }
        coVerify(exactly = 1) { mqProducer.sendTilUr(meldingen) }
    }

    @Test
    fun `ved mislykket forsøk på å lese melding fra kafka, så skal ikke  kafka-commit bli kalt`() {
        every { kafkaConsumer.poll(Duration.ofMillis(0)) } throws Exception()

        assertThrows<Exception> { runBlocking { pdlPersonDokumentRoute.listen(ApplicationState(defaultRunning = true)) } }

        coVerify(exactly = 0) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `når sending til første mq feiler, så skal ikke  kafka-commit bli kalt`() {
        every { kafkaConsumer.poll(Duration.ofMillis(0)) } returns enConsumerRecord("Dette er meldingen")
        coEvery { mqProducer.sendTilOs(any()) } throws Exception()

        assertThrows<Exception> { runBlocking { pdlPersonDokumentRoute.listen(ApplicationState(defaultRunning = true)) } }

        coVerify(exactly = 0) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `når sending til andre mq feiler så skal ikke  kafka-commit bli kalt, men det aksepteres at første mg har fått meldingen etter prinsippet om at-least-once delivery`() {
        val melding = "Dette er meldingen"
        every { kafkaConsumer.poll(Duration.ofMillis(0)) } returns enConsumerRecord(melding)
        coEvery { mqProducer.sendTilUr(any()) } throws Exception()

        assertThrows<Exception> { runBlocking { pdlPersonDokumentRoute.listen(ApplicationState(defaultRunning = true)) } }

        coVerify(exactly = 1) { mqProducer.sendTilOs(melding) }
        coVerify(exactly = 0) { kafkaConsumer.commitSync() }
    }

    @Test
    internal fun `når sending til OS MQ feiler skal det prøves på nytt 5 ganger`() {
        val melding = "Dette er meldingen"
        every { kafkaConsumer.poll(Duration.ofMillis(0)) } returns enConsumerRecord(melding)
        coEvery { mqProducer.sendTilOs(any()) } throws Exception()

        assertThrows<Exception> { runBlocking { pdlPersonDokumentRoute.listen(ApplicationState(defaultRunning = true)) } }

        coVerify(exactly = 5) { mqProducer.sendTilOs(melding) }
    }

    @Test
    internal fun `når sending til UR MQ feiler skal det prøves på nytt 5 ganger`() {
        val melding = "Dette er meldingen"
        every { kafkaConsumer.poll(Duration.ofMillis(0)) } returns enConsumerRecord(melding)
        coEvery { mqProducer.sendTilUr(any()) } throws Exception()

        assertThrows<Exception> { runBlocking { pdlPersonDokumentRoute.listen(ApplicationState(defaultRunning = true)) } }

        coVerify(exactly = 5) { mqProducer.sendTilUr(melding) }
    }

    private fun enConsumerRecord(melding: String) = ConsumerRecords(
        mutableMapOf(
            TopicPartition(kafkaTopic, 1) to mutableListOf(
                ConsumerRecord(kafkaTopic, 1, 0, "1", melding),
            )
        )
    )
}