package no.nav.pdl.person.mqadapter

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.pdl.aapenpersonpdldokumentv1.AapenPersonPdlDokumentV1
import no.nav.pdl.person.config.Configuration
import no.nav.pdl.person.config.propMap
import no.nav.pdl.person.mqadapter.kafka.AapenPersonPdlDokumentV1Consumer
import no.nav.pdl.person.mqadapter.metrics.Metrics
import no.nav.pdl.person.mqadapter.mq.MqProducer
import org.apache.kafka.clients.consumer.KafkaConsumer
import kotlin.properties.Delegates

private val logger = KotlinLogging.logger {}
const val SECURE_LOGGER_NAME = "secureLogger"

@DelicateCoroutinesApi
fun main() {
    val appState = ApplicationState()
    val appConfig = Configuration()
    val httpServer = HttpServer(
        appState = appState,
        port = appConfig.httpPort
    )

    httpServer.start()

    appState.running = true

    GlobalScope.launch {
        do try {
            val kafkaConsumer: KafkaConsumer<String, AapenPersonPdlDokumentV1> =
                KafkaConsumer(
                    appConfig.kafkaConsumer.propMap(useGroupId = true, useSecurity = appConfig.useAuthentication))
            val mqProducer = MqProducer(appConfig)
            logger.info("MqProducer er opprettet")
            logger.info { "Applikasjonen er startet" }
            AapenPersonPdlDokumentV1Consumer(appConfig.kafkaConsumer.topic, kafkaConsumer, mqProducer).listen(appState)
        } catch (ex: Exception) {
            logger.error("En uventet feil har oppstått, prøver igjen", ex)
            delay(10000)
        }
        while (appState.running)
    }


    Runtime.getRuntime().addShutdownHook(Thread {
        appState.running = false
        httpServer.stop()
    })
}

class ApplicationState(
    defaultInitialized: Boolean = true,
    defaultRunning: Boolean = false
) {
    var initialized: Boolean by Delegates.observable(defaultInitialized) { _, _, newValue ->
        if (!newValue) Metrics.appStateReadyFalse.inc()
    }
    var running: Boolean by Delegates.observable(defaultRunning) { _, _, newValue ->
        if (!newValue) Metrics.appStateRunningFalse.inc()
    }
}
