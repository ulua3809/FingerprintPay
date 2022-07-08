package com.surcumference.fingerprint.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.surcumference.fingerprint.listener.OnDismissListener;
import com.surcumference.fingerprint.listener.OnShowListener;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.Umeng;
import com.surcumference.fingerprint.util.drawable.XDrawable;

/**
 * Created by Jason on 2017/9/9.
 */

public abstract class DialogFrameLayout extends FrameLayout implements DialogInterface.OnDismissListener, DialogInterface.OnShowListener {

    private OnDismissListener mDismissListener;
    private OnShowListener mShowListener;
    private DialogInterface.OnClickListener mOnNeutralButtonClickListener;
    private DialogInterface.OnClickListener mOnNegativeButtonClickListener;
    private DialogInterface.OnClickListener mOnPositiveButtonClickListener;
    private String mNeutralButtonText;
    private String mNegativeButtonText;
    private String mPositiveButtonText;
    @Nullable
    private AlertDialog mDialog;

    public DialogFrameLayout(@NonNull Context context) {
        super(context);
    }

    public DialogFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DialogFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AlertDialog showInDialog() {
        Context context = getContext();
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, android.R.style.Theme_Material_NoActionBar_Fullscreen);
        //修复支付宝主页显示更新页面时dialog宽度不正常
        contextThemeWrapper.applyOverrideConfiguration(new Configuration());
        AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper)
                .setOnDismissListener(this);
        AlertDialog dialog;
        dialog = builder.create();
        dialog.setView(createDialogContentView(dialog));
        dialog.setOnShowListener(this);
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        window.setBackgroundDrawable(dialogWindowBackground());
        dialog.show();
        Umeng.onResume(getContext());
        mDialog = dialog;
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        OnDismissListener listener = mDismissListener;
        if (listener != null) {
            listener.onDismiss(this);
        }
        Umeng.onPause(getContext());
    }

    @Override
    public void onShow(DialogInterface dialog) {
        OnShowListener listener = mShowListener;
        if (listener != null) {
            listener.onShow(this);
        }
        Umeng.onResume(getContext());
    }

    public DialogFrameLayout withOnDismissListener(OnDismissListener listener) {
        mDismissListener = listener;
        return this;
    }

    public DialogFrameLayout withOnShowListener(OnShowListener listener) {
        mShowListener = listener;
        return this;
    }

    public DialogFrameLayout withOnNeutralButtonClickListener(DialogInterface.OnClickListener listener) {
        mOnNeutralButtonClickListener = listener;
        return this;
    }

    public DialogFrameLayout withOnNegativeButtonClickListener(DialogInterface.OnClickListener listener) {
        mOnNegativeButtonClickListener = listener;
        return this;
    }

    public DialogFrameLayout withOnPositiveButtonClickListener(DialogInterface.OnClickListener listener) {
        mOnPositiveButtonClickListener = listener;
        return this;
    }

    public DialogFrameLayout withNeutralButtonText(String text) {
        mNeutralButtonText = text;
        return this;
    }

    public DialogFrameLayout withNegativeButtonText(String text) {
        mNegativeButtonText = text;
        return this;
    }

    public DialogFrameLayout withPositiveButtonText(String text) {
        mPositiveButtonText = text;
        return this;
    }

    public String getDialogTitle() {
        return null;
    }

    @Nullable
    public AlertDialog getDialog() {
        return mDialog;
    }

    /**
     * @return pixel
     */
    public int dialogWindowHorizontalInsets () {
        return DpUtils.dip2px(getContext(), 4);
    }

    private TextView createTitleTextView() {
        String title = getDialogTitle();
        Context context = getContext();
        TextView titleTextView = new TextView(context);
        titleTextView.setText(title);
        titleTextView.setTextColor(Color.BLACK);
        titleTextView.setBackgroundColor(Color.TRANSPARENT);
        titleTextView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        titleTextView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        int defHPadding = DpUtils.dip2px(context, 15);
        titleTextView.setPadding(defHPadding,  DpUtils.dip2px(context, 15), defHPadding,  DpUtils.dip2px(context, 8));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_BIG);

        if (TextUtils.isEmpty(title)) {
            titleTextView.setVisibility(GONE);
        }
        return titleTextView;
    }

    private View createNeutralButton(AlertDialog dialog) {
        Context context = getContext();
        TextView textView = new TextView(context);
        textView.setTextColor(0xFF009688);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_DEFAULT);
        textView.setBackground(new XDrawable.Builder().round(DpUtils.dip2px(context, 3)).create());
        textView.setMinWidth(DpUtils.dip2px(context, 45));
        textView.setGravity(Gravity.CENTER);

        String text = mNeutralButtonText;
        int defHPadding = DpUtils.dip2px(context, 6);
        int defVPadding = DpUtils.dip2px(context, 10);
        textView.setPadding(defHPadding, defVPadding, defHPadding, defVPadding);
        if (TextUtils.isEmpty(text)) {
            textView.setVisibility(GONE);
        } else {
            textView.setText(text);
            textView.setOnClickListener(v -> {
                DialogInterface.OnClickListener listener = mOnNeutralButtonClickListener;
                if (listener != null) {
                    listener.onClick(dialog, DialogInterface.BUTTON_NEUTRAL);
                } else {
                    dialog.dismiss();
                }
            });
        }
        LinearLayout btnCon = new LinearLayout(context);
        btnCon.addView(textView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btnCon.setGravity(Gravity.LEFT);
        return btnCon;
    }

    private View createNegativeButton(AlertDialog dialog) {
        Context context = getContext();
        TextView textView = new TextView(context);
        textView.setTextColor(0xFF009688);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_DEFAULT);
        textView.setBackground(new XDrawable.Builder().round(DpUtils.dip2px(context, 3)).create());
        textView.setMinWidth(DpUtils.dip2px(context, 45));
        textView.setGravity(Gravity.CENTER);

        String text = mNegativeButtonText;
        int defHPadding = DpUtils.dip2px(context, 6);
        int defVPadding = DpUtils.dip2px(context, 10);
        textView.setPadding(defHPadding, defVPadding, defHPadding, defVPadding);
        if (TextUtils.isEmpty(text)) {
            textView.setVisibility(GONE);
        } else {
            textView.setText(text);
            textView.setOnClickListener(v -> {
                DialogInterface.OnClickListener listener = mOnNegativeButtonClickListener;
                if (listener != null) {
                    listener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
                } else {
                    dialog.dismiss();
                }
            });
        }
        LinearLayout btnCon = new LinearLayout(context);
        btnCon.addView(textView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btnCon.setGravity(Gravity.RIGHT);
        return btnCon;
    }

    private View createPositiveButton(AlertDialog dialog) {
        Context context = getContext();
        TextView textView = new TextView(context);
        textView.setTextColor(0xFF009688);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_DEFAULT);
        textView.setBackground(new XDrawable.Builder().round(DpUtils.dip2px(context, 3)).create());
        textView.setMinWidth(DpUtils.dip2px(context, 45));
        textView.setGravity(Gravity.CENTER);

        String text = mPositiveButtonText;
        int defHPadding = DpUtils.dip2px(context, 6);
        int defVPadding = DpUtils.dip2px(context, 10);
        textView.setPadding(defHPadding, defVPadding, defHPadding, defVPadding);
        if (TextUtils.isEmpty(text)) {
            textView.setVisibility(GONE);
        } else {
            textView.setText(text);
            textView.setOnClickListener(v -> {
                DialogInterface.OnClickListener listener = mOnPositiveButtonClickListener;
                if (listener != null) {
                    listener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                } else {
                    dialog.dismiss();
                }
            });
        }
        LinearLayout btnCon = new LinearLayout(context);
        btnCon.addView(textView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btnCon.setGravity(Gravity.RIGHT);
        return btnCon;
    }

    private View createBtnAreaView(AlertDialog dialog) {
        Context context = getContext();
        LinearLayout btnLLayout = new LinearLayout(context);
        btnLLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLLayout.setWeightSum(1);

        View neutralButton = createNeutralButton(dialog);
        View negativeButton = createNegativeButton(dialog);
        View positiveButton = createPositiveButton(dialog);

        int defPadding = DpUtils.dip2px(context, 20);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.leftMargin = defPadding;
        params.rightMargin = defPadding;
        btnLLayout.addView(neutralButton, params);
        params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = defPadding;
        btnLLayout.addView(negativeButton, params);
        params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = defPadding;
        btnLLayout.addView(positiveButton, params);

        if (hasButton()) {
            btnLLayout.setPadding(0, DpUtils.dip2px(context, 10), 0, DpUtils.dip2px(context, 15));
        }
        return btnLLayout;
    }

    private View createDialogContentView(AlertDialog dialog) {
        Context context = getContext();
        View titleView = createTitleTextView();
        View contentView = this;
        View btnView = createBtnAreaView(dialog);

        LinearLayout rootLLayout = new LinearLayout(context);
        rootLLayout.setOrientation(LinearLayout.VERTICAL);
        rootLLayout.setWeightSum(1);
        rootLLayout.setBackgroundColor(Color.TRANSPARENT);
        rootLLayout.addView(titleView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rootLLayout.addView(contentView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        rootLLayout.addView(btnView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rootLLayout.setElevation(DpUtils.dip2px(context, 2));
        return rootLLayout;
    }

    private boolean hasButton() {
        return (!TextUtils.isEmpty(mNegativeButtonText))
                || (!TextUtils.isEmpty(mNeutralButtonText))
                || (!TextUtils.isEmpty(mPositiveButtonText));
    }

    private Drawable dialogWindowBackground() {
        GradientDrawable shape =  new GradientDrawable();
        shape.setCornerRadius(DpUtils.dip2px(getContext(), 10));
        shape.setColor(0xFFF5F5F5);
        int paddingH = dialogWindowHorizontalInsets();
        return new InsetDrawable(shape, paddingH, 0, paddingH, 0);
    }

}
