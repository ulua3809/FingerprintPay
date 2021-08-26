package com.surcumference.fingerprint.network.updateCheck;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.network.inf.UpdateResultListener;
import com.surcumference.fingerprint.network.updateCheck.github.GithubUpdateChecker;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.UrlUtils;
import com.surcumference.fingerprint.util.log.L;
import com.surcumference.fingerprint.view.UpdateInfoView;

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
                    updateInfoView.withOnPositiveButtonClickListener((dialogInterface, i) -> UrlUtils.openUrl(context, pageUrl));
                    updateInfoView.showInDialog();
                }
            }).doUpdateCheck();
        } catch (Exception | Error e) {
            //for OPPO R11 Plus 6.0 NoSuchFieldError: No instance field mResultListener
            L.e(e);
        }
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
}
