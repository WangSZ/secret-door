package com.wangsz.netty.proxy;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * @author shaoze.wang
 */
public class EncryptHelper {
    private String password;
    private SecretKeySpec key;
    private String transformation="AES/ECB/PKCS5Padding";

    public EncryptHelper(String password) {
        this.password = password;
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(password.getBytes(StandardCharsets.UTF_8));
            kgen.init(256,random);
            key = new SecretKeySpec(kgen.generateKey().getEncoded(), "AES");
        } catch (Exception ignore) {
            throw new RuntimeException(ignore);
        }
    }

    public byte[] encrypt(byte[] byteContent) {
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(byteContent);
        } catch (Exception ignore) {
            throw new RuntimeException(ignore);
        }
    }

    public byte[] decrypt(byte[] bytes) {
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(bytes);
        } catch (Exception ignore) {
            throw new RuntimeException(ignore);
        }
    }

    public String encrypt(String content){
        return Base64.getEncoder().encodeToString(encrypt(content.getBytes(StandardCharsets.UTF_8)));
    }
    public String decrypt(String content){
        return new String(decrypt(Base64.getDecoder().decode(content)),StandardCharsets.UTF_8);
    }

}