package com.gft.dlp.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.dlp.model.JobExtended;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.Map;

public class JobExtendedDeserializer implements Deserializer<JobExtended>
{

        private ObjectMapper objectMapper = new ObjectMapper();

           private boolean isKey;

        @Override
        public void configure(Map configs, boolean isKey) {
        this.isKey = isKey;
    }

        @Override
        public JobExtended deserialize(String s, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JobExtended job = null;
        try {
            job = objectMapper.readValue(bytes, JobExtended.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return job;
    }


        @Override
        public void close() {

    }
}
