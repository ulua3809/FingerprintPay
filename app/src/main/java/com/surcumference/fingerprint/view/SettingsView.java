package com.surcumference.fingerprint.view;

import static com.surcumference.fingerprint.view.PasswordInputView.DEFAULT_HIDDEN_PASS;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.adapter.PreferenceAdapter;
import com.surcumference.fingerprint.network.updateCheck.UpdateFactory;
import com.surcumference.fingerprint.util.AESUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.NotifyUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.ViewUtils;
import com.surcumference.fingerprint.util.XFingerprintIdentify;
import com.surcumference.fingerprint.util.log.L;
import com.wei.android.lib.fingerprintidentify.bean.FingerprintIdentifyFailInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;

/**
 * Created by Jason on 2017/9/9.
 */

public class SettingsView extends DialogFrameLayout implements AdapterView.OnItemClickListener {

    private List<PreferenceAdapter.Data> mSettingsDataList = new ArrayList<>();
    private PreferenceAdapter mListAdapter;
    private ListView mListView;

    public SettingsView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SettingsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SettingsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LinearLayout rootVerticalLayout = new LinearLayout(context);
        rootVerticalLayout.setOrientation(LinearLayout.VERTICAL);

        View lineView = new View(context);
        lineView.setBackgroundColor(Color.TRANSPARENT);

        int defHPadding = DpUtils.dip2px(context, 0);
        int defVPadding = DpUtils.dip2px(context, 12);

        mListView = new ListView(context);
        mListView.setDividerHeight(0);
        mListView.setOnItemClickListener(this);
        mListView.setPadding(defHPadding, defVPadding, defHPadding, defVPadding);
        mListView.setDivider(new ColorDrawable(Color.TRANSPARENT));

