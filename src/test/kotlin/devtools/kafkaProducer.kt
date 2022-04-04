package devtools


import java.io.File
import no.nav.sokos.pdladapter.person.config.Configuration
import no.nav.sokos.pdladapter.person.config.propMap
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

val appConfig: Configuration = Configuration()
val kafkaProducer: KafkaProducer<String, String> = KafkaProducer(appConfig.kafkaConsumerConfig.propMap(false, false))

fun main() {
    val jsonString: String = File("src/test/resources/kafka_response.json").readText(Charsets.UTF_8)

    val send = kafkaProducer.send(ProducerRecord(appConfig.kafkaConsumerConfig.topic, jsonString))
    while (!send.isDone) {
        Thread.sleep(500)
    }
    println("Sent message")
}
