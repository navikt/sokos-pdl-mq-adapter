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

private val logger = KotlinLogging.logger {}
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
        logger.info("Kobler til MQ")
        connected = withTimeout(timeOutTerskel) {
            val urOppsett = initialiserMq(config.urMqProducerConfig.connect(), config.urMqProducerConfig.queue)
            urMessageProducer = urOppsett.first
            urSession = urOppsett.second

            val osOppsett = initialiserMq(config.osMqProducerConfig.connect(), config.osMqProducerConfig.queue)
            osMessageProducer = osOppsett.first
            osSession = osOppsett.second

            true
        }
    }

    private fun initialiserMq(connection: Connection, queueNavn: String): Pair<MessageProducer, Session> {
        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val queue = (session.createQueue(queueNavn) as MQQueue).apply {
            targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
            messageBodyStyle = WMQConstants.WMQ_MESSAGE_BODY_MQ
        }
        val messageProducer = session.createProducer(queue)
        connection.start()
        logger.info("Koblet til MQ $queueNavn")
        return Pair(messageProducer, session)
    }

    suspend fun sendTilUr(message: String) {
        if (!connected) connect()
        urMessageProducer.send(urSession.createTextMessage(message))

    }

    suspend fun sendTilOs(message: String) {
        if (!connected) connect()
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

