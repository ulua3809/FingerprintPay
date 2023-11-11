package com.surcumference.fingerprint.util;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.ColorInt;

/**
 * Created by Jason on 2017/9/9.
 */

public class StyleUtils {

    public static final float TEXT_SIZE_DEFAULT = 15.0f;
    public static final float TEXT_SIZE_BIG = 18.0f;
    public static final float TEXT_SIZE_SMALL = 12.0f;

    public static final int TEXT_COLOR_DEFAULT = Color.BLACK;
    public static final int TEXT_COLOR_SECONDARY = 0xFF8A9899;
    public static final int LINE_COLOR_DEFAULT = 0xFFE5E5E5;

    public static void apply(TextView textView) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DEFAULT);
        textView.setTextColor(TEXT_COLOR_DEFAULT);
    }

    public static boolean isDarkMode(Context context) {
        return Configuration.UI_MODE_NIGHT_YES == (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
    }

    public static boolean isColorDark(@ColorInt int color) {
        return getUnsignedInt(color) < getUnsignedInt(0xFF808080);
    }

    public static long getUnsignedInt(int x) {
        return x & (-1L >>> 32);
    }
}
