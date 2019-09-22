package com.gaoshantech.craker.test;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;


/**
 * kafka消费者类
 * @author lvfang
 *
 */
public class PasswordTestingCusumer extends Thread {

    private String topic;//主题

    private long i;

    public PasswordTestingCusumer(String topic){
        super();
        this.topic = topic;
    }

    //创建消费者
    private Consumer<String, String> createConsumer(){
        Properties properties = new Properties();
        //zkInfo
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"127.0.0.1:9092");
        //必须要使用别的组名称， 如果生产者和消费者都在同一组，则不能访问同一组内的topic数据
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "group2");
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<String, String>(properties);
    }

    @Override
    public void run() {
        //创建消费者
        Consumer<String, String> consumer = createConsumer();
        //创建一个获取消息的消息流
        consumer.subscribe(Collections.singletonList(topic));
        try {
            do {
                ConsumerRecords<String, String> poll = consumer.poll(Duration.ofMillis(0));
                if(poll == null) {
                    Thread.sleep(1000);
                    continue;
                }
                //循环打印
                for (ConsumerRecord<String, String> record : poll) {
                    String message = record.value();
                    i++;
                    System.out.println("接收到  " + i + " 条消息: "+ message);
                }
            } while (true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 使用kafka集群中创建好的主题 test
        new PasswordTestingCusumer("PasswordTesting").start();;
    }
}