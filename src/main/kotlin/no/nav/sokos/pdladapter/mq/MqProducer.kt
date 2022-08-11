package no.nav.sokos.pdladapter.mq

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.mq.jms.MQQueue
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.WMQConstants
import mu.KotlinLogging
import no.nav.sokos.pdladapter.config.Configuration
import no.nav.sokos.pdladapter.config.MqProducerConfig
import javax.jms.Connection
import javax.jms.MessageProducer
import javax.jms.Session
import mu.KotlinLogging
import no.nav.sokos.pdladapter.config.Configuration

private val logger = KotlinLogging.logger {}

class MqProducer(private val config: Configuration) {
    private lateinit var urSession: Session
    private lateinit var osSession: Session
    private lateinit var urMessageProducer: MessageProducer
    private lateinit var osMessageProducer: MessageProducer
    private var connected: Boolean = false

    init {
        kobleTilMq()
    }

    private fun kobleTilMq() {
        logger.info("Kobler til MQ")
        val urOppsett = initialiserMq(config.urMqProducerConfig.connect(), config.urMqProducerConfig.queue)
        urMessageProducer = urOppsett.first
        urSession = urOppsett.second

        val osOppsett = initialiserMq(config.osMqProducerConfig.connect(), config.osMqProducerConfig.queue)
        osMessageProducer = osOppsett.first
        osSession = osOppsett.second

        connected = true
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

    fun sendTilUr(message: String) {
        try {
            if (!connected) kobleTilMq()
            urMessageProducer.send(urSession.createTextMessage(message))
            logger.info("Melding sendt til UR-kø")
        } catch (ex: Exception) {
            connected = false
            throw ex
        }

    }

    fun sendTilOs(message: String) {
        try {
            if (!connected) kobleTilMq()
            osMessageProducer.send(osSession.createTextMessage(message))
            logger.info("Melding sendt til OS-kø")
        } catch (ex: Exception) {
            connected = false
            throw ex
        }
    }

    private fun MqProducerConfig.connect(): Connection = MQConnectionFactory().also {
        it.transportType = WMQConstants.WMQ_CM_CLIENT
        it.hostName = host
        it.port = port.toInt()
        it.channel = channel
        it.queueManager = name
        it.targetClientMatching = true
        it.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
    }.createConnection(username, password)

}

