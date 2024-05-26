package com.surcumference.fingerprint.view;

import static com.surcumference.fingerprint.Constant.ICON_FINGER_PRINT_ALIPAY_BASE64;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.ImageUtils;

/**
 * Created by Jason on 2021/8/27.
 */
public class AlipayPayView extends DialogFrameLayout<AlipayPayView> {

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
        fingerprintImage.setVisibility(Config.from(context).isShowFingerprintIcon() ? VISIBLE: INVISIBLE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(DpUtils.dip2px(context, 60), DpUtils.dip2px(context, 60));
        params.topMargin = DpUtils.dip2px(context, 90);
        params.bottomMargin = DpUtils.dip2px(context, 90);
        vLinearLayout.addView(fingerprintImage, params);

        rootFrameLayout.addView(vLinearLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams frameLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        frameLayoutParams.gravity = Gravity.TOP | Gravity.RIGHT;

        withNeutralButtonText(Lang.getString(R.id.cancel));
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
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.gravity = Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;
            window.setAttributes(layoutParams);
        }
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

    public AlipayPayView withOnCancelButtonClickListener(OnCancelButtonClickListener listener) {
        withOnNeutralButtonClickListener((dialog, which) -> listener.onClicked(AlipayPayView.this));
        return this;
    }

    public interface OnCancelButtonClickListener {
        void onClicked(AlipayPayView target);
    }

    @Override
    public String getDialogTitle() {
        return Lang.getString(R.id.fingerprint_verification);
    }

    @Override
    public Rect dialogWindowInset() {
        return new Rect(0,0,0,0);
    }
}
