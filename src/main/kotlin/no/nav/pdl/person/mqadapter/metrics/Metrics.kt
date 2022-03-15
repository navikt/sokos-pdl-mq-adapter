package no.nav.pdl.person.mqadapter.metrics

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.ContentType
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.Counter
import io.prometheus.client.exporter.common.TextFormat

private const val NAMESPACE = "sokos_krp_mq_adapter"

object Metrics {

    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val appStateRunningFalse: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("app_state_running_false")
        .help("app state running changed to false")
        .register(prometheusRegistry.prometheusRegistry)

    val appStateReadyFalse: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("app_state_ready_false")
        .help("app state ready changed to false")
        .register(prometheusRegistry.prometheusRegistry)

    val antallMeldingerMottattFraKafka: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("antall_meldinger_mottatt_fra_kafka")
        .help("Antall meldinger mottatt fra kafka-topic")
        .register(prometheusRegistry.prometheusRegistry)

    val antallMeldingerSendtTilMq: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("antall_meldinger_sendt_til_mq")
        .help("Antall meldinger sendt til mq-kø")
        .register(prometheusRegistry.prometheusRegistry)

    val antallFeiledeSendForsøkMotMq: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("antall_feilede_send_forsoek_mot_mq")
        .help("Antall feilede send forsøk mot mq-kø")
        .register(prometheusRegistry.prometheusRegistry)
}

fun Application.installMetrics() {
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
        route("metrics") {
            get {
                call.respondText(ContentType.parse(TextFormat.CONTENT_TYPE_004)) { Metrics.prometheusRegistry.scrape() }
            }
        }
    }
}