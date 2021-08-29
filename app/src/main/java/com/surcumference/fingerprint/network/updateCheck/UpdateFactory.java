package com.surcumference.fingerprint.network.updateCheck;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.network.inf.UpdateResultListener;
import com.surcumference.fingerprint.network.updateCheck.github.GithubUpdateChecker;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.FileUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.view.DownloadView;
import com.surcumference.fingerprint.view.MessageView;
import com.surcumference.fingerprint.view.UpdateInfoView;

import java.io.File;

/**
 * Created by Jason on 2017/9/10.
 */

public class UpdateFactory {

    public static void doUpdateCheck(final Context context) {
        doUpdateCheck(context, true, false);
    }

    public static void doUpdateCheck(final Context context, final boolean quite, final boolean dontSkip) {
        if (!quite) {
            Toast.makeText(context, Lang.getString(R.id.toast_checking_update), Toast.LENGTH_LONG).show();
        }
        try {
            File targetFile = FileUtils.getSharableFile(context, BuildConfig.APP_PRODUCT_NAME + ".apk");
            FileUtils.delete(targetFile);
            new GithubUpdateChecker(new UpdateResultListener() {
                @Override
                public void onNoUpdate() {
                    if (!quite) {
                        Toast.makeText(context, Lang.getString(R.id.toast_no_update), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onNetErr() {
                    if (!quite) {
                        Toast.makeText(context, Lang.getString(R.id.toast_check_update_fail_net_err), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onHasUpdate(final String version, String content, final String pageUrl, String downloadUrl) {
                    if (!dontSkip) {
                        if (isSkipVersion(context, version)) {
                            L.d("已跳過版本: " + version);
                            return;
                        }
                    }
                    UpdateInfoView updateInfoView = new UpdateInfoView(context);
                    updateInfoView.setTitle(Lang.getString(R.id.found_new_version) + version);
                    updateInfoView.setContent(content);
                    updateInfoView.withOnNeutralButtonClickListener((dialogInterface, i) -> {
                        Config.from(context).setSkipVersion(version);
                        dialogInterface.dismiss();
                    });
                    updateInfoView.withOnPositiveButtonClickListener((dialogInterface, i) -> {
                        new DownloadView(context)
                                .download(downloadUrl, targetFile, () -> {
                                    UpdateFactory.installApk(context, targetFile);
                                    dialogInterface.dismiss();
                                    new MessageView(context).text(Lang.getString(R.id.update_success_note)).showInDialog();
                                }).showInDialog();
                    });
                    updateInfoView.showInDialog();
                }
            }).doUpdateCheck();
        } catch (Exception | Error e) {
            //for OPPO R11 Plus 6.0 NoSuchFieldError: No instance field mResultListener
            L.e(e);
        }
    }

    public static void lazyUpdateWhenActivityAlive() {
        int lazyCheckTimeMsec = BuildConfig.DEBUG ? 200 : 6000;
        Task.onMain(lazyCheckTimeMsec, new Runnable() {
            @Override
            public void run() {
                Activity activity = ApplicationUtils.getCurrentActivity();
                L.d("top activity", activity);
                if (activity == null) {
                    Task.onMain(lazyCheckTimeMsec, this);
                    return;
                }
                UpdateFactory.doUpdateCheck(activity);
            }
        });
    }

    private static boolean isSkipVersion(Context context, String targetVersion) {
        Config config = Config.from(context);
        String skipVersion = config.getSkipVersion();
        if (TextUtils.isEmpty(skipVersion)) {
            return false;
        }
        if (String.valueOf(targetVersion).equals(skipVersion)) {
            return true;
        }
        return false;
    }

    public static void installApk(Context context, File file) {
        Uri uri = FileUtils.getUri(context, file);
        file.setReadable(true, false);
        file.getParentFile().setReadable(true, false);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uri,"application/vnd.android.package-archive");
        Task.onMain(() -> context.startActivity(intent));
    }
}
