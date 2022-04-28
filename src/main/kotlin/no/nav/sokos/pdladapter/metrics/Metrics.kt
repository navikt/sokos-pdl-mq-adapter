package no.nav.sokos.pdladapter.metrics

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.Counter

private const val NAMESPACE = "sokos_krp_mq_adapter"

object Metrics {

    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val appStateAliveFalse: Counter = Counter.build()
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