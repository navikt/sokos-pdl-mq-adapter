package no.nav.sokos.pdladapter.person.config

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.WMQConstants
import javax.jms.Connection
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

data class Configuration(
    val useAuthentication: Boolean = readProperty("USE_AUTHENTICATION", default = "true") != "false",
    val httpPort: Int = readProperty("HTTP_PORT", default = "8080").toInt(),
    val kafkaConsumerConfig: KafkaConsumerConfig = KafkaConsumerConfig(),
    val urMqProducerConfig: UrMqProducerConfig = UrMqProducerConfig(),
    val osMqProducerConfig: OsMqProducerConfig = OsMqProducerConfig()
) {
    data class KafkaConsumerConfig(
        val onPremBrokers: String = readProperty("ON_PREM_KAFKA_BROKERS"),
        val groupId: String = readProperty("KAFKA_CONSUMER_GROUP_ID"),
        val topic: String = readProperty("KAFKA_CONSUMER_TOPIC"),
        val username: String = readProperty("KAFKA_CONSUMER_USERNAME"),
        val password: String = readProperty("KAFKA_CONSUMER_PASSWORD"),
        val schemaRegistryUrl: String = readProperty("KAFKA_SCHEMA_REGISTRY"),
    )

    data class UrMqProducerConfig(
        override val queue: String = readProperty("UR_MQ_PRODUCER_QUEUE"),
        override val host: String = readProperty("UR_MQ_HOST"),
        override val port: String = readProperty("UR_MQ_PORT"),
        override val name: String = readProperty("UR_MQ_QUEUE_MANAGER_NAME"),
        override val channel: String = readProperty("UR_MQ_CHANNEL"),
        override val username: String = readProperty("MQ_USERNAME"),
        override val password: String = readProperty("MQ_PASSWORD")
    ) : MqProducerConfig()

    data class OsMqProducerConfig(
        override val queue: String = readProperty("OS_MQ_PRODUCER_QUEUE"),
        override val host: String = readProperty("OS_MQ_HOST"),
        override val port: String = readProperty("OS_MQ_PORT"),
        override val name: String = readProperty("OS_MQ_QUEUE_MANAGER_NAME"),
        override val channel: String = readProperty("OS_MQ_CHANNEL"),
        override val username: String = readProperty("MQ_USERNAME"),
        override val password: String = readProperty("MQ_PASSWORD")
    ) : MqProducerConfig()


    abstract class MqProducerConfig {
        abstract val password: String
        abstract val username: String
        abstract val channel: String
        abstract val name: String
        abstract val port: String
        abstract val host: String
        abstract val queue: String

        fun connect(): Connection = MQConnectionFactory().also {
            it.transportType = WMQConstants.WMQ_CM_CLIENT
            it.hostName = host
            it.port = port.toInt()
            it.channel = channel
            it.queueManager = name
            it.targetClientMatching = true
            it.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
        }
            .createConnection(username, password)
    }
}

private fun readProperty(name: String, default: String? = null) =
    System.getenv(name)
        ?: System.getProperty(name)
        ?: default.takeIf { it != null }?.also { logger.info("Bruker default verdi for property $name") }
        ?: throw RuntimeException("Mandatory property '$name' was not found")


