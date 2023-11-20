package com.surcumference.fingerprint.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.adapter.PreferenceAdapter;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DateUtils;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.FileUtils;
import com.surcumference.fingerprint.util.LogcatManager;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.log.L;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Created by Jason on 2023/11/11.
 */

public class AdvanceSettingsView extends DialogFrameLayout implements AdapterView.OnItemClickListener {

    private List<PreferenceAdapter.Data> mSettingsDataList = new ArrayList<>();
    private PreferenceAdapter mListAdapter;
    private ListView mListView;

    private static LogcatManager sLogcatManager;

    public AdvanceSettingsView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public AdvanceSettingsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AdvanceSettingsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        if (sLogcatManager == null) {
            File logFile = FileUtils.getSharableFile(context, "flog/" + context.getPackageName() + ".log");
            try {
                FileUtils.delete(logFile.getParentFile());
                logFile.getParentFile().mkdirs();
            } catch (Exception e) {
                L.e(e);
            }
            sLogcatManager = new LogcatManager(logFile);
            sLogcatManager.getTargetFile().deleteOnExit();
        }
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
        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_no_fingerprint_icon), Lang.getString(R.id.settings_sub_title_no_fingerprint_icon), true, Config.from(context).isShowFingerprintIcon()));
        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_use_biometric_api), Lang.getString(R.id.settings_sub_title_use_biometric_api), true, Config.from(context).isUseBiometricApi()));
        if (sLogcatManager.isRunning()) {
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_stop_logcat), Lang.getString(R.id.settings_sub_title_stop_logcat)));
        } else {
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_start_logcat), Lang.getString(R.id.settings_sub_title_start_logcat)));
        }
        mListAdapter = new PreferenceAdapter(mSettingsDataList);

        rootVerticalLayout.addView(lineView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(context, 2)));
        rootVerticalLayout.addView(mListView);

        this.addView(rootVerticalLayout);
    }

    @Override
    public String getDialogTitle() {
        return Lang.getString(R.id.settings_title_advance);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mListView.setAdapter(mListAdapter);
    }

    @Override
    public int dialogWindowHorizontalInsets() {
        return DpUtils.dip2px(getContext(), 13);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        PreferenceAdapter.Data data = mListAdapter.getItem(position);
        final Context context = getContext();
        final Config config = Config.from(context);
        if (Lang.getString(R.id.settings_title_no_fingerprint_icon).equals(data.title)) {
            data.selectionState = !data.selectionState;
            config.setShowFingerprintIcon(data.selectionState);
            mListAdapter.notifyDataSetChanged();
        } else if (Lang.getString(R.id.settings_title_use_biometric_api).equals(data.title)) {
            data.selectionState = !data.selectionState;
            config.setUseBiometricApi(data.selectionState);
            mListAdapter.notifyDataSetChanged();
        } else if (Lang.getString(R.id.settings_title_start_logcat).equals(data.title)) {
            sLogcatManager.startLogging(5 * 60 * 1000 /** 5min */);
            data.title = Lang.getString(R.id.settings_title_stop_logcat);
            data.subTitle = Lang.getString(R.id.settings_sub_title_stop_logcat);
            mListAdapter.notifyDataSetChanged();
            File logFile = sLogcatManager.getTargetFile();
            Toaster.showLong(String.format(Locale.getDefault(),
                    Lang.getString(R.id.toast_start_logging), logFile.getAbsoluteFile()));
        } else if (Lang.getString(R.id.settings_title_stop_logcat).equals(data.title)) {
            sLogcatManager.stopLogging();
            data.title = Lang.getString(R.id.settings_title_start_logcat);
            data.subTitle = Lang.getString(R.id.settings_sub_title_start_logcat);
            mListAdapter.notifyDataSetChanged();
            File logFile = sLogcatManager.getTargetFile();
            try {
                File logShareFile = new File(logFile.getParentFile(), context.getPackageName() + "-" + DateUtils.toString(new Date()).replaceAll("[: ]", "-")  + ".log");
                if (logFile.renameTo(logShareFile)) {
                    logFile = logShareFile;
                }
            } catch (Exception e) {
                L.e(e);
            }
            logFile.deleteOnExit();
            File finalLogFile = logFile;
            Task.onMain(500, () -> Toaster.showLong(String.format(Locale.getDefault(),
                    Lang.getString(R.id.toast_stop_logging), finalLogFile.getAbsoluteFile())));
            shareFile(logFile);
        }
    }

    private void shareFile(File targetFile) {
        try {
            Context context = getContext();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_STREAM, FileUtils.getUri(context, targetFile));
            context.startActivity(Intent.createChooser(intent, "Share File"));
        } catch (Exception e) {
            L.e(e);
        }
    }
}
