package no.nav.pdl.person.mqadapter.mq

import com.ibm.mq.jms.MQQueue
import com.ibm.msg.client.wmq.WMQConstants
import javax.jms.MessageProducer
import javax.jms.Session
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import no.nav.pdl.person.config.Configuration
import no.nav.pdl.person.mqadapter.SECURE_LOGGER_NAME
import no.nav.pdl.person.mqadapter.metrics.Metrics.antallFeiledeSendForsøkMotMq
import no.nav.pdl.person.mqadapter.metrics.Metrics.antallMeldingerSendtTilMq

private val logger = KotlinLogging.logger {}
private val secureLogger = KotlinLogging.logger(SECURE_LOGGER_NAME)
private const val timeOutTerskel: Long = 20_000
private const val forsinkelseSendtPåNytt: Long = 2000


class MqProducer(private val config: Configuration) {
    private lateinit var urMqSession: Session
    private lateinit var osMqSession: Session
    private lateinit var mqProducer: MessageProducer
    private var connected: Boolean = false

    init {
        connect()
    }


    private fun connect() {
        logger.info("Connecting to MQ...")
        runBlocking {
            try {
                connected = withTimeout(timeOutTerskel) {
                    val urMqConnection = config.mqProducerConfig.connect()
                    urMqSession = urMqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
                    val queue = (urMqSession.createQueue(config.mqProducerConfig.queue) as MQQueue).apply {
                        targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
                        messageBodyStyle = WMQConstants.WMQ_MESSAGE_BODY_MQ
                    }
                    mqProducer = urMqSession.createProducer(queue)
                    urMqConnection.start()
                    true
                }
                if (connected) {
                    logger.info("Connected to MQ")
                }
            } catch (e: TimeoutCancellationException) {
                logger.error("Greide ikke å koble til UR MQ i løpet av $timeOutTerskel ms")
                throw RuntimeException(e)
            }
            try {
                connected = withTimeout(timeOutTerskel) {
                    val osMqConnection = config.osMqProducerConfig.connect()
                    osMqSession = osMqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
                    val queue = (osMqSession.createQueue(config.osMqProducerConfig.queue) as MQQueue).apply {
                        targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
                        messageBodyStyle = WMQConstants.WMQ_MESSAGE_BODY_MQ
                    }
                    mqProducer = osMqSession.createProducer(queue)
                    osMqConnection.start()
                    true
                }
                if (connected) {
                    logger.info("Connected to MQ")
                }
            } catch (e: TimeoutCancellationException) {
                logger.error("Greide ikke å koble til OS MQ i løpet av $timeOutTerskel ms")
                throw RuntimeException(e)
            }
        }
    }

    suspend fun send(message: String, forsøkNr: Int = 0) {
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
    }
}

