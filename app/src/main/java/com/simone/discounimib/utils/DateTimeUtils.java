package com.simone.discounimib.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {
    private static final String DATE_FORMAT = "dd/MM/yyyy";
    private static final String TIME_FORMAT = "HH:mm";

    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.ITALY);
        return sdf.format(date);
    }

    public static String formatTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.ITALY);
        return sdf.format(date);
    }

    public static Date parseDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.ITALY);
            return sdf.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date parseTime(String timeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.ITALY);
            return sdf.parse(timeStr);
        } catch (Exception e) {
            return null;
        }
    }
}