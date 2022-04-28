package no.nav.sokos.pdladapter

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.path
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit

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
            filter { call -> call.request.path().startsWith("/") }
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
        }
    }
    fun start() = embeddedServer.start()
    fun stop() = embeddedServer.stop(5, 5, TimeUnit.SECONDS)
}
