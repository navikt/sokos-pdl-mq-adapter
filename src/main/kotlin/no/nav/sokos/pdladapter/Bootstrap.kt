package no.nav.sokos.pdladapter

import kotlin.properties.Delegates
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.sokos.pdladapter.config.Configuration
import no.nav.sokos.pdladapter.metrics.Metrics
import no.nav.sokos.pdladapter.mq.MqProducer
import org.apache.kafka.clients.consumer.KafkaConsumer

private val logger = KotlinLogging.logger {}
const val SECURE_LOGGER_NAME = "secureLogger"

fun main() = runBlocking {
    val appState = ApplicationState()
    val appConfig = Configuration()
    val httpServer = HttpServer(
        appState = appState,
        port = appConfig.httpPort
    )
    httpServer.start()
    appState.alive = true

    Runtime.getRuntime().addShutdownHook(Thread {
        appState.alive = false
        httpServer.stop()
    })

    do try {
        val kafkaConsumer: KafkaConsumer<String, String> = KafkaConsumer(appConfig.kafkaConsumerConfig.propMap)
        val mqProducer = MqProducer(appConfig)
        PdlPersonDokumentRoute(appConfig.kafkaConsumerConfig.topic, kafkaConsumer, mqProducer).listen(appState)
        logger.info { "Applikasjonen er startet" }
    } catch (ex: Exception) {
        logger.error("En uventet feil har oppstått", ex)
        appState.alive = false
    } while (appState.alive)

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
