package com.gaoshantech.craker.test;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * kafka生产者类
 * @author lvfang
 *
 */
public class KafkaProduce extends Thread {

    // 主题
    private String topic;
    // 数据源容器
    private static final Map<String, String> map = new HashMap<String, String>();
    final Random random = new Random();

    static {
        map.put("0", "java");
        map.put("1", "php");
        map.put("2", "groovy");
        map.put("3", "python");
        map.put("4", "ruby");
    }

    public KafkaProduce(String topic){
        super();
        this.topic = topic;
    }

    //创建生产者
    private Producer<String, String> createProducer(){
        Properties properties = new Properties();
        //kafka单节点
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<String, String>(properties);
    }

    @Override
    public void run() {
        //创建生产者
        Producer<String, String> producer = createProducer();
        //循环发送消息到kafka
        while(true){
            Future<RecordMetadata> send = producer.send(new ProducerRecord<String, String>(topic, map.get(String.valueOf(random.nextInt(5)))));
            try {
                RecordMetadata metadata = send.get();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        // 使用kafka集群中创建好的主题 test
        new KafkaProduce("ARF").start();
    }
}