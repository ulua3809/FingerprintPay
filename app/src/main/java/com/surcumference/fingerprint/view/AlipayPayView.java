package com.surcumference.fingerprint.view;

import static com.surcumference.fingerprint.Constant.ICON_FINGER_PRINT_ALIPAY_BASE64;
import static com.surcumference.fingerprint.Constant.ICON_FINGER_PRINT_CLOSE_BASE64;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.ImageUtils;
import com.surcumference.fingerprint.util.StyleUtils;

/**
 * Created by Jason on 2021/8/27.
 */
public class AlipayPayView extends DialogFrameLayout {

    private FrameLayout mCloseImageContainer;

    public AlipayPayView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public AlipayPayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AlipayPayView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        FrameLayout rootFrameLayout = new FrameLayout(context);

        LinearLayout vLinearLayout = new LinearLayout(context);
        vLinearLayout.setOrientation(LinearLayout.VERTICAL);
        vLinearLayout.setGravity(Gravity.CENTER);
        ImageView fingerprintImage = new ImageView(context);
        fingerprintImage.setImageBitmap(ImageUtils.base64ToBitmap(ICON_FINGER_PRINT_ALIPAY_BASE64));

        TextView textView = new TextView(context);
        StyleUtils.apply(textView);
        textView.setText(Lang.getString(R.id.fingerprint_verification));

        mCloseImageContainer = new FrameLayout(context);
        ImageView closeImage = new ImageView(context);
        closeImage.setImageBitmap(ImageUtils.base64ToBitmap(ICON_FINGER_PRINT_CLOSE_BASE64));
        mCloseImageContainer.setPadding(DpUtils.dip2px(context, 10),DpUtils.dip2px(context, 15),DpUtils.dip2px(context, 15),DpUtils.dip2px(context, 10));
        mCloseImageContainer.addView(closeImage, new FrameLayout.LayoutParams(DpUtils.dip2px(context, 22), DpUtils.dip2px(context, 22)));

        TextView enterPassBtn = new TextView(context);
        enterPassBtn.setText(Lang.getString(R.id.enter_password));
        StyleUtils.apply(enterPassBtn);
        enterPassBtn.setTextColor(0xFF1677FF);
        enterPassBtn.setPadding(DpUtils.dip2px(context, 10),DpUtils.dip2px(context, 15),DpUtils.dip2px(context, 15),DpUtils.dip2px(context, 10));
        enterPassBtn.setOnClickListener(v -> {
            AlertDialog dialog = getDialog();
            if (dialog != null) {
                dialog.dismiss();
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(DpUtils.dip2px(context, 60), DpUtils.dip2px(context, 60));
        params.topMargin = DpUtils.dip2px(context, 60);
        params.bottomMargin = DpUtils.dip2px(context, 30);
        vLinearLayout.addView(fingerprintImage, params);
        params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = DpUtils.dip2px(context, 30);
        vLinearLayout.addView(textView, params);

        rootFrameLayout.addView(vLinearLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams frameLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        frameLayoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
        rootFrameLayout.addView(enterPassBtn, frameLayoutParams);
        FrameLayout.LayoutParams closeImageLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        closeImageLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        rootFrameLayout.addView(mCloseImageContainer, closeImageLayoutParams);

        this.addView(rootFrameLayout);
    }

    @Override
    public AlertDialog showInDialog() {
        AlertDialog dialog = super.showInDialog();
        dialog.setCancelable(false);
        dialog.setOnKeyListener((dialog1, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                dialog1.dismiss();
                return true;
            }
            return false;
        });
        return dialog;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
    }

    public AlipayPayView withOnCloseImageClickListener(OnClickListener listener) {
        mCloseImageContainer.setOnClickListener(listener);
        return this;
    }
}
