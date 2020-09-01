package com.atmos.model;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public final class Encryption {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CFB/PKCS5Padding";
    private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final int ITERATION_COUNT = 10000;
    private static final int KEY_LENGTH = 128;

    private static byte[] salt = new byte[8];
    private static byte[] iv = new byte[16];

    private static SecureRandom srandom = new SecureRandom();
    private static SecretKeyFactory factory = buildFactory();
    private static Cipher cipher = buildCipher();

    public static void encryptFile(String password, File inFile, File outFile) throws CryptoException {
        try (FileInputStream in = new FileInputStream(inFile);
             FileOutputStream out = new FileOutputStream(outFile)) {

            srandom.nextBytes(iv);
            srandom.nextBytes(salt);
            SecretKeySpec secretKey = setup(password);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

            out.write(salt);
            out.write(iv);

            processFile(inFile, in, out);
        } catch (InvalidKeySpecException |
                IOException |
                InvalidKeyException |
                InvalidAlgorithmParameterException |
                IllegalBlockSizeException |
                BadPaddingException exc) {
            throw new CryptoException("Something went wrong couldn't encrypt the file", exc);
        }
    }

    public static void decryptFile(String password, File inFile, File outFile) throws CryptoException {
        try (FileOutputStream out = new FileOutputStream(outFile);
             FileInputStream in = new FileInputStream(inFile)) {

            in.read(salt);
            in.read(iv);

            SecretKeySpec secretKey = setup(password);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            processFile(inFile, in, out);
        } catch (IOException |
                InvalidKeySpecException |
                InvalidKeyException |
                InvalidAlgorithmParameterException |
                IllegalBlockSizeException |
                BadPaddingException |
                CryptoException exc) {
            throw new CryptoException("Something went wrong couldn't decrypt the file", exc);
        }
    }

    private static SecretKeySpec setup(String password) throws InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }

    private static void processFile(File file, InputStream in, OutputStream out) throws IllegalBlockSizeException, BadPaddingException, IOException, CryptoException {
        byte[] data = new byte[(int) file.length()];
        int len;
        while ((len = in.read(data)) != -1) {
            byte[] transformedData = cipher.update(data, 0, len);
            if (transformedData != null) out.write(transformedData);
        }
        boolean areSomeOfThemZero = false;
        boolean areAllZero = false;
        for (byte byde : data) {
            if (byde == (byte) 0) areAllZero = true;
            else areSomeOfThemZero = true;
        }
        if (areAllZero && !areSomeOfThemZero) throw new CryptoException("The file isn't appropriate apparently xD");
        byte[] finalData = cipher.doFinal();
        if (finalData != null) out.write(finalData);
    }

    private static SecretKeyFactory buildFactory() {
        SecretKeyFactory secretKeyFactory = null;
        try {
            secretKeyFactory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return secretKeyFactory;
    }

    private static Cipher buildCipher() {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(TRANSFORMATION);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return cipher;
    }

}
