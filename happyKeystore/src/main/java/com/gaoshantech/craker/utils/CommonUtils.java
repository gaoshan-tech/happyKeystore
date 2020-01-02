package com.gaoshantech.craker.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.web3j.crypto.WalletFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CommonUtils {
    private static Properties properties = new Properties();

    public static WalletFile deserializeKeyStore(String keystore) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return objectMapper.readValue(keystore, WalletFile.class);
        } catch (IOException ignored) {
        }
        return null;
    }
    static {
        //方法一
            InputStream inputStream = Object.class.getResourceAsStream("/code.properties");
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
    public static String readProperty(String key){
        return properties.getProperty(key);
    }
}
