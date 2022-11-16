package no.nav.sokos.pdladapter

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.prometheus.client.exporter.common.TextFormat
import java.util.concurrent.TimeUnit
import no.nav.sokos.pdladapter.metrics.Metrics
import org.slf4j.event.Level

class HttpServer(
    appState: ApplicationState,
    port: Int = 8080,
) {
    private val embeddedServer = embeddedServer(Netty, port) {
        install(ContentNegotiation) {
            jackson {
                enable(SerializationFeature.INDENT_OUTPUT)
            }
        }
        install(CallLogging) {
            level = Level.INFO
            filter { call ->
                call.request.path().contains("/internal").not()
                    .and(call.request.path().contains("/metrics").not())
            }
        }
        install(MicrometerMetrics) {
            registry = Metrics.prometheusRegistry
            meterBinders = listOf(
                UptimeMetrics(),
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                JvmThreadMetrics(),
                ProcessorMetrics()
            )
        }
        routing {
            route("internal") {
                get("is_alive") {
                    when (appState.alive) {
                        true -> call.respondText { "Application is alive" }
                        else -> call.respondText(
                            text = "Application is not alive",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }
                get("is_ready") {
                    when (appState.ready) {
                        true -> call.respondText { "Application is ready" }
                        else -> call.respondText(
                            text = "Application is not ready",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }
            }
            route("metrics") {
                get {
                    call.respondText(ContentType.parse(TextFormat.CONTENT_TYPE_004)) { Metrics.prometheusRegistry.scrape() }
                }
            }
        }
    }

    fun start() = embeddedServer.start()
    fun stop() = embeddedServer.stop(5, 5, TimeUnit.SECONDS)
}
