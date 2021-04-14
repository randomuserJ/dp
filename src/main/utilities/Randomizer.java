package main.utilities;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Randomizer {

    public static SecureRandom getSecureRandom() {
        SecureRandom sr = new SecureRandom();
        try {
            sr = SecureRandom.getInstance("NativePRNG");
        } catch (NoSuchAlgorithmException ignored) {}

        return sr;
    }
}
