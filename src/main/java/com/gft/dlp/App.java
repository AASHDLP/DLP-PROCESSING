package com.gft.dlp;

import com.gft.dlp.producer.TwitterKafkaProducer;

/**
 * Producer running
 */
public class App {
    public static void main(String[] args) throws Exception {

        // Mostramos por pantalla que hemos iniciado el productor de kafka
        System.out.println("Start producer dlp-twitter");
        TwitterKafkaProducer producer = new TwitterKafkaProducer();
        producer.run();

    }
}
