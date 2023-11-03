package no.nav.sokos.pdladapter.config

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import java.util.*
import mu.KotlinLogging
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringDeserializer

private val logger = KotlinLogging.logger { }

class Configuration(
    val httpPort: Int = readProperty("HTTP_PORT", default = "8080").toInt(),
    val kafkaConfig: KafkaConfig = KafkaConfig(),
    val kafkaConsumerConfig: KafkaConsumerConfig = KafkaConsumerConfig(kafkaConfig = kafkaConfig),
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

data class KafkaConfig(
    val brokers: String = readProperty("KAFKA_BROKERS"),
    val schemaRegistry: String = readProperty("KAFKA_SCHEMA_REGISTRY"),
    val schemaRegistryUser: String = readProperty("KAFKA_SCHEMA_REGISTRY_USER"),
    val schemaRegistryPassword: String = readProperty("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
    val useSSLSecurity: Boolean = readProperty("KAFKA_USE_SSL_SECURITY", "true") == "true",
    val propMap: Properties = Properties().apply {
        if (useSSLSecurity) {
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, readProperty("KAFKA_TRUSTSTORE_PATH"))
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, readProperty("KAFKA_CREDSTORE_PASSWORD"))
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, readProperty("KAFKA_KEYSTORE_PATH"))
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, readProperty("KAFKA_CREDSTORE_PASSWORD"))
        }
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
        put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistry)
        put(SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")
        put(SchemaRegistryClientConfig.USER_INFO_CONFIG, "${schemaRegistryUser}:$schemaRegistryPassword")
    },
)

data class KafkaConsumerConfig(
    val topic: String = readProperty("KAFKA_CONSUMER_TOPIC"),
    val consumerGroupId: String = readProperty("KAFKA_CONSUMER_GROUP_ID"),
    val offsetReset: String = readProperty("KAFKA_CONSUMER_OFFSET_RESET"),
    val kafkaConfig: KafkaConfig,
    val propMap: Properties = Properties().apply {
        put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId)
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1")
        put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "200000")
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset)
        put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed")
        putAll(kafkaConfig.propMap)
    },
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


