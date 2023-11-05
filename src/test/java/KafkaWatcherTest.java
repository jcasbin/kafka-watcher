import org.casbin.jcasbin.main.Enforcer;
import org.casbin.watcher.KafkaWatcher;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class KafkaWatcherTest {
    private KafkaWatcher kafkaWatcher;

    @Before
    public void initWatcher(){
        String topic = "jcasbin-topic";

        Map<String,Object> producerProps = new HashMap<>();
        producerProps.put("bootstrap.servers", "localhost:9092");
        producerProps.put("acks", "all");
        producerProps.put("retries", 3);
        producerProps.put("batch.size", 16384);
        producerProps.put("linger.ms", 1);
        producerProps.put("buffer.memory", 33554432);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Map<String,Object> consumerProps = new HashMap<>();
        consumerProps.put("bootstrap.servers", "localhost:9092");
        consumerProps.put("group.id", "my-consumer-group-01");
        consumerProps.put("enable.auto.commit", "true");
        consumerProps.put("auto.commit.interval.ms", "1000");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaWatcher = new KafkaWatcher(producerProps,consumerProps, topic);
        Enforcer enforcer = new Enforcer();
        enforcer.setWatcher(kafkaWatcher);
    }

    @Test
    public void testUpdate() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        kafkaWatcher.setUpdateCallback(()-> {
            countDownLatch.countDown();
            System.out.println("test");
        });
        kafkaWatcher.update();
        countDownLatch.await(1, TimeUnit.MINUTES);
    }

    @Test
    public void testConsumerCallback() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        kafkaWatcher.setUpdateCallback((s)-> {
            countDownLatch.countDown();
            System.out.println("[callback]" + s);
        });
        kafkaWatcher.update();
        countDownLatch.await(1, TimeUnit.MINUTES);
    }
}