        String packageName = context.getPackageName();
        switch (packageName) {
            case Constant.PACKAGE_NAME_WECHAT:
                mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_switch), Lang.getString(R.id.settings_sub_title_switch_wechat), true, Config.from(context).isOn()));
                mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_password), Lang.getString(R.id.settings_sub_title_password_wechat)));
                break;
            case Constant.PACKAGE_NAME_QQ:
                mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_switch), Lang.getString(R.id.settings_sub_title_switch_qq), true, Config.from(context).isOn()));
                mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_password), Lang.getString(R.id.settings_sub_title_password_qq)));
                break;
            case Constant.PACKAGE_NAME_TAOBAO:
            case Constant.PACKAGE_NAME_ALIPAY:
                mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_switch), Lang.getString(R.id.settings_sub_title_switch_alipay), true, Config.from(context).isOn()));
                mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_password), Lang.getString(R.id.settings_sub_title_password_alipay)));
                break;
            case Constant.PACKAGE_NAME_UNIONPAY:
                mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_switch), Lang.getString(R.id.settings_sub_title_switch_unionpay), true, Config.from(context).isOn()));
                mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_password), Lang.getString(R.id.settings_sub_title_password_unionpay)));
                break;
            default:
                throw new RuntimeException("Package " + packageName + " not supported yet!");
        }
        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_donate), Lang.getString(R.id.settings_sub_title_donate)));
        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_advance), Lang.getString(R.id.settings_sub_title_advance)));
        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_checkupdate), Lang.getString(R.id.settings_sub_title_checkupdate)));
        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_webside), Constant.PROJECT_URL));
        mListAdapter = new PreferenceAdapter(mSettingsDataList);

        rootVerticalLayout.addView(lineView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, com.surcumference.fingerprint.util.DpUtils.dip2px(context, 2)));
        rootVerticalLayout.addView(mListView);

        this.addView(rootVerticalLayout);
    }

    @Override
    public String getDialogTitle() {
        return Lang.getString(R.id.app_settings_name) + " " + BuildConfig.VERSION_NAME;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mListView.setAdapter(mListAdapter);
    }

    @Override
    public int dialogWindowHorizontalInsets() {
        return DpUtils.dip2px(getContext(), 26);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        PreferenceAdapter.Data data = mListAdapter.getItem(position);
        final Context context = getContext();
        final Config config = Config.from(context);
        if (Lang.getString(R.id.settings_title_switch).equals(data.title)) {
            if (!data.selectionState && !config.getLicenseAgree()) {
                new LicenseView(context)
                    .withOnNegativeButtonClickListener((dialog, which) -> {
                        config.setOn(false);
                        config.setLicenseAgree(false);
                        dialog.dismiss();
                        data.selectionState = false;
                        mListAdapter.notifyDataSetChanged();
                    }).withOnPositiveButtonClickListener((dialog, which) -> {
                        config.setLicenseAgree(true);
                        if (checkPasswordAndNotify(context)) {
                            config.setOn(true);
                            data.selectionState = true;
                        }
                        dialog.dismiss();
                        mListAdapter.notifyDataSetChanged();
                    }).showInDialog();
            } else {
                if (!data.selectionState && !checkPasswordAndNotify(context)) {
                    showUpdatePasswordViewDialog();
                    return;
                }
                data.selectionState = !data.selectionState;
                config.setOn(data.selectionState);
                mListAdapter.notifyDataSetChanged();
            }
        } else if (Lang.getString(R.id.settings_title_password).equals(data.title)) {
            showUpdatePasswordViewDialog();
        } else if (Lang.getString(R.id.settings_title_checkupdate).equals(data.title)) {
            UpdateFactory.doUpdateCheck(context, false, true);
        } else if (Lang.getString(R.id.settings_title_advance).equals(data.title)) {
            new AdvanceSettingsView(context).showInDialog();
        } else if (Lang.getString(R.id.settings_title_donate).equals(data.title)) {
            new DonateView(context).showInDialog();
        } else if (Lang.getString(R.id.settings_title_webside).equals(data.title)) {
            com.surcumference.fingerprint.util.UrlUtils.openUrl(context, Constant.PROJECT_URL);
            Task.onMain(1000, () -> Toaster.showLong(Lang.getString(R.id.toast_give_me_star)));
        }
    }

    private void showUpdatePasswordViewDialog() {
        Context context = getContext();
        Config config = Config.from(context);
        PasswordInputView passwordInputView = new PasswordInputView(context);
        if (!TextUtils.isEmpty(config.getPasswordEncrypted())) {
            passwordInputView.setDefaultText(DEFAULT_HIDDEN_PASS);
        }
        passwordInputView.withOnPositiveButtonClickListener((dialog, which) -> {
            String inputText = passwordInputView.getInput();
            if (TextUtils.isEmpty(inputText)) {
                config.setPasswordEncrypted("");
                config.setPasswordIV("");
                dialog.dismiss();
                return;
            }
            if (DEFAULT_HIDDEN_PASS.equals(inputText)) {
                dialog.dismiss();
                return;
            }
            updatePassword(dialog, inputText);
        }).showInDialog();
    }

    private void updatePassword(DialogInterface passwordInputDialog, String password) {
        Context context = this.getContext();
        Config config = Config.from(context);
        XFingerprintIdentify fingerprintIdentify = new XFingerprintIdentify(context)
                .withEncryptionMode();

        AlertDialog fingerprintVerificationDialog = new FingerprintVerificationView(context)
                .withOnCloseImageClickListener((target, v) -> {
            target.getDialog().dismiss();
            fingerprintIdentify.cancelIdentify();
        }).withOnDismissListener(v -> {
            fingerprintIdentify.cancelIdentify();
        }).showInDialog();
        fingerprintIdentify.startIdentify(new XFingerprintIdentify.IdentifyListener() {

            @Override
            public void onInited(XFingerprintIdentify identify) {
                super.onInited(identify);
                if (identify.isUsingBiometricApi()) {
                    ViewUtils.setAlpha(fingerprintVerificationDialog, 0);
                }
            }

            @Override
                    public void onSucceed(XFingerprintIdentify target, Cipher cipher) {
                        super.onSucceed(target, cipher);
                        NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_password_enc_success));
                        String encrypted = AESUtils.encrypt(cipher, password);
                        config.setPasswordEncrypted(encrypted);
                        config.setPasswordIV(AESUtils.byte2hex(cipher.getIV()));

                        passwordInputDialog.dismiss();
                        fingerprintVerificationDialog.dismiss();
                    }

            @Override
            public void onFailed(XFingerprintIdentify target, FingerprintIdentifyFailInfo failInfo) {
                super.onFailed(target, failInfo);
                ViewUtils.setAlpha(fingerprintVerificationDialog, 1);
                ViewUtils.setDimAmount(fingerprintVerificationDialog, 0.6f);
                fingerprintVerificationDialog.dismiss();
                Toaster.showShort(Lang.getString(R.id.toast_fingerprint_operation_cancel));
            }
        });
    }

    private boolean checkPasswordAndNotify(Context context) {
        String pwd = Config.from(context).getPasswordEncrypted();
        if (TextUtils.isEmpty(pwd)) {
            Toaster.showLong(Lang.getString(R.id.toast_password_not_set_switch_on_failed));
            return false;
        }
        return true;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        Context context = getContext();
        if (Constant.PACKAGE_NAME_QQ.equals(context.getPackageName())) {
            Config.from(context).commit();
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
                if ("com.tencent.mobileqq:tool".equals(processInfo.processName)) {
                    android.os.Process.killProcess(processInfo.pid);
                    try {
                        Runtime.getRuntime().exec(new String[]{"kill", "-9", String.valueOf(processInfo.pid)});
                    } catch (IOException e) {
                        L.e(e);
                    }
                }
                L.d("processInfo", processInfo.processName, processInfo.pid);
            }
        }
        super.onDismiss(dialogInterface);
    }
}
