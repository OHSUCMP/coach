package edu.ohsu.cmp.coach.util;

import java.util.UUID;
import java.util.regex.Pattern;

public class UUIDUtil {
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$");

    public static boolean isUUID(String s) {
        return UUID_PATTERN.matcher(s).matches();
    }

    public static String getRandomUUID() {
        return UUID.randomUUID().toString();
    }
}
