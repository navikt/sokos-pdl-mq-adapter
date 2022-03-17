package no.nav.pdl.person.config

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.WMQConstants
import mu.KotlinLogging
import javax.jms.Connection

private val logger = KotlinLogging.logger {  }

data class Configuration(
    val useAuthentication: Boolean = readProperty("USE_AUTHENTICATION", default = "true") != "false",
    val httpPort: Int = readProperty("HTTP_PORT", default = "8080").toInt(),
    val kafkaConsumer: KafkaConsumer = KafkaConsumer(),
    val mqProducerConfig: MqProducerConfig = MqProducerConfig(),
    val osMqProducerConfig: OsMqProducerConfig = OsMqProducerConfig()
) {
    data class MqProducerConfig  (val queue: String = readProperty("UR_MQ_PRODUCER_QUEUE")) : MqProperties()

    data class OsMqProducerConfig  (val queue: String = readProperty("OS_MQ_PRODUCER_QUEUE"))
        : MqProperties(readProperty("OS_MQ_HOST"),
        readProperty("OS_MQ_PORT"),
        readProperty("OS_MQ_QUEUE_MANAGER_NAME"),
        readProperty("OS_MQ_CHANNEL"),
        readProperty("MQ_USERNAME"),
        readProperty("MQ_PASSWORD")
    )

    open class MqProperties(
        val host: String = readProperty("UR_MQ_HOST"),
        val port: String = readProperty("UR_MQ_PORT"),
        val name: String = readProperty("UR_MQ_QUEUE_MANAGER_NAME"),
        val channel: String = readProperty("UR_MQ_CHANNEL"),
        val username: String = readProperty("MQ_USERNAME"),
        val password: String = readProperty("MQ_PASSWORD")
    ) {
        open fun connect(): Connection = MQConnectionFactory().also {
            it.transportType = WMQConstants.WMQ_CM_CLIENT
            it.hostName = host
            it.port = port.toInt()
            it.channel = channel
            it.queueManager = name
            it.targetClientMatching = true
            it.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
        }.createConnection(username, password)
    }


    data class KafkaConsumer(
        val kafkaBrokers: String = readProperty("KAFKA_BROKERS"),
        val groupId: String = readProperty("KAFKA_CONSUMER_GROUP_ID", ""),
        val maxPollRecords: String = "1",
        val maxPollInterval: String = "200000",
        val enableAutoCommit: String = "false",
        val autoOffsetReset: String = "latest",
        val topic: String = readProperty("KAFKA_CONSUMER_TOPIC"),
        val schemaRegistryUrl: String = readProperty("KAFKA_SCHEMA_REGISTRY"),
        val schemaRegistryUser: String = readProperty("KAFKA_SCHEMA_REGISTRY_USER", ""),
        val schemaRegistryPassword: String = readProperty("KAFKA_SCHEMA_REGISTRY_PASSWORD", ""),
        val javaKeystore: String = "jks",
        val pkcs12: String = "PKCS12",
        val truststore: String = readProperty("KAFKA_TRUSTSTORE_PATH", ""),
        val truststorePassword: String = readProperty("KAFKA_CREDSTORE_PASSWORD", ""),
        val keystoreLocation: String = readProperty("KAFKA_KEYSTORE_PATH", ""),
        val keystorePassword: String = readProperty("KAFKA_CREDSTORE_PASSWORD", ""),
    )
}

private fun readProperty(name: String, default: String? = null) =
    System.getenv(name)
        ?: System.getProperty(name)
        ?: default.takeIf { it != null }?.also { logger.info("Bruker default verdi for property $name") }
        ?: throw RuntimeException("Mandatory property '$name' was not found")


