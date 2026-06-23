package com.shorthand.backend.infrastructure.adapter.outbound.generator;

public class Base62Encoder {

    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = CHARACTERS.length();

    public String encode(long number) {
        if (number == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        while (number > 0) {
            int remainder = (int) (number % BASE);
            sb.append(CHARACTERS.charAt(remainder));
            number /= BASE;
        }
        return sb.reverse().toString();
    }

    public long decode(String string) {
        long number = 0;
        for (int i = 0; i < string.length(); i++) {
            number = number * BASE + CHARACTERS.indexOf(string.charAt(i));
        }
        return number;
    }
}
