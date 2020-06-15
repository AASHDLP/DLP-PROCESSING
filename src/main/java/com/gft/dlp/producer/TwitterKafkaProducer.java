package com.gft.dlp.producer;

import com.gft.dlp.config.KafkaConfiguration;
import com.gft.dlp.config.TwitterConfiguration;
import com.gft.dlp.model.Job;
import com.gft.dlp.model.Tweet;
import com.gft.dlp.model.TweetExtended;
import com.gft.dlp.producer.callback.BasicCallback;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

/*
  Esta clase se encargará de consumir de Twitter por la API y publicar en el tópico Kafka
 */
public class TwitterKafkaProducer {
    private Client client;
    private BlockingQueue<String> queue;
    private Gson gson;
    private Callback callback;

    public TwitterKafkaProducer() {

        // Configuramos la autenticación necesaria para conectarmos a la API de Twiter
        Authentication authentication = new OAuth1(
                TwitterConfiguration.CONSUMER_KEY,
                TwitterConfiguration.CONSUMER_SECRET,
                TwitterConfiguration.ACCESS_TOKEN,
                TwitterConfiguration.TOKEN_SECRET);

        // Obtenemos los mensajes de twitter via API del usuario dlpexercisepro1
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();

        // 1258295785260756992 is dlpexercisepro1
        List<Long> IduserTweeter = new ArrayList<Long>();
        IduserTweeter.add(1258295785260756992L);
        endpoint.followings(IduserTweeter);

        //endpoint.trackTerms(Collections.singletonList(TwitterConfiguration.HASHTAG));

        // Establecemos un máximo de 10.000 mensajes a leer desde Twitter.
        // Como todo el ejercicio lo haremos en realtime es un número más que suficiente puesto
        // que para las pruebas ingestaremos datos de 2 o 3 días como mucho.
        queue = new LinkedBlockingQueue<>(10000);

        // Inicializamos los 3 componentes client, gson y callback
        client = new ClientBuilder()
                .hosts(Constants.STREAM_HOST)
                .authentication(authentication)
                .endpoint(endpoint)
                .processor(new StringDelimitedProcessor(queue))
                .build();
        gson = new Gson();
        callback = new BasicCallback();
    }

    /*
      El fichero de propiedades usado para un productor kafka que va insertar en el tópico
     */
    private Producer<String, Job> getProducer() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfiguration.SERVERS);
        properties.put(ProducerConfig.ACKS_CONFIG, "1");
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 500);
        properties.put(ProducerConfig.RETRIES_CONFIG, 0);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "com.gft.dlp.serializer.JobSerializer");

        return new KafkaProducer<>(properties);
    }

    /*
     Función que hará todo el trabajo:
     - Obtener tweet
     - Procesar cada línea del tweet
     - Por cada línea con información relevante la publicaremos en el tópico
     */
    public void run() {

        ProducerRecord<String, Job> record;
        client.connect();
        // Si hemos podido establecer conexión con twitter y tenemos las propiedades kafka
        // para ser publicar mensajes en el tópico
        try (Producer<String, Job> producer = getProducer()) {
            while (true) {
                // Declaramos variables que vamos a usar
                String keyi = "0";
                String [] sLines;
                String [] sJobs;
                String pattern;
                int i;
                SimpleDateFormat formatter=new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);

                // Obtenemos el tweet y los campos que vamos a usar Id, mensaje y fecha creación
                String sTwitter = queue.take();
                System.out.printf("tweet %s\n", sTwitter);

                // Si el Tweet viene truncado recuperamos el Json extended
                Tweet tweet = gson.fromJson(sTwitter, Tweet.class);
                String msg;
                if (tweet.isTruncated())
                {
                    System.out.println("TRUNCATED");
                    TweetExtended tweet_ext = gson.fromJson(sTwitter, TweetExtended.class);
                    JsonObject jText = tweet_ext.getExtended_tweet();
                    msg = jText.get("full_text").toString();
                    msg = msg.replace("\"", "");
                    pattern = Pattern.quote("\\" + "n");

                   // byte[] ptext = value.getBytes(UTF_8) ;
                   // msg = new String(ptext, UTF_8);
                }else
                {
                    msg = tweet.getText();
                    pattern = Pattern.quote("\n");
                }
                long key = tweet.getId();

                String created_at = tweet.getCreated_at();

                // Parseamos la fecha para transformarla a Long
                Date date1=formatter.parse(created_at);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date1);
                String year       = String.valueOf(calendar.get(Calendar.YEAR));
                String month      = String.format("%02d", calendar.get(Calendar.MONTH)+1);
                String dayOfMonth = String.format("%02d",calendar.get(Calendar.DAY_OF_MONTH));
                String hourOfDay  = String.format("%02d",calendar.get(Calendar.HOUR_OF_DAY)); // 24 hour clock
                String minute     = String.format("%02d",calendar.get(Calendar.MINUTE));

                // Para depurar vamos sacando por consola datos que estamos procesando.
                System.out.printf("tweet id %d\n", key);
                System.out.printf("tweet msg %s\n", msg);

                // Si tenemos datos en tweet
                if (msg != null) {
                    // Partimos el mensaje por líneas
                    //sLines = msg.split("(\n|\\n|\\\n)");
                    //sLines = StringUtils.split(msg, "€\n");
                    //sLines =msg.split("\n");
                    //sLines = msg.split("[\\r\\n]+");
                    sLines = msg.split(pattern);

                    i=0;
                    System.out.printf("tweet 1msg %s\n", sLines[0]);
                    // Por cada línea del mensaje
                    while(sLines.length > i){
                        // Sacamos el trabajo y el salario como Strings
                        sJobs = sLines[i].split(" Salary: ");
                        if (sJobs.length == 2)
                        {
                            // Si la línea tenía trabajo y salario preparamos la salida para publicarla al tópico
                            keyi = year + month + dayOfMonth + hourOfDay + minute + String.format("%02d", i);
                            float fSalary = Float.valueOf(sJobs[1].split("€")[0]);
                            Job jJob = new Job (sJobs[0], fSalary);
                            // Preparamos el registro para el tópico con:
                            // key<String>: YYYYMMDDHHMM concatenado a un contador de 00 a 99
                            // value<Job>: {description, salary}
                            record = new ProducerRecord<>(KafkaConfiguration.TOPIC, keyi, jJob);

                            // Mostramos por consola los valores a pulbicar al tópico.
                            System.out.printf("kafka id %s\n", keyi);
                            System.out.printf("kafka msg %s\n", jJob.toString());

                            // Publicamos al tópico
                            producer.send(record, callback);
                        }
                        i++;
                    }
                }
                
            }
        } catch (InterruptedException | ParseException e) {
            e.printStackTrace();
        } finally {
            client.stop();
        }
    }
}
