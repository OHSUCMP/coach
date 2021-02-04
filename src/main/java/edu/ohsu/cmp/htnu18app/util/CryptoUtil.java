package edu.ohsu.cmp.htnu18app.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtil {
    private static final Logger logger = LoggerFactory.getLogger(CryptoUtil.class);

    private static final String ALGORITHM = "AES";

    private static SecretKey SECRET_KEY = null;

    public static String genRandom(int length) {
        return RandomStringUtils.random(length, 0, 0, true, true, null, new SecureRandom());
    }

    public static String encrypt(String plain) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());

            Base64.Encoder encoder = Base64.getEncoder();
            byte[] enc = cipher.doFinal(plain.getBytes());
            return encoder.encodeToString(enc);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public static String decrypt(String garbled) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] enc = decoder.decode(garbled);
            return new String(cipher.doFinal(enc));

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    private static SecretKey getSecretKey() throws NoSuchAlgorithmException {
        if (SECRET_KEY == null) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256);
            SECRET_KEY = keyGenerator.generateKey();
        }
        return SECRET_KEY;
    }
}
