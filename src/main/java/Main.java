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
    private static String keystore = "{\"address\":\"5G17Kx5ivX8dM7pJz19o2KHnPf2iKaitHdCxgdXeeDKJVMbynyjs95Qh8cjvKVstx1vC2cAhTXF5FxtcCUV28uiX\",\"tk\":\"5G17Kx5ivX8dM7pJz19o2KHnPf2iKaitHdCxgdXeeDKJmGi22ipTu3K3aZD2gN1K1UDzEZrUGeo28kcQ2djsb6K9\",\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"15e571216de7dfb26822a546d7df345c66b1b7f7c8d431cf6def26c5f2327f7d\",\"cipherparams\":{\"iv\":\"ef68671d2ef4bea2297c89b6ea88e066\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":262144,\"p\":1,\"r\":8,\"salt\":\"78d1c45a4024c6e829bc05363b91aa0692a0e31161b2b8581abe01a59a68c364\"},\"mac\":\"b5fbc5d0d6c1004713868e2f24afe0829391dd1426736028ec8aadfdd8566b3b\"},\"id\":\"1fc9ec84-f247-4b10-bac7-e7098b03fa75\",\"version\":3}";
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
