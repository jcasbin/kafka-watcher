package org.casbin.watcher;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.casbin.jcasbin.persist.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class KafkaWatcher implements Watcher{
    private Runnable updateCallback;
    private final String localId;
    private final Map<String,Object> producerProps;
    private final Map<String,Object> consumerProps;
    private final String topic;
    private final Logger log = LoggerFactory.getLogger(KafkaWatcher.class);

    public KafkaWatcher(Map<String,Object> producerProps,Map<String,Object> consumerProps,String topic){
        this.producerProps=producerProps;
        this.consumerProps=consumerProps;
        this.topic=topic;
        this.localId = UUID.randomUUID().toString();
        startSub();
    }

    @Override
    public void setUpdateCallback(Runnable runnable) {
        this.updateCallback=runnable;
    }

    @Override
    public void update() {
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
        ProducerRecord<String,String> record = new ProducerRecord<>(topic, "Casbin policy has a new version from kafka watcher: "+localId);
        Future<RecordMetadata> future = producer.send(record);
        producer.flush();
        RecordMetadata metadata = null;
        try {
            metadata = future.get();
            if(metadata==null){
                log.info("kafka send result is null");
            }
            else{
                log.info("topic: " + metadata.topic() + ", partition: " + metadata.partition() + ", offset: " + metadata.offset());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        finally {
            producer.close();
        }
    }

    public void startSub(){
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(topic));
        Thread subThread = new Thread(()->{
            try{
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                    for (ConsumerRecord<String, String> record : records) {
                        log.info(String.format("topic: %s, partition: %s, offset: %s, key: %s, value: %s",
                                record.topic(), record.partition(), record.offset(), record.key(), record.value()));
                        updateCallback.run();
                    }
                }
            }
            finally {
                consumer.close();
            }
        });
        subThread.setName("subThread");
        subThread.start();
    }
}
