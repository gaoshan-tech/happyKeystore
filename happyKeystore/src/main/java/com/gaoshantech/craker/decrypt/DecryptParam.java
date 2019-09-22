package com.gaoshantech.craker.decrypt;

import org.web3j.crypto.WalletFile;
import org.web3j.utils.Numeric;

public class DecryptParam {
    byte[] mac;
    byte[] iv;
    String kdf;
    Object kdfparams;
    String password;
    public DecryptParam(WalletFile.Crypto crypto, String password){
        mac = Numeric.hexStringToByteArray(crypto.getMac());
        iv = Numeric.hexStringToByteArray(crypto.getCipherparams().getIv());
        kdfparams = crypto.getKdfparams();
        kdf = crypto.getKdf();
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
}
