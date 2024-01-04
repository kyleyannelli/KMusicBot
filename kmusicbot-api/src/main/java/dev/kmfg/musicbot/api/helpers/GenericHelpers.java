package dev.kmfg.musicbot.api.helpers;

public class GenericHelpers {
    public static boolean isNotNumber(String s) {
        return s == null || s == "" || !s.matches("-?\\d+(\\.\\d+)?");
    }
    public static boolean isNumber(String s) {
        return s != null && s != "" || s.matches("-?\\d+(\\.\\d+)?");
    }
}
