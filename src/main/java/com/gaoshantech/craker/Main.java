package com.gaoshantech.craker;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class Main {
    private static String keystore = "";
    public static void main(String[] args) throws IOException {
        AtomicLong progress = new AtomicLong(0);
        System.out.println(new Date());
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        WalletFile walletFile = objectMapper.readValue(keystore, WalletFile.class);
        if(args.length > 1){
            int min = Integer.parseInt(args[0]);
            int max = Integer.parseInt(args[1]);
            System.out.println("Range: [" + min + "," + max + ")");
            String passwordString = IntStream.range(min, max).mapToObj(i -> "Qq" + StringUtils.leftPad(String.valueOf(i), 6, "0")).parallel().filter(
                    password -> computeLogic(progress, min, max, walletFile, password)
            ).findFirst().orElse("");
            System.out.println("PASSWORD: " + passwordString);
        }
        if(args.length == 1){
            List<String> passwords = Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8);
            String passwordString = passwords.parallelStream().filter(password -> computeLogic(progress, 0, passwords.size(), walletFile, password)).findFirst().orElse("");
            System.out.println("PASSWORD: " + passwordString);
        }
    }

    private static boolean computeLogic(AtomicLong progress, int min, int max, WalletFile walletFile, String password) {
        while (true) {
            try {
                long current = progress.incrementAndGet();
                try {
                    System.out.println("Testing: " + password);
                    Wallet.decrypt(password, walletFile);
                    return true;
                } catch (CipherException ignored) {
                    if (current % 100 == 0) {
                        System.out.println(new Date());
                        System.out.println("Progress: " + (current / ((max - min) / 100.0)) + "%");
                    }
                }
                return false;
            } catch (OutOfMemoryError oom){
                progress.decrementAndGet();
                System.out.println("OOM");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
