package com.surcumference.fingerprint.network.update.github;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.bean.UpdateInfo;
import com.surcumference.fingerprint.network.inf.UpdateResultListener;
import com.surcumference.fingerprint.network.update.BaseUpdateChecker;
import com.surcumference.fingerprint.network.update.github.bean.GithubAssetsInfo;
import com.surcumference.fingerprint.network.update.github.bean.GithubLatestInfo;
import com.surcumference.fingerprint.util.DateUtils;
import com.surcumference.fingerprint.util.StringUtils;
import com.surcumference.fingerprint.util.log.L;

import java.io.IOException;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Jason on 2017/9/9.
 */

public class GithubUpdateChecker extends BaseUpdateChecker {

    public static OkHttpClient sHttpClient = new OkHttpClient();
    private final String mLocalVersion;
    private final String mUpdateUrl;

    public GithubUpdateChecker(String localVersion, String updateUrl, UpdateResultListener listener) {
        super(listener);
        this.mLocalVersion = localVersion;
        this.mUpdateUrl = updateUrl;
    }

    @Override
    public void doUpdateCheck() {
        Callback callback;
        callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                onNetErr(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response != null && response.isSuccessful()) {
                    String replay = response.body().string();
                    response.close();
                    try {
                        GithubLatestInfo info = new Gson().fromJson(replay, GithubLatestInfo.class);
                        if (!info.isDataComplete()) {
                            onNetErr(new IllegalArgumentException("data not complete!"));
                            return;
                        }
                        if (BuildConfig.DEBUG || StringUtils.isAppNewVersion(mLocalVersion, info.version)) {
                            L.d("info", info);
                            String content = appendUpdateExtInfo(info.content, info.date, info.contentUrl);
                            L.d("content", content);
                            GithubAssetsInfo assetsInfo = info.getDownloadAssetsInfo();
                            UpdateInfo updateInfo = new UpdateInfo(info.version, content,
                                    info.contentUrl, assetsInfo.url, assetsInfo.name, assetsInfo.size);
                            onHasUpdate(updateInfo);
                        } else {
                            onNoUpdate();
                        }
                        return;
                    } catch (Exception e) {
                        L.d(e);
                    }
                }
                onNetErr(new IOException("response not successful. code: " + response.code()));
            }
        };

        Request request = new Request.Builder()
                .url(this.mUpdateUrl)
                .build();
        sHttpClient.newCall(request).enqueue(callback);
    }

    private String appendUpdateExtInfo(String content, Date date, String pageUrl) {
        StringBuilder sb = new StringBuilder(content);
        if (date != null) {
            sb.append("\n");
            if (TextUtils.isEmpty(pageUrl)) {
                sb.append("\n");
            } else {
                sb.append("<a href='");
                sb.append(pageUrl.replaceAll("http(s)*://", ""));
                sb.append("'>");
                sb.append(Lang.getString(R.id.goto_update_page));
                sb.append("</a>");
                sb.append("\n");
            }
            sb.append(Lang.getString(R.id.update_time)).append(": ").append(DateUtils.toString(date));
        }
        return sb.toString();
    }

}
