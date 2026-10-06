package com.quizlive.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.HexFormat;

public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    public static String hashPassword(String password, String saltHex) {
        if (password == null || saltHex == null) {
            throw new IllegalArgumentException("Password and salt cannot be null");
        }

        byte[] salt = HexFormat.of().parseHex(saltHex);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password with " + ALGORITHM, e);
        }
    }

    public static boolean verifyPassword(String candidatePassword, String storedHashHex, String saltHex) {
        if (candidatePassword == null || storedHashHex == null || saltHex == null) {
            return false;
        }

        String candidateHashHex = hashPassword(candidatePassword, saltHex);
        byte[] candidateHash = HexFormat.of().parseHex(candidateHashHex);
        byte[] storedHash = HexFormat.of().parseHex(storedHashHex);

        return MessageDigest.isEqual(candidateHash, storedHash);
    }
}
