package edu.ohsu.cmp.coach.util;

import java.util.regex.Pattern;

public class UUIDUtil {
    private static final Pattern UUID_PATTERN = Pattern.compile("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$");

    public static boolean isUUID(String s) {
        return s != null && UUID_PATTERN.matcher(s).matches();
    }
}
