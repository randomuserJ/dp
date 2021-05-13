package main.helpers.utilities;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Randomizer {

    /**
     * Only a simple wrapper for creating SecureRandom instance with pre-defined seed.
     * The purpose of this class is to provide the same 'randomness' in every part of project.
     * @return the instance of 'NativePRNG' SecureRandom instance
     */
    public static SecureRandom getSecureRandom() {
        SecureRandom sr = new SecureRandom();
        try {
            sr = SecureRandom.getInstance("NativePRNG");
        } catch (NoSuchAlgorithmException ignored) {}

        return sr;
    }
}
