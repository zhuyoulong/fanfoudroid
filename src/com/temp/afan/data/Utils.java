package com.temp.afan.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {
    static SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
    
    public static Date parseDate(String str, String format) throws ParseException {
        if (str == null || "".equals(str)) {
            return null;
        }
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.parse(str);
    }
}