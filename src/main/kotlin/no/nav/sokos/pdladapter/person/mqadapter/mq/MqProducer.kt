package no.nav.sokos.pdladapter.person.mqadapter.mq

import com.ibm.mq.jms.MQQueue
import com.ibm.msg.client.wmq.WMQConstants
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
    private lateinit var urMqSession: Session
    private lateinit var osMqSession: Session
    private lateinit var mqProducer: MessageProducer
    private var connected: Boolean = false

    init {
        runBlocking { connect() }
    }


    private suspend fun connect() {
        logger.info("Connecting to MQ...")
            connected = withTimeout(timeOutTerskel) {
                val urMqConnection = config.urMqProducerConfig.connect()
                urMqSession = urMqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
                val urQueue = (urMqSession.createQueue(config.urMqProducerConfig.queue) as MQQueue).apply {
                    targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
                    messageBodyStyle = WMQConstants.WMQ_MESSAGE_BODY_MQ
                }
                mqProducer = urMqSession.createProducer(urQueue)
                urMqConnection.start()

                val osMqConnection = config.osMqProducerConfig.connect()
                osMqSession = osMqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
                val osQueue = (osMqSession.createQueue(config.osMqProducerConfig.queue) as MQQueue).apply {
                    targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
                    messageBodyStyle = WMQConstants.WMQ_MESSAGE_BODY_MQ
                }
                mqProducer = osMqSession.createProducer(osQueue)
                osMqConnection.start()

                true
            }
                logger.info("Connected to MQ")
    }

    /*suspend fun send(message: String, forsøkNr: Int = 0) {
        try {
            if (!connected) connect()
            logger.info("Sender melding til MQ.")
            secureLogger.info("Sender melding til MQ: $message")
            mqProducer.send(urMqSession.createTextMessage(message))
            antallMeldingerSendtTilMq.inc()
            logger.debug("Melding sendt ok til MQ")
        } catch (ex: Exception) {
            logger.warn("Feil ved forsøk på å sende melding til MQ: ${ex.message}")
            antallFeiledeSendForsøkMotMq.inc()
            connected = false
            delay(timeMillis = forsinkelseSendtPåNytt)
            if (forsøkNr < 6) {
                val nesteForsøk = forsøkNr + 1
                logger.warn("Forsøker å sende melding til MQ på nytt. Forsøk nr: $nesteForsøk")
                send(message, nesteForsøk)
                logger.debug("Melding sendt ok til MQ på forsøk nr: $nesteForsøk")
            } else {
                secureLogger.error { "Greide ikke å å sende melding til MQ: $message" }
                throw RuntimeException("Kunne ikke sende meldingen til MQ.")
            }
        }
    }*/

    suspend fun sendTilUr(message: String) {
        if (!connected) connect()
        logger.info("Sender melding til MQ.")
        secureLogger.info("Sender melding til MQ: $message")
        mqProducer.send(urMqSession.createTextMessage(message))

    }

    suspend fun sendTilOs(message: String) {
        if (!connected) connect()
        logger.info("Sender melding til MQ.")
        secureLogger.info("Sender melding til MQ: $message")
        mqProducer.send(osMqSession.createTextMessage(message))
    }
}

