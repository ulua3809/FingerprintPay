package com.surcumference.fingerprint.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.UrlUtils;
import com.surcumference.fingerprint.util.log.L;

/**
 * Created by Jason on 2017/11/18.
 */

public class LicenseView extends DialogFrameLayout {

    private ProgressBar mProgressBar;

    public LicenseView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public LicenseView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LicenseView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        try {
            mProgressBar = initProgressBar(new ContextThemeWrapper(context, android.R.style.Theme_Material_NoActionBar_Fullscreen));
            WebView webView = initWebView(context);
            webView.loadUrl(Constant.HELP_URL_LICENSE);
            this.setMinimumHeight(DpUtils.dip2px(context, 200));
            this.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            this.addView(mProgressBar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(context, 4)));
        } catch (Exception e) {
            L.e(e);
        }
        withNegativeButtonText(Lang.getString(R.id.disagree));
        withPositiveButtonText(Lang.getString(R.id.agree));
    }

    private ProgressBar initProgressBar(Context context) {
        ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.BLUE, android.graphics.PorterDuff.Mode.MULTIPLY);
        progressBar.setBackgroundColor(0x20009688);
        return progressBar;
    }

    private WebView initWebView(Context context) throws Exception {
        WebView webView = new WebView(context);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (TextUtils.isEmpty(url)) {
                    return super.shouldOverrideUrlLoading(view, url);
                }
                String lurl = url.toLowerCase();
                if (lurl.startsWith("http://") || lurl.startsWith("https://")) {
                    if (lurl.endsWith(".apk") || lurl.endsWith(".zip") || lurl.endsWith(".tar.gz") || lurl.contains("pan.baidu.com/s/")) {
                        UrlUtils.openUrl(context, url);
                        return true;
                    }
                    view.loadUrl(url);
                    return true;
                }
                UrlUtils.openUrl(context, url);
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                handleProgressChanged(newProgress);
            }
        });
        return webView;
    }

    private  void handleProgressChanged(int progress) {
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

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
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
        return Lang.getString(R.id.settings_title_license);
    }
}
