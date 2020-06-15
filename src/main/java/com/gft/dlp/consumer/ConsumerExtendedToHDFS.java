package com.gft.dlp.consumer;

import com.gft.dlp.model.JobExtended;
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
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

public class ConsumerExtendedToHDFS {
    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "com.gft.dlp.serializer.JobExtendedDeserializer");
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "consumer-extended");

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());

        //Toda la cola
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        final StreamsBuilder builder = new StreamsBuilder();

        // Consumir primera cola
        KafkaConsumer<String, JobExtended> kafkaConsumer = new KafkaConsumer<String, JobExtended>(properties);

        //Subscribe
        kafkaConsumer.subscribe(Arrays.asList("topic-extendedJob"));

        try{
            while (true){
                ConsumerRecords<String, JobExtended> records = kafkaConsumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, JobExtended> record: records){
                    JobExtended myJob = record.value();
                    String myKey = convertTime(record.timestamp());
                    if(myJob != null && myKey != null) {

                        String fileContent = myKey + "," + myJob.getDescription() + "," + myJob.getSalary() + "," + myJob.getCount() + "," + myJob.getStockValue() + "\n";


                        String hdfsuri = "hdfs://localhost:9000";

                        String path="/kafka/extendedcast";
                        String fileName="outExtended.csv";

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
                            Path workingDir = fs.getWorkingDirectory();
                            Path newFolderPath = new Path(path);
                            if (!fs.exists(newFolderPath)) {
                                // Create new Directory
                                fs.mkdirs(newFolderPath);
                            }

                            //==== Write file
                            //Create a path
                            Path hdfswritepath = new Path(newFolderPath + "/" + fileName);
                            //Init output stream

                            if (!fs.exists(hdfswritepath)) {
                                outputStream = fs.create(hdfswritepath);
                            } else {
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

    public static String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyyMMddHHmmss");
        return format.format(date);
    }

}
