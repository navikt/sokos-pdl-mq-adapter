package no.nav.sokos.pdladapter.person.mqadapter

import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import no.nav.sokos.pdladapter.person.mqadapter.metrics.installMetrics
import no.nav.sokos.pdladapter.person.mqadapter.plugins.configureSerialization
import no.nav.sokos.pdladapter.person.mqadapter.plugins.naisApi
import java.util.concurrent.TimeUnit

class HttpServer(
    appState: ApplicationState,
    port: Int = 8080,
) {
    private val embeddedServer = embeddedServer(Netty, port) {
        installMetrics()
        naisApi(alive = { appState.alive }, ready = { appState.ready })
        configureSerialization()
    }

    fun start() = embeddedServer.start()
    fun stop() = embeddedServer.stop(5, 5, TimeUnit.SECONDS)
}
