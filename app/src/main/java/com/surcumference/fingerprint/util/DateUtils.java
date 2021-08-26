package com.surcumference.fingerprint.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Jason on 2017/9/10.
 */

public class DateUtils {

    private static final SimpleDateFormat DATE_FORMAT_YYYYMMDDHHMMSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String toString(Date date) {
        return DATE_FORMAT_YYYYMMDDHHMMSS.format(date);
    }
}
