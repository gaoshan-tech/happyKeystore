package com.gaoshantech.craker.decrypt;

public class DecryptResult {
    private byte[] derivedKey;

    public byte[] getDerivedKey() {
        return derivedKey;
    }

    public void setDerivedKey(byte[] derivedKey) {
        this.derivedKey = derivedKey;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String password;

    public DecryptResult(byte[] derivedKey, String password) {
        this.derivedKey = derivedKey;
        this.password = password;
    }
}
