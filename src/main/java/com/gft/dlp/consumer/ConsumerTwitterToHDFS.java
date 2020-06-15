package com.gft.dlp.consumer;

import com.gft.dlp.model.Job;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

public class ConsumerTwitterToHDFS {
    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "com.gft.dlp.serializer.JobDeserializer");
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "consumer-twitter");

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());

        //Toda la cola
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        final StreamsBuilder builder = new StreamsBuilder();

        // Consumir primera cola
        KafkaConsumer<String, Job> kafkaConsumer = new KafkaConsumer<String, Job>(properties);

        //Subscribe
        kafkaConsumer.subscribe(Arrays.asList("dlp-twitter"));

        try{
            while (true){
                ConsumerRecords<String, Job> records = kafkaConsumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Job> record: records){
                    Job myJob = record.value();
                    String myKey = record.key();
                    if(myJob != null && myKey != null) {

                        String fileContent= myKey + "," + myJob.getDescription() + "," + myJob.getSalary() + "\n";

                        String hdfsuri = "hdfs://localhost:9000";

                        String path="/kafka/topictwitter";
                        String fileName="out.csv";

                        // ====== Init HDFS File System Object
                        Configuration conf = new Configuration();
                        // Set FileSystem URI
                        conf.set("fs.defaultFS", hdfsuri);
                        //conf.set("dfs.replication", "1");
                        // Because of Maven
                        conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
                        conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
                        // Set HADOOP user
                        System.setProperty("HADOOP_USER_NAME", "hdfs");
                        System.setProperty("hadoop.home.dir", "C:\\Hadoop");

                        FileSystem fs = null;
                        FSDataOutputStream outputStream = null;

                        try {
                        //Get the filesystem - HDFS
                        fs = FileSystem.get(URI.create(hdfsuri), conf);

                        //==== Create folder if not exists
                        Path workingDir=fs.getWorkingDirectory();
                        Path newFolderPath= new Path(path);
                        if(!fs.exists(newFolderPath)) {
                            // Create new Directory
                            fs.mkdirs(newFolderPath);
                        }

                        //==== Write file
                        //Create a path
                        Path hdfswritepath = new Path(newFolderPath + "/" + fileName);

                        //Init output stream
                        if(!fs.exists(hdfswritepath)){
                            outputStream = fs.create(hdfswritepath);
                        }
                        else{
                            outputStream = fs.append(hdfswritepath);
                        }
                        //Cassical output stream usage
                        outputStream.writeBytes(fileContent);
                        outputStream.close();

                        fs.close();

                        } catch (Exception e){
                            try{
                                outputStream.close();
                                fs.close();
                            } catch (Exception exec){

                            }
                        }

                        System.out.println(String.format("Topic - %s, Partition - %d, Key: %s, Value: %s, Out: %s", record.topic(), record.partition(), myKey.toString(), myJob.toString(), fileContent));

                        }
                }
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }finally {
            kafkaConsumer.close();
        }
    }

}
