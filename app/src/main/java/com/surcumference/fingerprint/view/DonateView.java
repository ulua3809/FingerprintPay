package com.surcumference.fingerprint.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.adapter.PreferenceAdapter;
import com.surcumference.fingerprint.util.DonateUtils;
import com.surcumference.fingerprint.util.DpUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jason on 2017/9/9.
 */

public class DonateView extends DialogFrameLayout implements AdapterView.OnItemClickListener {

    private List<PreferenceAdapter.Data> mSettingsDataList = new ArrayList<>();
    private PreferenceAdapter mListAdapter;
    private ListView mListView;

    public DonateView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public DonateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DonateView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LinearLayout rootVerticalLayout = new LinearLayout(context);
        rootVerticalLayout.setOrientation(LinearLayout.VERTICAL);

        int defVPadding = DpUtils.dip2px(context, 12);

        mListView = new ListView(context);
        mListView.setDividerHeight(0);
        mListView.setOnItemClickListener(this);
        mListView.setPadding(0, defVPadding, 0, defVPadding);
        mListView.setDivider(new ColorDrawable(Color.TRANSPARENT));

        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_alipay), Constant.AUTHOR_ALIPAY));
        if (Constant.PACKAGE_NAME_WECHAT.equals(context.getPackageName())) {
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_wechat), Constant.AUTHOR_WECHAT));
        } else if (Constant.PACKAGE_NAME_QQ.equals(context.getPackageName())) {
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_qq), Constant.AUTHOR_QQ));
        }
        mListAdapter = new PreferenceAdapter(mSettingsDataList);

        rootVerticalLayout.addView(mListView);

        this.addView(rootVerticalLayout);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mListView.setAdapter(mListAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        PreferenceAdapter.Data data = mListAdapter.getItem(position);
        final Context context = getContext();
        if (Lang.getString(R.id.settings_title_alipay).equals(data.title)) {
            if (!DonateUtils.openAlipayPayPage(context)) {
                Toast.makeText(context, Lang.getString(R.id.toast_goto_donate_page_fail_alipay), Toast.LENGTH_LONG).show();
            }
        } else if (Lang.getString(R.id.settings_title_wechat).equals(data.title)) {
            if (!DonateUtils.openWeChatPay(context)) {
                Toast.makeText(context, Lang.getString(R.id.toast_goto_donate_page_fail_wechat), Toast.LENGTH_LONG).show();
            }
        } else if (Lang.getString(R.id.settings_title_qq).equals(data.title)) {
            if (!DonateUtils.openQQPay(context)) {
                Toast.makeText(context, Lang.getString(R.id.toast_goto_donate_page_fail_qq), Toast.LENGTH_LONG).show();
            }
        }
    }
}
