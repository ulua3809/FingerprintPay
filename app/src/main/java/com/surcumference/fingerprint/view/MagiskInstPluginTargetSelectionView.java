package com.surcumference.fingerprint.view;

import android.app.AlertDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.bean.PluginTarget;
import com.surcumference.fingerprint.plugin.PluginApp;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.view.smoothcompoundbutton.SmoothSwitch;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jason on 2021/8/30.
 */
public class MagiskInstPluginTargetSelectionView extends DialogFrameLayout {

    public MagiskInstPluginTargetSelectionView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public MagiskInstPluginTargetSelectionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MagiskInstPluginTargetSelectionView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LinearLayout rootLinearLayout = new LinearLayout(context);
        rootLinearLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingH = DpUtils.dip2px(context, 20);
        int paddingV = DpUtils.dip2px(context, 3);
        rootLinearLayout.setPadding(paddingH, DpUtils.dip2px(context, 6), paddingH, paddingV);
        rootLinearLayout.setClipChildren(false);
        rootLinearLayout.setClipToOutline(false);
        rootLinearLayout.setClipToPadding(false);

        PluginApp.iterateAllPluginTarget(pluginTarget -> {
            View view = createPluginTargetSelectionView(context, pluginTarget);
            rootLinearLayout.addView(view);
        });

        this.addView(rootLinearLayout);
        withPositiveButtonText(Lang.getString(R.id.ok));
        withNegativeButtonText(Lang.getString(R.id.cancel));
    }

    private View createPluginTargetSelectionView(Context context, PluginTarget pluginTarget) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setWeightSum(1);

        TextView textView = new TextView(context);
        StyleUtils.apply(textView);
        textView.setText(pluginTarget.getAppName());
        textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        linearLayout.addView(textView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        SmoothSwitch switchView = new SmoothSwitch(context);
        switchView.setChecked(true);
        switchView.setTag(pluginTarget);
        switchView.setEnabled(true);
        switchView.setClickable(true);
        linearLayout.addView(switchView);

        int paddingV = DpUtils.dip2px(context, 3);
        linearLayout.setPadding(0, paddingV, 0, paddingV);
        return linearLayout;
    }

    public Map<PluginTarget, Boolean> getSelection() {
        Map<PluginTarget, Boolean> resultMap = new HashMap<>();
        PluginApp.iterateAllPluginTarget(pluginTarget -> {
            SmoothSwitch switchView = findViewWithTag(pluginTarget);
            if (switchView == null) {
                L.e("switchView is null");
                return;
            }
            resultMap.put(pluginTarget, switchView.isChecked());
        });
        return resultMap;
    }

    @Override
    public String getDialogTitle() {
        return Lang.getString(R.id.settings_sub_title_update_modules_same_time);
    }

    @Override
    public AlertDialog showInDialog() {
        AlertDialog dialog = super.showInDialog();
        dialog.setCancelable(false);
        return dialog;
    }
}
