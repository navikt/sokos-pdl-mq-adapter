package no.nav.sokos.pdladapter

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.sokos.pdladapter.config.Configuration
import no.nav.sokos.pdladapter.metrics.Metrics
import no.nav.sokos.pdladapter.mq.MqProducer
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
    appState.alive = true

    GlobalScope.launch {
        try {
            val kafkaConsumer: KafkaConsumer<String, String> = KafkaConsumer(appConfig.kafkaConsumerConfig.propMap)
            val mqProducer = MqProducer(appConfig)
            PdlPersonDokumentRoute(appConfig.kafkaConsumerConfig.topic, kafkaConsumer, mqProducer).listen(appState)
        } catch (ex: Exception) {
            logger.error("En uventet feil har oppstått", ex)
            appState.alive = false
        }
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        appState.alive = false
        httpServer.stop()
    })
    logger.info { "Applikasjonen er startet" }
}

class ApplicationState(
    defaultInitialized: Boolean = true,
    defaultRunning: Boolean = false
) {
    var ready: Boolean by Delegates.observable(defaultInitialized) { _, _, newValue ->
        if (!newValue) Metrics.appStateReadyFalse.inc()
    }
    var alive: Boolean by Delegates.observable(defaultRunning) { _, _, newValue ->
        if (!newValue) Metrics.appStateAliveFalse.inc()
    }
}
