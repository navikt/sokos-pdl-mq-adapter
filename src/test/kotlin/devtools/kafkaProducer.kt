package devtools


import no.nav.pdl.person.config.Configuration
import no.nav.pdl.person.config.propMap
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.File

val appConfig: Configuration = Configuration()
val kafkaProducer: KafkaProducer<String, String> = KafkaProducer(appConfig.kafkaConsumer.propMap(false, false))

fun main() {
    val jsonString: String = File("src/test/resources/kafka_response.json").readText(Charsets.UTF_8)

    val send = kafkaProducer.send(ProducerRecord(appConfig.kafkaConsumer.topic, jsonString))
    while (!send.isDone) {
        Thread.sleep(500)
    }
    println("Sent message")
}
