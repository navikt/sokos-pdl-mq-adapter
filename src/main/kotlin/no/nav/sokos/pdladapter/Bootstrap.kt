package no.nav.sokos.pdladapter

import kotlin.properties.Delegates
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.sokos.pdladapter.config.Configuration
import no.nav.sokos.pdladapter.config.propMap
import no.nav.sokos.pdladapter.kafka.PdlPersonDokumentRoute
import no.nav.sokos.pdladapter.metrics.Metrics
import no.nav.sokos.pdladapter.mq.MqProducer
import org.apache.kafka.clients.consumer.KafkaConsumer

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
            val kafkaConsumer: KafkaConsumer<String, String> =
                KafkaConsumer(
                    appConfig.kafkaConsumerConfig.propMap(useGroupId = true, useSecurity = appConfig.useAuthentication)
                )

            val mqProducer = MqProducer(appConfig)
            logger.info { "Applikasjonen er startet" }
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
