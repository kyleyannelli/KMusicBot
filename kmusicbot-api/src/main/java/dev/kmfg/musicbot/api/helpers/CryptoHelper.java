package dev.kmfg.musicbot.api.helpers;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import javax.crypto.spec.PBEParameterSpec;

import io.github.cdimascio.dotenv.Dotenv;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

public class CryptoHelper {
    private static String INSTANCE_TYPE = "PBEWithSHA256And256BitAES-CBC-BC";
    private static String PASSPHRASE = Dotenv.load().get("ENCRYPTION_PASS");
    private static final int ITERATION_COUNT = 65536;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String[] encrypt(String data) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        Cipher cipher = Cipher.getInstance(INSTANCE_TYPE);
        KeySpec keySpec = new PBEKeySpec(PASSPHRASE.toCharArray());
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(INSTANCE_TYPE);
        SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
        PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, ITERATION_COUNT);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
        return new String[] { org.bouncycastle.util.encoders.Base64.toBase64String(cipher.doFinal(data.getBytes())), new String(Base64.encode(salt)) };
    }

    public static String decrypt(String data, String encodedSalt) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] salt = Base64.decode(encodedSalt);
        Cipher cipher = Cipher.getInstance(INSTANCE_TYPE);
        KeySpec keySpec = new PBEKeySpec(PASSPHRASE.toCharArray());
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(INSTANCE_TYPE);
        SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
        PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, ITERATION_COUNT);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        return new String(cipher.doFinal(Base64.decode(data)));
    }
}
