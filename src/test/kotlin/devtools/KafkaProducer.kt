package devtools

import java.io.File
import java.util.Properties

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import no.nav.sokos.pdladapter.config.Configuration

val appConfig: Configuration = Configuration()
val kafkaProducerProperties =
    Properties().apply {
        put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, appConfig.kafkaConfig.brokers)
        put(SCHEMA_REGISTRY_URL_CONFIG, appConfig.kafkaConfig.schemaRegistry)
    }
val kafkaProducer: KafkaProducer<String, String> = KafkaProducer(kafkaProducerProperties)

fun main() {
    val jsonString: String = File("src/test/resources/kafka_response.json").readText(Charsets.UTF_8)

    val send = kafkaProducer.send(ProducerRecord(appConfig.kafkaConsumerConfig.topic, jsonString))
    while (!send.isDone) {
        Thread.sleep(500)
    }
    println("Sent message")
}
