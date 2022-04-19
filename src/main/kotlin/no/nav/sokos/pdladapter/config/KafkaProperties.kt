package no.nav.sokos.pdladapter.config

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

fun Configuration.KafkaConsumerConfig.propMap(useGroupId: Boolean, useSecurity: Boolean) = Properties().apply {
    if (useGroupId) {
        put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    }
    if (useSecurity) saslProperties(username, password)
    put(BOOTSTRAP_SERVERS_CONFIG, onPremBrokers)
    put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
    put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
    put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
    put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
    put(MAX_POLL_RECORDS_CONFIG, "1")
    put(MAX_POLL_INTERVAL_MS_CONFIG, "200000")
    put(ENABLE_AUTO_COMMIT_CONFIG, "false")
    put(AUTO_OFFSET_RESET_CONFIG, "none") //TODO Implementere støtte for å håndtere at man mister offset for topic
    put(SPECIFIC_AVRO_READER_CONFIG, true)
    put(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)
    put(BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")
}

private fun Properties.saslProperties(username: String, password: String) {
    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
    put(SaslConfigs.SASL_MECHANISM, "PLAIN")
    put(
        SaslConfigs.SASL_JAAS_CONFIG,
        """org.apache.kafka.common.security.plain.PlainLoginModule required username="$username" password="$password";"""
    )
}