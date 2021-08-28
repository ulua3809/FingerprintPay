package com.surcumference.fingerprint.adapter;

import static com.surcumference.fingerprint.util.StyleUtils.TEXT_COLOR_SECONDARY;
import static com.surcumference.fingerprint.util.StyleUtils.TEXT_SIZE_SMALL;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.drawable.XDrawable;
import com.surcumference.fingerprint.view.smoothcompoundbutton.SmoothSwitch;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jason on 2017/9/9.
 */

public class PreferenceAdapter extends BaseAdapter {


    private final List<Data> mData;

    public PreferenceAdapter(List<Data> mData) {
        this.mData = new ArrayList<>(mData);
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Data getItem(int i) {
        return mData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = new ViewHolder(viewGroup.getContext()).itemView;
        }
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.bind(position, (Data) getItem(position));
        return view;
    }

    private class ViewHolder {

        private final LinearLayout itemView;
        private final TextView titleText;
        private final TextView subTitleText;
        private final SmoothSwitch selectBox;

        public ViewHolder(Context context) {
            itemView = new LinearLayout(context);
            itemView.setBackground(XDrawable.defaultDrawable());
            itemView.setOrientation(LinearLayout.VERTICAL);

            LinearLayout rootHorizontalLayout = new LinearLayout(context);
            rootHorizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
            rootHorizontalLayout.setWeightSum(1);
            rootHorizontalLayout.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout verticalLayout = new LinearLayout(context);
            verticalLayout.setOrientation(LinearLayout.VERTICAL);

            int defHPadding = DpUtils.dip2px(context, 5);

            titleText = new TextView(context);
            StyleUtils.apply(titleText);
            titleText.setPadding(0, 0, 0, 0);
            titleText.setGravity(Gravity.BOTTOM | Gravity.LEFT | Gravity.CENTER_VERTICAL);

            subTitleText = new TextView(context);
            StyleUtils.apply(subTitleText);
            subTitleText.setTextColor(TEXT_COLOR_SECONDARY);
            subTitleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_SMALL);
            subTitleText.setPadding(0, 0, 0, 0);
            subTitleText.setGravity(Gravity.TOP | Gravity.LEFT | Gravity.CENTER_VERTICAL);


            verticalLayout.setPadding(defHPadding, DpUtils.dip2px(context, 7), defHPadding, DpUtils.dip2px(context, 7));
            verticalLayout.addView(titleText);
            verticalLayout.addView(subTitleText);

            selectBox = new SmoothSwitch(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                selectBox.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));
                selectBox.setBackgroundTintMode(PorterDuff.Mode.SRC_OVER);
            }
            selectBox.setClickable(false);
            selectBox.setEnabled(false);
            selectBox.setAlpha(1);
            selectBox.setFocusable(false);
            selectBox.setFocusableInTouchMode(false);
            LinearLayout.LayoutParams selectBoxLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            selectBoxLayoutParams.setMargins(0,0,defHPadding,0);

            rootHorizontalLayout.addView(verticalLayout, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            rootHorizontalLayout.addView(selectBox, selectBoxLayoutParams);
            rootHorizontalLayout.setPadding(DpUtils.dip2px(context, 15), DpUtils.dip2px(context, 6), DpUtils.dip2px(context, 15), DpUtils.dip2px(context, 6));

            itemView.addView(rootHorizontalLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            itemView.setTag(this);
        }

        public void bind(int position, Data data) {
            if (data == null) {
                return;
            }
            int count = getCount();
            if (data.isSelection) {
                selectBox.setVisibility(View.VISIBLE);
            } else {
                selectBox.setVisibility(View.GONE);
            }

            selectBox.setChecked(data.selectionState);
            titleText.setText(data.title);
            subTitleText.setText(data.subTitle);
        }
    }

    public static class Data {

        public String title;
        public String subTitle;
        public boolean isSelection;
        public boolean selectionState;

        public Data(String title, String subTitle) {
            this(title, subTitle, false, false);
        }

        public Data(String title, String subTitle, boolean isSelection, boolean selectionState) {
            this.title = title;
            this.subTitle = subTitle;
            this.isSelection = isSelection;
            this.selectionState = selectionState;
        }
    }
}
