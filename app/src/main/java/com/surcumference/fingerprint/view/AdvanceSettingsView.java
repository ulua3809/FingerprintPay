package com.surcumference.fingerprint.view;

import android.content.Context;
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

import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.adapter.PreferenceAdapter;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DpUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Jason on 2023/11/11.
 */

public class AdvanceSettingsView extends DialogFrameLayout implements AdapterView.OnItemClickListener {

    private List<PreferenceAdapter.Data> mSettingsDataList = new ArrayList<>();
    private PreferenceAdapter mListAdapter;
    private ListView mListView;

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
        }
    }

}
