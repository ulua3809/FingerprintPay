package com.surcumference.fingerprint.util.paydialog;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.surcumference.fingerprint.util.ViewUtils;
import com.surcumference.fingerprint.util.log.L;

import java.util.ArrayList;
import java.util.List;

public class QQPayDialog {

    private static final String TAG_PASSWORD_EDITTEXT = "TAG_PASSWORD_EDITTEXT";
    private static final String TAG_LONGPASSWORD_OK_BUTTON = "TAG_LONGPASSWORD_OK_BUTTON";


    public EditText inputEditText;
    public View keyboardView;
    @Nullable
    public TextView usePasswordText;
    @Nullable
    public TextView titleTextView;
    //长密码不为空
    @Nullable
    public View okButton;
    //提现时的标题
    @Nullable
    public TextView withdrawTitleTextView;


    @Nullable
    public static QQPayDialog findFrom(ViewGroup rootView) {
        try {
            QQPayDialog payDialog = new QQPayDialog();

            List<View> childViews = new ArrayList<>();
            ViewUtils.getChildViews(rootView, childViews);
            payDialog.okButton = ViewUtils.findViewByText(rootView, "立即支付", "立即验证", "确定");
            if (payDialog.okButton != null && payDialog.okButton.isShown()) {
                payDialog.okButton.setTag(TAG_LONGPASSWORD_OK_BUTTON);
            }
            boolean longPassword = payDialog.isLongPassword();
            for (View view : childViews) {
                if (view == null) {
                    continue;
                }
                if (longPassword) {
                    if (view instanceof EditText && "输入财付通支付密码".equals(((EditText) view).getHint())) {
                        if (view.isShown() || TAG_PASSWORD_EDITTEXT.equals(view.getTag())) {
                            payDialog.inputEditText = (EditText)view;
                            view.setTag(TAG_PASSWORD_EDITTEXT);
                        }
                    }
                } else {
                    if (view instanceof EditText && "支付密码输入框".equals(view.getContentDescription())) {
                        if (view.isShown() || TAG_PASSWORD_EDITTEXT.equals(view.getTag())) {
                            payDialog.inputEditText = (EditText)view;
                            view.setTag(TAG_PASSWORD_EDITTEXT);
                        }
                    }
                }
                if (view.getClass().getName().endsWith(".MyKeyboardWindow")) {
                    L.d("密码键盘:" + view);
                    if (view.getParent() != null) {
                        payDialog.keyboardView = view;
                    }
                }
                if (payDialog.inputEditText != null && payDialog.keyboardView != null) {
                    break;
                }
            }

            if (payDialog.inputEditText == null) {
                L.d("inputEditText not found");
                return null;
            }

            if (payDialog.keyboardView == null) {
                L.d("keyboardView not found");
                return null;
            }

            payDialog.usePasswordText = (TextView) ViewUtils.findViewByText(rootView,
                    "使用密码", "使用密碼", "Password",
                    "使用指纹", "使用指紋", "Fingerprint");
            if (payDialog.usePasswordText == null) {
                L.d("usePasswordText not found");
            }

            payDialog.titleTextView = (TextView) ViewUtils.findViewByText(rootView,
                    "请验证指纹", "請驗證指紋", "找回密码", "Verify fingerprint",
                    "请输入支付密码", "請輸入付款密碼", "Enter payment password");
            if (payDialog.titleTextView == null) {
                L.d("titleTextView not found");
            }

            payDialog.withdrawTitleTextView = (TextView) ViewUtils.findViewByText(rootView, "输入支付密码，验证身份");
            if (payDialog.withdrawTitleTextView == null) {
                L.d("withdrawTitleTextView not found");
            }
            return payDialog;
        } catch (Exception e) {
            L.e(e);
        }
        return null;
    }

    @Override
    public String toString() {
        return "PayDialog{" +
                "inputEditText=" + inputEditText +
                ", keyboardView=" + keyboardView +
                ", titleTextView=" + titleTextView +
                '}';
    }

    public boolean isLongPassword() {
        return okButton != null && (okButton.isShown() || TAG_LONGPASSWORD_OK_BUTTON.equals(okButton.getTag()));
    }
}
