package no.nav.sokos.pdladapter

import java.time.Duration
import java.util.UUID

import kotlinx.coroutines.time.delay

import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.MDC

import no.nav.sokos.pdladapter.config.ApplicationState
import no.nav.sokos.pdladapter.config.TEAM_LOGS_MARKER
import no.nav.sokos.pdladapter.metrics.Metrics
import no.nav.sokos.pdladapter.mq.MqProducer

private val logger = KotlinLogging.logger {}

class PdlPersonDokumentRoute(
    private val kafkaTopic: String,
    private val kafkaConsumer: KafkaConsumer<String, String>,
    private val mqProducer: MqProducer,
) {
    suspend fun listen(appState: ApplicationState) {
        kafkaConsumer.use { kafkaConsumer ->
            kafkaConsumer.subscribe(listOf(kafkaTopic))
            do {
                val consumerRecords: ConsumerRecords<String, String> = kafkaConsumer.poll(Duration.ofMillis(0))
                if (!consumerRecords.isEmpty) {
                    consumerRecords
                        .forEach { record ->
                            MDC.put("x-correlation-id", UUID.randomUUID().toString())
                            Metrics.antallMeldingerMottattFraKafka.inc()
                            logger.info(
                                "Record mottatt med offset = ${record.offset()}, partisjon = ${record.partition()}, topic = ${record.topic()}",
                            )
                            logger.info(marker = TEAM_LOGS_MARKER) { "Record: key = ${record.key()}, value = ${record.value()}" }
                            record.value()?.let {
                                retry { mqProducer.sendTilOs(it) }
                                retry { mqProducer.sendTilUr(it) }
                            }
                        }
                    mqProducer.commit()
                    kafkaConsumer.commitSync()
                } else {
                    delay(Duration.ofMillis(500))
                }
            } while (appState.alive)
        }
    }
}

suspend fun <T> retry(
    numOfRetries: Int = 5,
    initialDelayMs: Long = 250,
    block: suspend () -> T,
): T {
    var throwable: Exception? = null
    for (n in 1..numOfRetries) {
        try {
            return block()
        } catch (ex: Exception) {
            throwable = ex
            kotlinx.coroutines.delay(initialDelayMs)
        }
    }
    throw throwable!!
}
