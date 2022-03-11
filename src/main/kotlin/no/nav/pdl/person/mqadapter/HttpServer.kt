package no.nav.pdl.person.mqadapter

import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import java.util.concurrent.TimeUnit
import no.nav.pdl.person.mqadapter.metrics.installMetrics
import no.nav.pdl.person.mqadapter.plugins.configureSerialization
import no.nav.pdl.person.mqadapter.plugins.naisApi

class HttpServer(
    appState: ApplicationState,
    port: Int = 8080,
) {
    private val embeddedServer = embeddedServer(Netty, port) {
        installMetrics()
        naisApi({ appState.initialized }, { appState.running })
        configureSerialization()
    }

    fun start() = embeddedServer.start()
    fun stop() = embeddedServer.stop(5, 5, TimeUnit.SECONDS)
}
