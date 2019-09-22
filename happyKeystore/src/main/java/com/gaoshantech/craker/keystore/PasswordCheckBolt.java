package com.gaoshantech.craker.keystore;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;

import java.io.IOException;
import java.util.Map;

public class PasswordCheckBolt implements IRichBolt {
    private OutputCollector collector;
    private String keystore;
    WalletFile walletFile;
    public PasswordCheckBolt(String keystore) throws IOException {
        this.keystore = keystore;
    }
    @Override
    public void prepare(Map<String, Object> map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            walletFile = objectMapper.readValue(keystore, WalletFile.class);
        } catch (IOException e) {
        }
    }

    @Override
    public void execute(Tuple tuple) {
        String password = tuple.getStringByField("password");
        while (true) {
            try {
                Wallet.decrypt(password, walletFile);
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
        this.collector.emit(new Values(password, true));
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("password", "state"));
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return null;
    }
}
