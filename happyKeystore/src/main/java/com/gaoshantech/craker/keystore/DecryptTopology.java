package com.gaoshantech.craker.keystore;

import com.gaoshantech.craker.utils.CommonUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.kafka.bolt.KafkaBolt;
import org.apache.storm.kafka.bolt.mapper.TupleToKafkaMapper;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Tuple;

import java.util.Properties;

public class DecryptTopology {
    private static final String KAFKA_HOST = CommonUtils.readProperty("KAFKA_HOST");
    private static final String KAFKA_PORT = CommonUtils.readProperty("KAFKA_PORT");

    public static void main(String[] args) throws Exception {
        // 组建拓扑，并使用流分组
        TopologyBuilder builder = new TopologyBuilder();
        //配置
        Config cfg = new Config();
        cfg.setMaxSpoutPending(1000);
        cfg.setDebug(false);
        //创建spout
        KafkaSpoutConfig<String, String> kafkaSpoutConfig = KafkaSpoutConfig.builder(KAFKA_HOST + ":" + KAFKA_PORT, "DecryptTask")
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
        KafkaBolt<String, String> kafkaBolt = new KafkaBolt<String, String>().withTopicSelector("DecryptResult").withTupleToKafkaMapper(new TupleToKafkaMapper<String, String>() {
            @Override
            public String getKeyFromTuple(Tuple tuple) {
                return tuple.getStringByField("address");
            }

            @Override
            public String getMessageFromTuple(Tuple tuple) {
                return tuple.getStringByField("result");
            }
        }).withProducerProperties(properties);
        // 设置storm数据源为kafka整合storm的kafkaSpout
        builder.setSpout("KafkaDecryptTaskSpout", kafkaSpout, 1);
        //数据流向，流向dataBolt进行处理
        if(args.length >= 1) {
            builder.setBolt("DecryptCheckBolt", new DecryptCheckBolt(), Integer.parseInt(args[0])).shuffleGrouping("KafkaDecryptTaskSpout");
            builder.setBolt("DecryptReportBolt", kafkaBolt).shuffleGrouping("DecryptCheckBolt");
            //提交拓扑图
            StormSubmitter.submitTopology("password-decrypt-topo", cfg, builder.createTopology());
        }
        else{
            builder.setBolt("DecryptCheckBolt", new DecryptCheckBolt(), 1).shuffleGrouping("KafkaDecryptTaskSpout");
            builder.setBolt("DecryptReportBolt", kafkaBolt).shuffleGrouping("DecryptCheckBolt");
            //提交拓扑图
            LocalCluster localCluster = new LocalCluster();
            localCluster.submitTopology("password-decrypt-topo", cfg, builder.createTopology());
        }
    }
}
