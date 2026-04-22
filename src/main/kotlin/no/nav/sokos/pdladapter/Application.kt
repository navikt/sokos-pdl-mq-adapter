package no.nav.sokos.pdladapter

import kotlinx.coroutines.runBlocking

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.KafkaConsumer

import no.nav.sokos.pdladapter.config.ApplicationState
import no.nav.sokos.pdladapter.config.Configuration
import no.nav.sokos.pdladapter.config.applicationLifecycleConfig
import no.nav.sokos.pdladapter.config.commonConfig
import no.nav.sokos.pdladapter.config.routingConfig
import no.nav.sokos.pdladapter.mq.MqProducer

private val logger = KotlinLogging.logger {}

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(true)
}

fun Application.module() {
    val applicationState = ApplicationState()
    val appConfig = Configuration()

    applicationLifecycleConfig(applicationState)
    commonConfig()
    routingConfig(applicationState)

    runBlocking {
        do {
            try {
                val kafkaConsumer: KafkaConsumer<String, String> = KafkaConsumer(appConfig.kafkaConsumerConfig.propMap)
                val mqProducer = MqProducer(appConfig)
                PdlPersonDokumentRoute(appConfig.kafkaConsumerConfig.topic, kafkaConsumer, mqProducer).listen(applicationState)
                logger.info { "Applikasjonen er avsluttet" }
            } catch (ex: Exception) {
                logger.error("En uventet feil har oppstått", ex)
                applicationState.alive = false
            }
        } while (applicationState.alive)
    }
}
