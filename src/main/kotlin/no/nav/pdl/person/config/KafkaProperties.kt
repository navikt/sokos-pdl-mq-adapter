package no.nav.pdl.person.config

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
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
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

fun Configuration.KafkaConsumer.propMap(useGroupId: Boolean, useSecurity: Boolean) = Properties().apply {
    if (useGroupId) {
        put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    }
    if (useSecurity) {
        sslSecuredProperties(truststore, truststorePassword, keystoreLocation, keystorePassword)
    }

    put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers)
    put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
    put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
    put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
    put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
    put(MAX_POLL_RECORDS_CONFIG, maxPollRecords)
    put(MAX_POLL_INTERVAL_MS_CONFIG, maxPollInterval)
    put(ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit)
    put(AUTO_OFFSET_RESET_CONFIG, autoOffsetReset)
    put(SPECIFIC_AVRO_READER_CONFIG, true)
    put(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)
    put(BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")
    put(SchemaRegistryClientConfig.USER_INFO_CONFIG, "${schemaRegistryUser}:$schemaRegistryPassword")
}

private fun Properties.sslSecuredProperties(
    truststore: String, truststorePassword: String, keystoreLocation: String, keystorePassword: String
) {
    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
    put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
    put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
    put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
    put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststore)
    put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword)
    put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation)
    put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword)
}