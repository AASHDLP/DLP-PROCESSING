package com.gft.dlp.producer.callback;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;

public class BasicCallback implements Callback {
    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        //  Si no se ha producido ninguna incidencia
        if (exception == null) {
            // Mostramos la posición del mensaje el tópico y la partición donde se ha guardado
            System.out.printf("Message with offset %d acknowledged by partition %d\n",
                    metadata.offset(), metadata.partition());
        } else {
            // Mostramos el mensaje de la excepción.
            System.out.println(exception.getMessage());
        }
    }
}
