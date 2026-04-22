package no.nav.sokos.pdladapter.kafka

import java.time.Duration

import kotlin.time.Duration.Companion.seconds

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition

import no.nav.sokos.pdladapter.PdlPersonDokumentRoute
import no.nav.sokos.pdladapter.config.ApplicationState
import no.nav.sokos.pdladapter.mq.MqProducer

private const val KAFKA_TOPIC: String = "Ikke_interessant"
const val MELDING = "Dette er meldingen"

internal class PdlPersonDokumentRouteTest :
    FunSpec({

        val kafkaConsumer = mockk<KafkaConsumer<String, String>>(relaxed = true)
        val mqProducer = mockk<MqProducer>(relaxed = true)
        val pdlPersonDokumentRoute = PdlPersonDokumentRoute(KAFKA_TOPIC, kafkaConsumer, mqProducer)

        afterTest {
            clearAllMocks()
        }

        test("når melding fra kafka consumeres så skal meldingen legges på to køer") {
            val meldingen = "Dette er meldingen"
            every { kafkaConsumer.poll(Duration.ofMillis(0)) } returns enConsumerRecord()

            eventually(eventuallyConfig) {
                pdlPersonDokumentRoute.listen(ApplicationState())

                coVerify(exactly = 1) { mqProducer.sendTilOs(meldingen) }
                coVerify(exactly = 1) { mqProducer.sendTilUr(meldingen) }
                coVerify(exactly = 1) { mqProducer.commit() }
            }
        }

        test("ved mislykket forsøk på å lese melding fra kafka, så skal ikke  kafka-commit bli kalt") {
            every { kafkaConsumer.poll(Duration.ofMillis(0)) } throws Exception()

            shouldThrow<Exception> { pdlPersonDokumentRoute.listen(ApplicationState(alive = true)) }

            coVerify(exactly = 0) { kafkaConsumer.commitSync() }
        }

        test("når sending til første mq feiler, så skal ikke  kafka-commit bli kalt") {
            every { kafkaConsumer.poll(Duration.ofMillis(0)) } returns enConsumerRecord()
            coEvery { mqProducer.sendTilOs(any()) } throws Exception()

            shouldThrow<Exception> { pdlPersonDokumentRoute.listen(ApplicationState(alive = true)) }

            coVerify(exactly = 0) { kafkaConsumer.commitSync() }
        }

        test("når sending til andre mq feiler så skal ikke  kafka-commit bli kalt, men det aksepteres at første mg har fått meldingen etter prinsippet om at-least-once delivery") {
            val melding = "Dette er meldingen"
            every { kafkaConsumer.poll(Duration.ofMillis(0)) } returns enConsumerRecord()
            coEvery { mqProducer.sendTilUr(any()) } throws Exception()

            shouldThrow<Exception> { pdlPersonDokumentRoute.listen(ApplicationState(alive = true)) }

            coVerify(exactly = 1) { mqProducer.sendTilOs(melding) }
            coVerify(exactly = 0) { kafkaConsumer.commitSync() }
            coVerify(exactly = 0) { mqProducer.commit() }
        }

        test("når sending til OS MQ feiler skal det prøves på nytt 5 ganger") {
            every { kafkaConsumer.poll(Duration.ofMillis(0)) } returns enConsumerRecord()
            coEvery { mqProducer.sendTilOs(any()) } throws Exception()

            shouldThrow<Exception> { pdlPersonDokumentRoute.listen(ApplicationState(alive = true)) }

            coVerify(exactly = 5) { mqProducer.sendTilOs(MELDING) }
            coVerify(exactly = 0) { mqProducer.commit() }
        }

        test("når sending til UR MQ feiler skal det prøves på nytt 5 ganger") {
            every { kafkaConsumer.poll(Duration.ofMillis(0)) } returns enConsumerRecord()
            coEvery { mqProducer.sendTilUr(any()) } throws Exception()

            shouldThrow<Exception> { pdlPersonDokumentRoute.listen(ApplicationState(alive = true)) }

            coVerify(exactly = 5) { mqProducer.sendTilUr(MELDING) }
            coVerify(exactly = 0) { mqProducer.commit() }
        }
    })

private fun enConsumerRecord(): ConsumerRecords<String, String> =
    ConsumerRecords(
        mutableMapOf(
            TopicPartition(KAFKA_TOPIC, 1) to
                mutableListOf(
                    ConsumerRecord(KAFKA_TOPIC, 1, 0, "1", MELDING),
                ),
        ),
        emptyMap(),
    )

private val eventuallyConfig =
    eventuallyConfig {
        initialDelay = 1.seconds
        retries = 3
    }
