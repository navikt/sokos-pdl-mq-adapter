package no.nav.sokos.pdladapter.metrics

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.core.metrics.Counter

private const val METRICS_NAMESPACE = "sokos_pdl_mq_adapter"
private const val ANTALL_MELDINGER_MOTTATT_FRA_KAFKA = "${METRICS_NAMESPACE}_antall_meldinger_motatt_fra_kafka"

object Metrics {
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val antallMeldingerMottattFraKafka: Counter =
        Counter
            .builder()
            .name(ANTALL_MELDINGER_MOTTATT_FRA_KAFKA)
            .withoutExemplars()
            .register(prometheusRegistry.prometheusRegistry)
}
