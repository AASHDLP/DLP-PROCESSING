package com.gft.dlp.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.Properties;
import java.util.UUID;

public class KafkaConfiguration {
    public static final String SERVERS = "localhost:9092";
    public static final String TOPIC = "dlp-twitter";
    public static final long SLEEP_TIMER = 1000;


    public static Properties getKafkaProperties(){
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "com.gft.dlp.serializer.JobDeserializer");
        properties.put("group.id", "stream-output-client");
        //properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "com.gft.dlp.serializer.JobDeserializer");

        // SIn esta propertie no sacaba nada...
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());

        //SOLUTION

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }
}
