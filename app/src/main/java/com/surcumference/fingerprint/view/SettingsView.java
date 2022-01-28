package com.surcumference.fingerprint.view;

import static com.surcumference.fingerprint.view.PasswordInputView.DEFAULT_HIDDEN_PASS;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.adapter.PreferenceAdapter;
import com.surcumference.fingerprint.network.updateCheck.UpdateFactory;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;

import java.util.ArrayList;
import java.util.List;

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
                if (data.selectionState || checkPasswordAndNotify(context)) {
                    data.selectionState = !data.selectionState;
                    config.setOn(data.selectionState);
                    mListAdapter.notifyDataSetChanged();
                }
            }
        } else if (Lang.getString(R.id.settings_title_password).equals(data.title)) {
            PasswordInputView passwordInputView = new PasswordInputView(context);
            if (!TextUtils.isEmpty(config.getPassword())) {
                passwordInputView.setDefaultText(DEFAULT_HIDDEN_PASS);
            }
            passwordInputView.withOnDismissListener(v -> {
                PasswordInputView inputView = (PasswordInputView) v;
                String inputText = inputView.getInput();
                if (TextUtils.isEmpty(inputText)) {
                    config.setPassword("");
                    return;
                }
                if (DEFAULT_HIDDEN_PASS.equals(inputText)) {
                    return;
                }
                config.setPassword(inputText);
            }).showInDialog();
        } else if (Lang.getString(R.id.settings_title_checkupdate).equals(data.title)) {
            UpdateFactory.doUpdateCheck(context, false, true);
        } else if (Lang.getString(R.id.settings_title_donate).equals(data.title)) {
            new DonateView(context).showInDialog();
        } else if (Lang.getString(R.id.settings_title_webside).equals(data.title)) {
            com.surcumference.fingerprint.util.UrlUtils.openUrl(context, Constant.PROJECT_URL);
            Toast.makeText(context, Lang.getString(R.id.toast_give_me_star), Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkPasswordAndNotify(Context context) {
        String pwd = Config.from(context).getPassword();
        if (TextUtils.isEmpty(pwd)) {
            String packageName = context.getPackageName();
            if (Constant.PACKAGE_NAME_WECHAT.equals(packageName)) {
                Toast.makeText(context, Lang.getString(R.id.toast_password_not_set_wechat), Toast.LENGTH_SHORT).show();
            } else if (Constant.PACKAGE_NAME_ALIPAY.equals(packageName)) {
                Toast.makeText(context, Lang.getString(R.id.toast_password_not_set_alipay), Toast.LENGTH_SHORT).show();
            } else if (Constant.PACKAGE_NAME_TAOBAO.equals(packageName)) {
                Toast.makeText(context, Lang.getString(R.id.toast_password_not_set_taobao), Toast.LENGTH_SHORT).show();
            } else if (Constant.PACKAGE_NAME_QQ.equals(packageName)) {
                Toast.makeText(context, Lang.getString(R.id.toast_password_not_set_qq), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, Lang.getString(R.id.toast_password_not_set_generic), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }
}
