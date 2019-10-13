package com.gaoshantech.craker.check;

import com.alibaba.fastjson.JSON;
import com.gaoshantech.craker.decrypt.DecryptResult;
import com.gaoshantech.craker.utils.CommonUtils;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.WalletExtend;
import org.web3j.crypto.WalletFile;

import java.util.Map;

public class PasswordCheckBolt implements IRichBolt {
    private String keystore;
    WalletFile walletFile;

    public PasswordCheckBolt(String keystore) {
        this.keystore = keystore;
    }
    private OutputCollector collector;
    @Override
    public void prepare(Map<String, Object> map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
        this.walletFile = CommonUtils.deserializeKeyStore(keystore);
    }

    @Override
    public void execute(Tuple tuple) {
        String address = tuple.getStringByField("address");
        DecryptResult result = JSON.parseObject(tuple.getStringByField("result"), DecryptResult.class);
        try {
            WalletExtend.decryptCheck(walletFile, result.getDerivedKey());
        } catch (CipherException e) {
            return;
        }
        this.collector.emit(new Values(address, JSON.toJSONString(result.getPassword())));
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("address", "result"));
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return null;
    }
}
