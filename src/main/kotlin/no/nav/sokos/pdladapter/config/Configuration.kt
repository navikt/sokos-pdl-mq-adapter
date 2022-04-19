package no.nav.sokos.pdladapter.config

import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

data class Configuration(
    val useAuthentication: Boolean = readProperty("USE_AUTHENTICATION", default = "true") != "false",
    val httpPort: Int = readProperty("HTTP_PORT", default = "8080").toInt(),
    val kafkaConsumerConfig: KafkaConsumerConfig = KafkaConsumerConfig(),
) {
    val urMqProducerConfig = MqProducerConfig(
        queue = readProperty("UR_MQ_PRODUCER_QUEUE"),
        host = readProperty("UR_MQ_HOST"),
        port = readProperty("UR_MQ_PORT"),
        name = readProperty("UR_MQ_QUEUE_MANAGER_NAME"),
        channel = readProperty("UR_MQ_CHANNEL"),
        username = readProperty("MQ_USERNAME"),
        password = readProperty("MQ_PASSWORD")
    )

    val osMqProducerConfig = MqProducerConfig(
        queue = readProperty("OS_MQ_PRODUCER_QUEUE"),
        host = readProperty("OS_MQ_HOST"),
        port = readProperty("OS_MQ_PORT"),
        name = readProperty("OS_MQ_QUEUE_MANAGER_NAME"),
        channel = readProperty("OS_MQ_CHANNEL"),
        username = readProperty("MQ_USERNAME"),
        password = readProperty("MQ_PASSWORD")
    )

    data class KafkaConsumerConfig(
        val onPremBrokers: String = readProperty("ON_PREM_KAFKA_BROKERS"),
        val groupId: String = readProperty("KAFKA_CONSUMER_GROUP_ID"),
        val topic: String = readProperty("KAFKA_CONSUMER_TOPIC"),
        val username: String = readProperty("KAFKA_CONSUMER_USERNAME"),
        val password: String = readProperty("KAFKA_CONSUMER_PASSWORD"),
        val schemaRegistryUrl: String = readProperty("KAFKA_SCHEMA_REGISTRY"),
    )

    data class MqProducerConfig(
        val password: String,
        val username: String,
        val channel: String,
        val name: String,
        val port: String,
        val host: String,
        val queue: String
    )
}

private fun readProperty(name: String, default: String? = null) =
    System.getenv(name)
        ?: System.getProperty(name)
        ?: default.takeIf { it != null }?.also { logger.info("Bruker default verdi for property $name") }
        ?: throw RuntimeException("Mandatory property '$name' was not found")


