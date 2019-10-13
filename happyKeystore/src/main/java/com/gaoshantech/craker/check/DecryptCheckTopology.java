package com.gaoshantech.craker.check;

import com.gaoshantech.craker.keystore.DecryptCheckBolt;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.redis.bolt.RedisStoreBolt;
import org.apache.storm.redis.common.config.JedisPoolConfig;
import org.apache.storm.topology.TopologyBuilder;

import java.util.Properties;

public class DecryptCheckTopology {
    private static final String KAFKA_HOST = "127.0.0.1";
    private static final String KAFKA_PORT = "9092";

    public static void main(String[] args) throws Exception {
        // 组建拓扑，并使用流分组
        TopologyBuilder builder = new TopologyBuilder();
        //配置
        Config cfg = new Config();
        cfg.setMaxSpoutPending(1000);
        cfg.setDebug(false);
        //创建spout
        KafkaSpoutConfig<String, String> kafkaSpoutConfig = KafkaSpoutConfig.builder(KAFKA_HOST + ":" + KAFKA_PORT, "DecryptResult")
                .setProp(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000)
                .setProp(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000)
                .setProp(ConsumerConfig.GROUP_ID_CONFIG, "group1")
                .setProp(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)
                .setProp(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName())
                .setProp(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName())
                .setProp(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
                .setOffsetCommitPeriodMs(1000)//控制spout多久向kafka提交一次offset
                .setMaxUncommittedOffsets(1)
                .setProcessingGuarantee(KafkaSpoutConfig.ProcessingGuarantee.AT_LEAST_ONCE)
                .build();
        //整合kafkaSpout
        KafkaSpout<String, String> kafkaSpout = new KafkaSpout<String, String>(kafkaSpoutConfig);
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_HOST + ":" + KAFKA_PORT);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // 设置storm数据源为kafka整合storm的kafkaSpout
        builder.setSpout("KafkaDecryptCheckSpout", kafkaSpout, 1);
        JedisPoolConfig poolConfig = new JedisPoolConfig.Builder()
                .setHost("192.168.100.5").setPort(6379).build();
        RedisStoreBolt redisStoreBolt = new RedisStoreBolt(poolConfig, new PasswordStoreMapper());
        String keystore = "{\"address\":\"4vHQ8D1js97rx2argrNnJ6fkTT66hfsFMsxF9jTKBFozdLksh3aMN2Yr4KXgnem9D2QRRhoa4ESysWfmzSNsgAWi\",\"tk\":\"4vHQ8D1js97rx2argrNnJ6fkTT66hfsFMsxF9jTKBFozWB6rExrL6aB9ELZo1CjzbYo8qbY7GgNH3F9ZY4tJyzY7\",\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"ff4889ec39c75f2bd1977a674d00932d4b113a67ebb7b6baea2fe4d2d3015383\",\"cipherparams\":{\"iv\":\"93c2e04a7137d2bc53c2a739c3fd08db\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":262144,\"p\":1,\"r\":8,\"salt\":\"b3206b441f0567460f38caec75227ee7d2809c72ef544912a1330fbf178d4b8e\"},\"mac\":\"d680f6e43af7fe18946b62dcc0c3c7731799db56ce62bcc20314b6b736e447de\"},\"id\":\"8ac00755-18d6-4a2f-bc08-d43dc83e830d\",\"version\":3}";
        //数据流向，流向dataBolt进行处理
        if(args.length >= 3) {
            builder.setBolt("PasswordCheckBolt", new PasswordCheckBolt(keystore), 1).shuffleGrouping("KafkaDecryptTaskSpout");
            builder.setBolt("PasswordReportBolt", redisStoreBolt).shuffleGrouping("PasswordCheckBolt");
            //提交拓扑图
            StormSubmitter.submitTopology("password-decrypt-topo", cfg, builder.createTopology());
        }
        else{
            builder.setBolt("DecryptCheckBolt", new PasswordCheckBolt(keystore), 1).shuffleGrouping("KafkaDecryptTaskSpout");
            builder.setBolt("PasswordReportBolt", redisStoreBolt).shuffleGrouping("DecryptCheckBolt");
            //提交拓扑图
            LocalCluster localCluster = new LocalCluster();
            localCluster.submitTopology("password-decrypt-topo", cfg, builder.createTopology());
        }
    }
}
