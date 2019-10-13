package com.gaoshantech.craker.keystore;

import com.alibaba.fastjson.JSON;
import com.gaoshantech.craker.decrypt.DecryptParam;
import com.gaoshantech.craker.decrypt.DecryptResult;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.WalletExtend;

import java.util.Map;

public class DecryptCheckBolt implements IRichBolt {
    private OutputCollector collector;
    @Override
    public void prepare(Map<String, Object> map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
    }

    @Override
    public void execute(Tuple tuple) {
        String address = tuple.getStringByField("address");
        DecryptParam decryptParam = new DecryptParam(tuple.getStringByField("decryptParam"));
        byte[] result;
        while (true) {
            try {
                result = WalletExtend.decryptPart(decryptParam);
                break;
            } catch (CipherException e) {
                return;
            } catch (OutOfMemoryError oom) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
        this.collector.emit(new Values(address, JSON.toJSONString(new DecryptResult(result, decryptParam.getPassword()))));
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
