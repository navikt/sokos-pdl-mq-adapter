package no.nav.sokos.pdladapter

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.apache.kafka.clients.consumer.KafkaConsumer

import no.nav.sokos.pdladapter.config.ApplicationState
import no.nav.sokos.pdladapter.config.Configuration
import no.nav.sokos.pdladapter.config.applicationLifecycleConfig
import no.nav.sokos.pdladapter.config.commonConfig
import no.nav.sokos.pdladapter.config.routingConfig
import no.nav.sokos.pdladapter.mq.MqProducer
import no.nav.sokos.pdladapter.pdl.PdlService
import no.nav.sokos.pdladapter.util.launchBackgroundTask

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(true)
}

fun Application.module() {
    val applicationState = ApplicationState()
    val appConfig = Configuration()

    applicationLifecycleConfig(applicationState)
    commonConfig()
    routingConfig(applicationState)

    applicationState.onReady = {
        val kafkaConsumer: KafkaConsumer<String, String> = KafkaConsumer(appConfig.kafkaConsumerConfig.propMap)
        val mqProducer = MqProducer(appConfig)
        launchBackgroundTask(applicationState) {
            PdlService(appConfig.kafkaConsumerConfig.topic, kafkaConsumer, mqProducer).listen(applicationState)
        }
    }
}
