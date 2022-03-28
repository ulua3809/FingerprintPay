package com.surcumference.fingerprint.view;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.log.L;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by Jason on 2021/8/30.
 */
public class ShellExecuteView extends DialogFrameLayout {

    private ScrollView mMessageScrollView;
    private TextView mMessageText;
    private String mTitle;

    private Shell.OnSyncCommandLineListener mSyncCommandLineListener = new Shell.OnSyncCommandLineListener() {
        @Override
        public void onSTDERR(String line) {
            L.e(line);
            Task.onMain(() -> appendCommandLineOutput(line));
        }

        @Override
        public void onSTDOUT(String line) {
            L.d(line);
            Task.onMain(() -> appendCommandLineOutput(line));
        }
    };

    public ShellExecuteView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ShellExecuteView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ShellExecuteView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mMessageScrollView = new ScrollView(new ContextThemeWrapper(context, android.R.style.Theme_Material_NoActionBar_Fullscreen));
        mMessageScrollView.setBackgroundColor(Color.BLACK);
        mMessageText = new TextView(context);
        int paddingH = DpUtils.dip2px(context, 20);
        mMessageText.setPadding(paddingH, DpUtils.dip2px(context, 5), paddingH, 0);
        mMessageText.setTextColor(Color.WHITE);
        mMessageScrollView.addView(mMessageText);
        this.addView(mMessageScrollView,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(context, 300)));
        withPositiveButtonText(Lang.getString(R.id.ok));
    }

    public ShellExecuteView execute(String command) {
        executeCommand(command);
        return this;
    }

    @WorkerThread
    public int executeCommand(String command) {
        try {
            return Shell.Pool.SU.run(command, mSyncCommandLineListener);
        } catch (Exception e) {
            L.e(e);
            return -1;
        }
    }

    public void appendCommandLineOutput(CharSequence s) {
        TextView textView = mMessageText;
        if (textView == null) {
            return;
        }
        textView.append(s);
        textView.append("\n");
        Task.onMain(()-> mMessageScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    public ShellExecuteView text(CharSequence s) {
        mMessageText.setText(s);
        return this;
    }

    public ShellExecuteView title(String s) {
        mTitle = s;
        return this;
    }

    @Override
    public String getDialogTitle() {
        String title = mTitle;
        return TextUtils.isEmpty(title) ? Lang.getString(R.string.app_name) : title;
    }

    @Override
    public AlertDialog showInDialog() {
        AlertDialog dialog = super.showInDialog();
        dialog.setCancelable(false);
        return dialog;
    }
}
