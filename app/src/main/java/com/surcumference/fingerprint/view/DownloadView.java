package com.surcumference.fingerprint.view;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.StyleUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.log.L;

import java.io.File;

/**
 * Created by Jason on 2021/8/28.
 */
public class DownloadView extends DialogFrameLayout {

    private ProgressBar mProgressBar;
    private TextView mProgressText;

    public DownloadView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public DownloadView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DownloadView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        try {
            LinearLayout rootLinearLayout = new LinearLayout(context);
            rootLinearLayout.setOrientation(LinearLayout.VERTICAL);
            mProgressBar = initProgressBar(new ContextThemeWrapper(context, android.R.style.Theme_Material_NoActionBar_Fullscreen));
            mProgressBar.setBackgroundColor(0x20009688);
            int paddingH = DpUtils.dip2px(context, 20);
            rootLinearLayout.addView(mProgressBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(context, 4)));

            mProgressText = new TextView(context);
            StyleUtils.apply(mProgressText);
            mProgressText.setText("0%");
            mProgressText.setTextColor(0xFF757575);
            mProgressText.setPadding(0, DpUtils.dip2px(context, 5), 0,0);
            mProgressText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            rootLinearLayout.addView(mProgressText);

            LayoutParams rootParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rootParams.leftMargin = paddingH;
            rootParams.rightMargin = paddingH;
            rootParams.topMargin = DpUtils.dip2px(context, 20);
            rootParams.bottomMargin = 0;
            this.addView(rootLinearLayout, rootParams);
        } catch (Exception e) {
            L.e(e);
        }

        withPositiveButtonText(Lang.getString(R.id.cancel));
    }

    public DownloadView download(String url, File targetFile) {
        return download(url, targetFile, null);
    }

    public DownloadView download(String url, File targetFile, @Nullable Runnable onSuccessRunnable) {

        long startTime = SystemClock.uptimeMillis();
        targetFile.delete();
        OkGo.<File>get(url)//
                .tag(this)//
                .execute(new FileCallback(targetFile.getParent(), targetFile.getName()) {

                    @Override
                    public void onStart(Request<File, ? extends Request> request) {
                        L.d("正在下载中");
                    }

                    @Override
                    public void onSuccess(Response<File> response) {
                        L.d("下载完成");
                        if (onSuccessRunnable != null) {
                            onSuccessRunnable.run();
                        }
                        dismiss();
                    }

                    @Override
                    public void onError(Response<File> response) {
                        Throwable exception = response.getException();
                        String message = String.valueOf(exception.getMessage());
                        if (message.contains("CANCEL")) {
                            L.d("下载取消");
                            return;
                        }
                        L.d("下载出错", response.code(), exception);
                        new MessageView(getContext())
                                .title("下载出错")
                                .text(exception.getMessage())
                                .showInDialog();
                        dismiss();
                    }

                    @Override
                    public void downloadProgress(Progress progress) {
                        L.d(progress);
                        int percent = (int)(progress.fraction * 100);
                        long speed = progress.speed / 1000;
                        handleProgressChanged(percent);
                        TextView progressText = mProgressText;
                        if (progressText != null) {
                            progressText.setText(percent + "% - " + speed + "kb/s");
                        }
                    }
                });

        return this;
    }

    private void dismiss() {
        AlertDialog dialog = getDialog();
        if (dialog == null) {
           return;
        }
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private ProgressBar initProgressBar(Context context) {
        ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.BLUE, android.graphics.PorterDuff.Mode.MULTIPLY);
        progressBar.setBackgroundColor(Color.TRANSPARENT);
        return progressBar;
    }

    private void handleProgressChanged(int progress) {
        ProgressBar progressBar = mProgressBar;
        if (progress >= 100) {
            Task.onMain(1000, () -> {
                if (progressBar.getVisibility() != View.GONE) {
                    progressBar.setVisibility(View.GONE);
                }
                progressBar.setProgress(0);
            });
        } else {
            if (progressBar.getVisibility() != View.VISIBLE) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // will update the "progress" propriety of seekbar until it reaches progress
            ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", progress);
            animation.setDuration(600);
            animation.setInterpolator(new DecelerateInterpolator());
            animation.start();
        } else {
            progressBar.setProgress(progress);
        }
    }

    @Override
    public String getDialogTitle() {
        return Lang.getString(R.id.downloading);
    }

    @Override
    public AlertDialog showInDialog() {
        AlertDialog dialog = super.showInDialog();
        dialog.setCancelable(false);
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        OkGo.getInstance().cancelTag(this);
    }

}
