package no.nav.pdl.person.mqadapter.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.pdl.person.mqadapter.ApplicationState
import no.nav.pdl.person.mqadapter.SECURE_LOGGER_NAME
import no.nav.pdl.person.mqadapter.X_CORRELATION_ID
import no.nav.pdl.person.mqadapter.aapenPersonPdlDokument.AapenPersonPdlDokumentV1Mq
import no.nav.pdl.person.mqadapter.metrics.Metrics
import no.nav.pdl.person.mqadapter.mq.MqProducer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.MDC
import java.time.Duration
import java.util.*

private val logger = KotlinLogging.logger {}
private val secureLogger = KotlinLogging.logger(SECURE_LOGGER_NAME)


class AapenPersonPdlDokumentV1Consumer(
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
                    logger.debug("Mottatt ${consumerRecords.count()} meldinger fra Kontoregister person")
                    consumerRecords
                        .forEach { record ->
                            Metrics.antallMeldingerMottattFraKafka.inc()
                            onRecord(record)
                        }
                    kafkaConsumer.commitSync()
                } else {
                    delay(Duration.ofMillis(500))
                }
            } while (appState.running)
        }
    }

    private suspend fun onRecord(record: ConsumerRecord<String, String>)  = coroutineScope {
        MDC.put(X_CORRELATION_ID, UUID.randomUUID().toString())

        withContext(coroutineContext) {
            logger.info("Record mottatt med offset = ${record.offset()}")
            secureLogger.info("Record: key = ${record.key()}, value = ${record.value()}")
            if (record.value() != null) {
                /*val aapenPersonPdlDokumentV1 = mapAapenPersonPdlDokumentV1(record.value())
                if (aapenPersonPdlDokumentV1 != null) {
                    mqProducer.send(aapenPersonPdlDokumentV1.tilJson())
                }*/
                mqProducer.send(record.value())
            }
        }
    }
}

fun AapenPersonPdlDokumentV1Mq.tilJson(): String {
    return jacksonObjectMapper().writeValueAsString(this)
}