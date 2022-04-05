package no.nav.sokos.pdladapter.person.mqadapter.mq

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.mq.jms.MQQueue
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.WMQConstants
import javax.jms.Connection
import javax.jms.MessageProducer
import javax.jms.Session
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import no.nav.sokos.pdladapter.person.config.Configuration
import no.nav.sokos.pdladapter.person.mqadapter.SECURE_LOGGER_NAME

private val logger = KotlinLogging.logger {}
private val secureLogger = KotlinLogging.logger(SECURE_LOGGER_NAME)
private const val timeOutTerskel: Long = 20_000


class MqProducer(private val config: Configuration) {
    private lateinit var urSession: Session
    private lateinit var osSession: Session
    private lateinit var urMessageProducer: MessageProducer
    private lateinit var osMessageProducer: MessageProducer
    private var connected: Boolean = false

    init {
        runBlocking { connect() }  //TODO Undersøke om det er en riktigere måte å gjøre dette på
    }

    private suspend fun connect() {
        logger.info("Connecting to MQ...")
        connected = withTimeout(timeOutTerskel) {
            val urMqConnection = config.urMqProducerConfig.connect()
            urSession = urMqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val urQueue = (urSession.createQueue(config.urMqProducerConfig.queue) as MQQueue).apply {
                targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
                messageBodyStyle = WMQConstants.WMQ_MESSAGE_BODY_MQ
            }
            urMessageProducer = urSession.createProducer(urQueue)
            urMqConnection.start()

            val osMqConnection = config.osMqProducerConfig.connect()
            osSession = osMqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val osQueue = (osSession.createQueue(config.osMqProducerConfig.queue) as MQQueue).apply {
                targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
                messageBodyStyle = WMQConstants.WMQ_MESSAGE_BODY_MQ
            }
            osMessageProducer = osSession.createProducer(osQueue)
            osMqConnection.start()

            true
        }
        logger.info("Connected to MQ")
    }

    suspend fun sendTilUr(message: String) {
        if (!connected) connect()
        logger.info("Sender melding til MQ.")
        secureLogger.info("Sender melding til MQ: $message")
        urMessageProducer.send(urSession.createTextMessage(message))

    }

    suspend fun sendTilOs(message: String) {
        if (!connected) connect()
        logger.info("Sender melding til MQ.")
        secureLogger.info("Sender melding til MQ: $message")
        osMessageProducer.send(osSession.createTextMessage(message))
    }

    private fun Configuration.MqProducerConfig.connect(): Connection = MQConnectionFactory().also {
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

