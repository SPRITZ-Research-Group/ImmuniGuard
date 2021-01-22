package com.contacttracing.immuniguard;

import java.util.Random;

public class RandomStringGenerator {
    private static final String ALLOWED_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMOPQRSTUVWXYZ";

    public static String getRandomString(final int stringSize) {
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(stringSize);
        for(int i=0; i<stringSize; i++)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }
}
