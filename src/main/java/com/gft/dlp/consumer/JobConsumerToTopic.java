package com.gft.dlp.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.dlp.model.Job;
import com.gft.dlp.model.JobExtended;
import org.apache.commons.lang.SerializationException;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.streaming.runtime.tasks.AsynchronousException;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Properties;

public class JobConsumerToTopic {

    public static void main(String[] args) throws Exception, AsynchronousException {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.setParallelism(1);

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");
        properties.setProperty("group.id", "flinkKafka");


        FlinkKafkaConsumer<String> kafkaSource = new FlinkKafkaConsumer<>("dlp-twitter", new SimpleStringSchema(), properties);

        DataStream<String> streamJobs = env.addSource(kafkaSource);

        KeyedStream<Job, String> readJobs = streamJobs
                .map(new Job.ConvertFunction())
                .filter(map -> map != null)
                .keyBy(job -> job.description);


        DataStream<JobExtended> averageSalaryJobs =  readJobs
                .window(SlidingProcessingTimeWindows.of(Time.hours(1), Time.minutes(1)))
                .process(new AverageSalary());



        // Escribir en topic kafka
        averageSalaryJobs.addSink(new FlinkKafkaProducer<>(
                "topic-extendedJob",
                new KafkaSerializationSchema<JobExtended>() {

                    @Override
                    public ProducerRecord<byte[], byte[]> serialize(JobExtended jobExt, @Nullable Long aLong) {

                        ObjectMapper objectMapper = new ObjectMapper();
                        if (jobExt == null) {
                            return null;
                        }
                        try {
                            byte[] jobBytes = objectMapper.writeValueAsBytes(jobExt);

                            return new ProducerRecord<byte[], byte[]>("topic-extendedJob", jobBytes);

                        } catch (IOException | RuntimeException e) {
                            throw new SerializationException("Error serializing value", e);
                        }
                    }
                }, properties, FlinkKafkaProducer.Semantic.EXACTLY_ONCE));

        averageSalaryJobs.print();
        env.execute("Job Average Calculation");

    }



    public static class AverageSalary extends ProcessWindowFunction<Job, JobExtended, String, TimeWindow> {

        @Override
        public void process(String key, Context context, Iterable<Job> jobs, Collector<JobExtended> out) throws Exception {

            try {
                Float total = 0F;
                int count = 0;
                Float stockValue = 0F;
                String desc = "";
                for (Job j : jobs){
                    desc = j.description;
                    total += j.salary;
                    count += 1;
                    stockValue = total/count;
                }

                out.collect(new JobExtended(desc, total, count, stockValue));

            } catch (Exception e) {
                System.out.println("Excepcion: " + e);
                throw new Exception();
            }
        }
    }


}