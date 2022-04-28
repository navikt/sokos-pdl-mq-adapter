package no.nav.sokos.pdladapter

import kotlinx.coroutines.time.delay
import mu.KotlinLogging
import no.nav.sokos.pdladapter.metrics.Metrics
import no.nav.sokos.pdladapter.mq.MqProducer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.MDC
import java.time.Duration
import java.util.*

private val logger = KotlinLogging.logger {}
private val secureLogger = KotlinLogging.logger(SECURE_LOGGER_NAME)


class PdlPersonDokumentRoute(
    private val kafkaTopic: String,
    private val kafkaConsumer: KafkaConsumer<String, String>,
    private val mqProducer: MqProducer
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
                            logger.info("Record mottatt med offset = ${record.offset()}, partisjon = ${record.partition()}, topic = ${record.topic()}")
                            secureLogger.info("Record: key = ${record.key()}, value = ${record.value()}")
                            record.value()?.let {
                                retry { mqProducer.sendTilOs(it) }
                                retry { mqProducer.sendTilUr(it) }
                            }
                        }
                    kafkaConsumer.commitSync()
                } else {
                    delay(Duration.ofMillis(500))
                }
            } while (appState.alive)
        }
    }

}