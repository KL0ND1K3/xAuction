package dev.minecraft.xauction.util;

import java.util.Locale;

public final class Money {
    private Money() {
    }

    public static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    public static String format(double value) {
        double v = round(value);
        if (Math.abs(v - Math.rint(v)) < 0.00005) {
            return String.valueOf((long) Math.rint(v));
        }
        String s = String.format(Locale.US, "%.4f", v);
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static String clock(long seconds) {
        long s = Math.max(0, seconds);
        long m = s / 60;
        long r = s % 60;
        return m + ":" + (r < 10 ? "0" : "") + r;
    }

    public static Double parse(String raw, boolean allowDecimal) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim().replace(",", ".").replace(" ", "");
        if (t.isEmpty()) {
            return null;
        }
        try {
            double v = Double.parseDouble(t);
            if (!Double.isFinite(v) || v <= 0) {
                return null;
            }
            if (!allowDecimal && Math.abs(v - Math.rint(v)) > 0.00005) {
                return null;
            }
            return round(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
