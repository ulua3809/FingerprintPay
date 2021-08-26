package com.surcumference.fingerprint.util;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.surcumference.fingerprint.util.log.L;

/**
 * Created by Jason on 2017/8/5.
 */

public class KeyboardUtils {

    public static void switchIme(View view, boolean show) {

        try {
            Context context = view.getContext();
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

            if (context.getResources().getConfiguration().hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES) {
                show = false;
            }
            if (show) { // 显示键盘，即输入法
                if (imm != null) {
                    imm.showSoftInput(view, 0);
                }
            } else { // 隐藏键盘
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        } catch (Exception | Error e) {
            L.e(e);
        }
    }
}
