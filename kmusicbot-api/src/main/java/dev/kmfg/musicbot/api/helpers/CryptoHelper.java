package dev.kmfg.musicbot.api.helpers;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Base64;
import java.security.spec.KeySpec;

public class CryptoHelper {
    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PASSPHRASE = Dotenv.load().get("ENCRYPTION_PASS");
    private static final int KEY_SIZE = 128;
    private static final int ITERATION_COUNT = 1000;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    public static String[] encrypt(String data) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);

        SecretKey key = deriveKey(PASSPHRASE, iv);

        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

        byte[] encryptedData = cipher.doFinal(data.getBytes());

        return new String[] { Base64.getEncoder().encodeToString(encryptedData),
                Base64.getEncoder().encodeToString(iv) };
    }

    public static String decrypt(String encryptedData, String encodedIv) throws Exception {
        byte[] iv = Base64.getDecoder().decode(encodedIv);

        SecretKey key = deriveKey(PASSPHRASE, iv);

        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

        byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedData);
    }

    private static SecretKey deriveKey(String passphrase, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_SIZE);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
}
