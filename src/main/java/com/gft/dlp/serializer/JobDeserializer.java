package com.gft.dlp.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.dlp.model.Job;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.Map;

public class JobDeserializer implements Deserializer<Job>
{

        private ObjectMapper objectMapper = new ObjectMapper();

           private boolean isKey;

        @Override
        public void configure(Map configs, boolean isKey) {
        this.isKey = isKey;
    }

        @Override
        public Job deserialize(String s, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        Job job = null;
        try {
            job = objectMapper.readValue(bytes, Job.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return job;
    }

    /*
    @Override
        public Job deserialize(String topic, Headers headers, byte[] data) {
        if (data == null) {
            return null;
        }

        Job job = null;
        try {
            job = objectMapper.readValue(data, Job.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return job;
    } */

        @Override
        public void close() {

    }
}
