package org.web3j.crypto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaoshantech.craker.decrypt.DecryptParam;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.params.KeyParameter;
import org.web3j.utils.Numeric;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class WalletExtend extends Wallet {
    static void validate(WalletFile walletFile) throws CipherException {
        WalletFile.Crypto crypto = walletFile.getCrypto();
        if (walletFile.getVersion() > 3) {
            throw new CipherException("Wallet version is not supported");
        } else if (!crypto.getCipher().equals("aes-128-ctr")) {
            throw new CipherException("Wallet cipher is not supported");
        } else if (!crypto.getKdf().equals("pbkdf2") && !crypto.getKdf().equals("scrypt")) {
            throw new CipherException("KDF type is not supported");
        }
    }
    public static byte[] decryptPart(DecryptParam decryptParam) throws CipherException {
        byte[] derivedKey;
        int c;
        if (decryptParam.getKdfparams() instanceof WalletFile.ScryptKdfParams) {
            WalletFile.ScryptKdfParams scryptKdfParams = (WalletFile.ScryptKdfParams)decryptParam.getKdfparams();
            c = scryptKdfParams.getDklen();
            int n = scryptKdfParams.getN();
            int p = scryptKdfParams.getP();
            int r = scryptKdfParams.getR();
            byte[] salt = Numeric.hexStringToByteArray(scryptKdfParams.getSalt());
            derivedKey = generateDerivedScryptKey(decryptParam.getPassword().getBytes(StandardCharsets.UTF_8), salt, n, r, p, c);
        } else {
            if (!(decryptParam.getKdfparams() instanceof WalletFile.Aes128CtrKdfParams)) {
                throw new CipherException("Unable to deserialize params: " + decryptParam.getKdf());
            }

            WalletFile.Aes128CtrKdfParams aes128CtrKdfParams = (WalletFile.Aes128CtrKdfParams)decryptParam.getKdfparams();
            c = aes128CtrKdfParams.getC();
            String prf = aes128CtrKdfParams.getPrf();
            byte[] salt = Numeric.hexStringToByteArray(aes128CtrKdfParams.getSalt());
            derivedKey = generateAes128CtrDerivedKey(decryptParam.getPassword().getBytes(StandardCharsets.UTF_8), salt, c, prf);
        }
        return derivedKey;
    }
    public static ECKeyPair decryptCheck(WalletFile walletFile, byte[] derivedKey) throws CipherException {
        validate(walletFile);
        WalletFile.Crypto crypto = walletFile.getCrypto();
        byte[] mac = Numeric.hexStringToByteArray(crypto.getMac());
        byte[] iv = Numeric.hexStringToByteArray(crypto.getCipherparams().getIv());
        byte[] cipherText = Numeric.hexStringToByteArray(crypto.getCiphertext());
        byte[] derivedMac = generateMac(derivedKey, cipherText);
        if (!Arrays.equals(derivedMac, mac)) {
            throw new CipherException("Invalid password provided");
        } else {
            byte[] encryptKey = Arrays.copyOfRange(derivedKey, 0, 16);
            byte[] privateKey = performCipherOperation(2, iv, encryptKey, cipherText);
            return ECKeyPair.create(privateKey);
        }
    }

    private static byte[] generateDerivedScryptKey(
            byte[] password, byte[] salt, int n, int r, int p, int dkLen) throws CipherException {
        return SCrypt.generate(password, salt, n, r, p, dkLen);
    }

    private static byte[] generateAes128CtrDerivedKey(
            byte[] password, byte[] salt, int c, String prf) throws CipherException {

        if (!prf.equals("hmac-sha256")) {
            throw new CipherException("Unsupported prf:" + prf);
        }

        // Java 8 supports this, but you have to convert the password to a character array, see
        // http://stackoverflow.com/a/27928435/3211687

        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        gen.init(password, salt, c);
        return ((KeyParameter) gen.generateDerivedParameters(256)).getKey();
    }
    private static byte[] generateMac(byte[] derivedKey, byte[] cipherText) {
        byte[] result = new byte[16 + cipherText.length];

        System.arraycopy(derivedKey, 16, result, 0, 16);
        System.arraycopy(cipherText, 0, result, 16, cipherText.length);

        return Hash.sha3(result);
    }
    private static byte[] performCipherOperation(
            int mode, byte[] iv, byte[] encryptKey, byte[] text) throws CipherException {

        try {
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");

            SecretKeySpec secretKeySpec = new SecretKeySpec(encryptKey, "AES");
            cipher.init(mode, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(text);
        } catch (NoSuchPaddingException
                | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException
                | InvalidKeyException
                | BadPaddingException
                | IllegalBlockSizeException e) {
            throw new CipherException("Error performing cipher operation", e);
        }
    }
    public static void main(String[] args){
        String keystore = "{\"address\":\"4vHQ8D1js97rx2argrNnJ6fkTT66hfsFMsxF9jTKBFozdLksh3aMN2Yr4KXgnem9D2QRRhoa4ESysWfmzSNsgAWi\",\"tk\":\"4vHQ8D1js97rx2argrNnJ6fkTT66hfsFMsxF9jTKBFozWB6rExrL6aB9ELZo1CjzbYo8qbY7GgNH3F9ZY4tJyzY7\",\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"ff4889ec39c75f2bd1977a674d00932d4b113a67ebb7b6baea2fe4d2d3015383\",\"cipherparams\":{\"iv\":\"93c2e04a7137d2bc53c2a739c3fd08db\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":262144,\"p\":1,\"r\":8,\"salt\":\"b3206b441f0567460f38caec75227ee7d2809c72ef544912a1330fbf178d4b8e\"},\"mac\":\"d680f6e43af7fe18946b62dcc0c3c7731799db56ce62bcc20314b6b736e447de\"},\"id\":\"8ac00755-18d6-4a2f-bc08-d43dc83e830d\",\"version\":1}";
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        WalletFile walletFile;
        try {
            walletFile = objectMapper.readValue(keystore, WalletFile.class);
            DecryptParam decryptParam = new DecryptParam(walletFile.getCrypto(), "Qq963123");
            byte[] derivedKey = decryptPart(decryptParam);
            ECKeyPair ecKeyPair = decryptCheck(walletFile, derivedKey);
            System.out.println("0x" + ecKeyPair.getPrivateKey().toString(16));
        } catch (IOException e) {
        } catch (CipherException e) {
            e.printStackTrace();
        }
    }
}
