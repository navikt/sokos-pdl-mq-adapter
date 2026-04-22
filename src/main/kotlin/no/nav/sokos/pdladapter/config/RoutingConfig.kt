package no.nav.sokos.pdladapter.config

import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.routingConfig(applicationState: ApplicationState) {
    routing {
        internalNaisRoutes(applicationState)
    }
}
