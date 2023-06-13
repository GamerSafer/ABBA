package com.gamersafer.minecraft.abbacaving.util;

import java.security.SecureRandom;

public class GameToken {

    private static final int TOKEN_LENGTH = 6;
    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom rnd = new SecureRandom();

    public static String randomToken() {
        final StringBuilder sb = new StringBuilder(TOKEN_LENGTH);

        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        }

        return sb.toString();
    }
}
