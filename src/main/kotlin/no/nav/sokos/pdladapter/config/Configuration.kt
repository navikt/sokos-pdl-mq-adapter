package no.nav.sokos.pdladapter.config

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG
import mu.KotlinLogging
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

private val logger = KotlinLogging.logger { }

private val useAuthentication: Boolean = readProperty("USE_AUTHENTICATION", default = "true") != "false"

class Configuration(
    val httpPort: Int = readProperty("HTTP_PORT", default = "8080").toInt(),
    val kafkaConsumerConfig: KafkaConsumerConfig = KafkaConsumerConfig(),
    val urMqProducerConfig: MqProducerConfig = MqProducerConfig(
        queue = readProperty("UR_MQ_PRODUCER_QUEUE"),
        host = readProperty("UR_MQ_HOST"),
        port = readProperty("UR_MQ_PORT"),
        name = readProperty("UR_MQ_QUEUE_MANAGER_NAME"),
        channel = readProperty("UR_MQ_CHANNEL"),
        username = readProperty("MQ_USERNAME"),
        password = readProperty("MQ_PASSWORD")
    ),
    val osMqProducerConfig: MqProducerConfig = MqProducerConfig(
        queue = readProperty("OS_MQ_PRODUCER_QUEUE"),
        host = readProperty("OS_MQ_HOST"),
        port = readProperty("OS_MQ_PORT"),
        name = readProperty("OS_MQ_QUEUE_MANAGER_NAME"),
        channel = readProperty("OS_MQ_CHANNEL"),
        username = readProperty("MQ_USERNAME"),
        password = readProperty("MQ_PASSWORD")
    )
)

class KafkaConsumerConfig(
    val onPremBrokers: String = readProperty("ON_PREM_KAFKA_BROKERS"),
    val topic: String = readProperty("KAFKA_CONSUMER_TOPIC"),
    val schemaRegistryUrl: String = readProperty("KAFKA_SCHEMA_REGISTRY"),
    private val groupId: String = readProperty("KAFKA_CONSUMER_GROUP_ID"),
    private val username: String = readProperty("KAFKA_CONSUMER_USERNAME"),
    private val password: String = readProperty("KAFKA_CONSUMER_PASSWORD"),
    val propMap: Properties = Properties().apply {
        if (useAuthentication) {
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                """org.apache.kafka.common.security.plain.PlainLoginModule required username="$username" password="$password";"""
            )
        }
        put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
        put(BOOTSTRAP_SERVERS_CONFIG, onPremBrokers)
        put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        put(MAX_POLL_RECORDS_CONFIG, "1")
        put(MAX_POLL_INTERVAL_MS_CONFIG, "200000")
        put(ENABLE_AUTO_COMMIT_CONFIG, "false")
        put(AUTO_OFFSET_RESET_CONFIG, "none") //TODO Implementere støtte for å håndtere at man mister offset for topic
        put(SPECIFIC_AVRO_READER_CONFIG, true)
        put(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)
        put(BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")
    }
)
class MqProducerConfig(
    val password: String,
    val username: String,
    val channel: String,
    val name: String,
    val port: String,
    val host: String,
    val queue: String
)

private fun readProperty(name: String, default: String? = null) =
    System.getenv(name)
        ?: System.getProperty(name)
        ?: default.takeIf { it != null }?.also { logger.info("Bruker default verdi for property $name") }
        ?: throw RuntimeException("Mandatory property '$name' was not found")


