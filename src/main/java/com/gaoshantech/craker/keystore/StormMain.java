package com.gaoshantech.craker.keystore;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.redis.bolt.RedisStoreBolt;
import org.apache.storm.redis.common.config.JedisPoolConfig;
import org.apache.storm.topology.TopologyBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class StormMain {
    public static void main(String[] args) throws Exception {
        JedisPoolConfig poolConfig = new JedisPoolConfig.Builder()
                .setHost("192.168.100.5").setPort(6379).build();
        RedisStoreBolt redisStoreBolt = new RedisStoreBolt(poolConfig, new PasswordStoreMapper());
        // 组建拓扑，并使用流分组
        TopologyBuilder builder = new TopologyBuilder();
        //配置
        Config cfg = new Config();
        cfg.setMaxSpoutPending(1000);
        cfg.setDebug(false);
        if(args.length >= 3) {
            cfg.setNumWorkers(Integer.parseInt(args[2]));
            String keystore = Files.lines(Paths.get(args[0]), StandardCharsets.UTF_8).collect(Collectors.joining("\n"));
            builder.setSpout("PasswordSpout", new PasswordSpout(args[1]), 1);
            builder.setBolt("PasswordCheckBolt", new PasswordCheckBolt(keystore), 8).shuffleGrouping("PasswordSpout");
            builder.setBolt("PasswordReportBolt", redisStoreBolt).shuffleGrouping("PasswordCheckBolt");
            //提交拓扑图
            StormSubmitter.submitTopology("keystore-topo", cfg, builder.createTopology());
        }
        else{
            String keystore = "{\"address\":\"4vHQ8D1js97rx2argrNnJ6fkTT66hfsFMsxF9jTKBFozdLksh3aMN2Yr4KXgnem9D2QRRhoa4ESysWfmzSNsgAWi\",\"tk\":\"4vHQ8D1js97rx2argrNnJ6fkTT66hfsFMsxF9jTKBFozWB6rExrL6aB9ELZo1CjzbYo8qbY7GgNH3F9ZY4tJyzY7\",\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"ff4889ec39c75f2bd1977a674d00932d4b113a67ebb7b6baea2fe4d2d3015383\",\"cipherparams\":{\"iv\":\"93c2e04a7137d2bc53c2a739c3fd08db\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":262144,\"p\":1,\"r\":8,\"salt\":\"b3206b441f0567460f38caec75227ee7d2809c72ef544912a1330fbf178d4b8e\"},\"mac\":\"d680f6e43af7fe18946b62dcc0c3c7731799db56ce62bcc20314b6b736e447de\"},\"id\":\"8ac00755-18d6-4a2f-bc08-d43dc83e830d\",\"version\":3}";
            builder.setSpout("PasswordSpout", new PasswordSpout("Qq%d%d%d%d%d%d"), 1);
            builder.setBolt("PasswordCheckBolt", new PasswordCheckBolt(keystore), 8).shuffleGrouping("PasswordSpout");
            builder.setBolt("PasswordReportBolt", redisStoreBolt).shuffleGrouping("PasswordCheckBolt");
            //提交拓扑图
            LocalCluster localCluster = new LocalCluster();
            localCluster.submitTopology("keystore-topo", cfg, builder.createTopology());
        }
    }
}
