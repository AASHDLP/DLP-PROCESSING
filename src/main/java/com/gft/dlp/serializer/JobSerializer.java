package com.gft.dlp.serializer;

import com.gft.dlp.model.Job;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class JobSerializer implements Serializer<Job> {

    private boolean isKey;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey)
    {
        this.isKey = isKey;
    }

    @Override
    public byte[] serialize(String topic, Job message)
    {
        if (message == null) {
            return null;
        }

        try {

            String json = objectMapper.writeValueAsString(message);
            System.out.println(json);
            byte[] bytes = objectMapper.writeValueAsBytes(message);
            return bytes;
        } catch (IOException | RuntimeException e) {
            throw new SerializationException("Error serializing value", e);
        }
    }

    @Override
    public void close()
    {

    }
}

