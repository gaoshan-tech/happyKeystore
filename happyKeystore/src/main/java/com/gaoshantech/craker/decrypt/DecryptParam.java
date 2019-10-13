package com.gaoshantech.craker.decrypt;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import kafka.utils.Json;
import org.web3j.crypto.WalletFile;
import org.web3j.utils.Numeric;

public class DecryptParam {
    byte[] mac;
    byte[] iv;
    String kdf;
    Object kdfparams;
    String kdfparamsClassName;
    String password;
    public DecryptParam(String json){
        JSONObject object = JSON.parseObject(json);
        mac = object.getBytes("mac");
        iv = object.getBytes("iv");
        kdfparamsClassName = object.getString("kdfparamsClassName");
        try {
            kdfparams = object.getObject("kdfparams", Class.forName(kdfparamsClassName));
        } catch (ClassNotFoundException e) {
            kdfparams = null;
        }
        kdf = object.getString("kdf");
        this.password = object.getString("password");
    }
    public DecryptParam(WalletFile.Crypto crypto, String password){
        mac = Numeric.hexStringToByteArray(crypto.getMac());
        iv = Numeric.hexStringToByteArray(crypto.getCipherparams().getIv());
        kdfparams = crypto.getKdfparams();
        kdf = crypto.getKdf();
        kdfparamsClassName = kdfparams.getClass().getName();
        this.password = password;
    }

    public byte[] getMac() {
        return mac;
    }

    public byte[] getIv() {
        return iv;
    }

    public Object getKdfparams() {
        return kdfparams;
    }

    public String getPassword() {
        return password;
    }

    public String getKdf() {
        return kdf;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
