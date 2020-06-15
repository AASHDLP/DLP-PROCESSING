# DLP-PROCESSING

## Instalación:
Instalar cliente local de Hadoop en Windows. Para ello debemos descomprimir la carpeta Hadoop y copiarla en el directorio C de nuestro local, de manera que podamos acceder a la carpeta bin de este modo: C:\Hadoop\bin

## Proyecto:
Desde IntelliJ, abrir un nuevo proyecto a partir del pom.xml adjunto. Este proyecto, contiene unas librerías que hay que añadir manualmente, se encuentran en la ruta twitter-kafka\src\main\java\lib. Para añadirlas, al abrir el proyecto, debemos situarnos sobre ellas en el explorador de proyecto y seleccionar la opción de añadirlas. 

Las clases que se tienen que ejecutar son las siguientes, hacerlo en el orden indicado.

1.	twitter-kafka\src\main\java\com\gft\dlp\App.java Esta clase lanza el producer Kafka que se encarga de leer los tweets y escribirlos en el topic dlp-twitter.
2.	twitter-kafka\src\main\java\com\gft\dlp\consumer\JobConsumerToTopic.java Esta clase consume del topic anterior, y en ventanas temporales de 1 hora calcula la media del salario para cada tipo de Job recibido en Twitter. Esta información se alamcena en el topic Kafka topic-extendedJob. 
3.	twitter-kafka\src\main\java\com\gft\dlp\consumer\ConsumerTwitterToHDFS.java Al lanzar esta clase se vuelca el contenido del topic dlp-twitter en la siguiente ruta HDFS: kafka/topictwitter/
4.	twitter-kafka\src\main\java\com\gft\dlp\consumer\ConsumerExtendedToHDFS.java Por último se ejecuta esta clase que guarda el contenido del topic topic-extendedJob en la ruta HDFS: kafka/extendedcast

## Configuración interpreter:
Adicionalmente sería necesario configurar el nuevo interprete hive y algunos pasos adicionales para que el interpreter pueda lanzar comandos hadoop  (que permite muchos más comandos que la versión existente en zeppelin de hadoop). 

1.	Ejecutables de hadoop: Están en otros pods como el de hive. Por tanto, si hacemos lo mismo que con el driver y copiamos la carpeta al compartido, ya deberíamos tener acceso. En vuestro pc copiar desde la carpeta de hive de arriba a la carpeta <donde_lanzo_docker-compose>/zeppelin/shared.
En el pod del hive server:
- PS C:\Users\fola\Desktop\GFT\dlp\Sessions\ING-Ingestion\Resources\zeppelin> docker exec -u root -t -i hiveserver /bin/bash
- root@ca5953ef9e5c:/opt# cd /opt/hadoop-2.7.4
- root@ca5953ef9e5c:/opt/hadoop-2.7.4# mkdir -p /tmp/shared/hadoop-2.7.4
- root@ca5953ef9e5c:/opt/hadoop-2.7.4# cp -R * /tmp/shared/hadoop-2.7.4

2.	Hive: Una vez levantéis el nuevo Docker compose, tendréis que logaros en el pod de hiveserver:
-	docker exec -u root -t -i hiveserver /bin/bash
- root@ca5953ef9e5c:/opt # cd /opt/hive/jdbc
- root@ca5953ef9e5c:/opt/hive/jdbc# cp hive-jdbc-2.3.2-standalone.jar /tmp/shared/
- Una vez hecho eso, en vuestro PC debería haberos aparecido una carpeta hive, desde donde hayáis lanzado el Docker-compose. Dentro deberíais tener el driver que habéis compartido. En Windows, copiais ese driver al volume mount de zeppelin, que según el Docker compose está donde hayáis lanzado el Docker-compose/zeppelin/shared. Una vez copiado, os logais em el pod de zeppelin, y deberíais tener el driver en /tmp/shared:
- docker exec -u root -t -i zeppelin /bin/bash
 
Creáis el enlace simbólico y comprobais que esté:
-	ln -s  /tmp/shared/hive-jdbc-2.3.2-standalone.jar interpreter/jdbc/hive-jdbc-2.3.2-standalone.jar
-	ls -l interpreter/jdbc/hive*
 
Después os vais a zeppelin: http://localhost:9999/#/ Y creáis el interprete con los siguientes valores:
-	Interpreter name: hive
-	Interpreter group: jdbc
-	default.driver: org.apache.hive.jdbc.HiveDriver
-	default.url:  jdbc:hive2://hiveserver:10000
-	default.user: hive
-	split.queries: true 

El resto de valores podéis dejarlos por defecto. Le dais a Save y debería quedarse el interprete en verde.

## Hue: Creación de vistas y consultas realizadas
Por último, estos son los scripts que hemos ejecutado en Hue para crear las tablas y vistas que nos permiten realizar después el análisis de los datos.

CREATE EXTERNAL TABLE topictwitter (Ingestion_Time String, Job String, Salary String)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION 'hdfs://namenode:9000/kafka/topictwitter/'; 
 
CREATE EXTERNAL TABLE extendedcast (Ingestion_Time String, Job String, Salary String, Count String, Mean_Value String)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION 'hdfs://namenode:9000/kafka/extendedcast/'; 
 
CREATE TABLE portfolio (Buytime Int, Job String, Volume Integer); 
 
CREATE VIEW IF NOT EXISTS twittercast
    AS SELECT cast (SUBSTR(Ingestion_Time,9,4) as int) as twittertime, job, salary
  FROM topictwitter;

CREATE VIEW IF NOT EXISTS extendedcast
    AS SELECT cast (SUBSTR(Ingestion_Time,9,4) as int) as extendedtime, job, salary, count, mean_value
  FROM extendedcast;
 
CREATE VIEW IF NOT EXISTS extendedfilt
    AS SELECT TC.twittertime, TC.job, MIN(extendedtime) as extendedtime
  FROM extendedcast EC 
 INNER JOIN twittercast TC
    ON EC.Job = TC.Job
 WHERE EC.extendedtime > TC.twittertime
 GROUP BY TC.Job, TC.twittertime;
