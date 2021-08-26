package com.surcumference.fingerprint.util;

import android.content.Context;
import android.content.Intent;

import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.util.log.L;

import org.json.JSONObject;

import java.net.URLEncoder;

/**
 * Created by Jason on 2017/9/10.
 */

public class DonateUtils {

    public static boolean openAlipayPayPage(Context context) {
        return openAlipayPayPage(context, Constant.DONATE_ID_ALIPAY);
    }

    public static boolean openAlipayPayPage(Context context, String qrcode) {
        try {
            qrcode = URLEncoder.encode(qrcode, "utf-8");
        } catch (Exception e) {
        }
        try {
            final String alipayqr = "alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=" + qrcode;
            openUri(context, alipayqr + "%3F_s%3Dweb-other&_t=" + System.currentTimeMillis());
            return true;
        } catch (Exception e) {
            L.e(e);
        }
        return false;
    }

    public static boolean openWeChatPay(Context context) {
        try {
            Intent donateIntent = new Intent();
            donateIntent.setClassName(context, "com.tencent.mm.plugin.remittance.ui.RemittanceAdapterUI");
            donateIntent.putExtra("scene", 1);
            donateIntent.putExtra("pay_channel", 13);
            donateIntent.putExtra("receiver_name", Constant.DONATE_ID_WECHAT);
            context.startActivity(donateIntent);
            return true;
        } catch (Exception e) {
            L.e(e);
        }
        return false;
    }

    public static boolean openQQPay(Context context) {
        Intent donateIntent = new Intent();
        donateIntent.putExtra("come_from", 5);
        JSONObject localJSONObject = new JSONObject();
        try {
            localJSONObject.put("targetUin", Constant.AUTHOR_QQ);
            localJSONObject.put("targetNickname", Constant.AUTHOR_QQ_NAME);
            localJSONObject.put("sign", "");
            localJSONObject.put("trans_fee", "");
            localJSONObject.put("source", "1");
            localJSONObject.put("desc", "");
            donateIntent.putExtra("extra_data", localJSONObject.toString());
            donateIntent.putExtra("app_info", "appid#20000001|bargainor_id#1000026901|channel#wallet");
            donateIntent.putExtra("callbackSn", "0");
            donateIntent.setClassName(context, "com.tencent.mobileqq.activity.qwallet.TransactionActivity");
            context.startActivity(donateIntent);
            return true;
        } catch (Exception e) {
            L.e(e);
        }
        return false;
    }

    private static void openUri(Context context, String s) throws Exception {
        Intent intent = Intent.parseUri(s, Intent.URI_INTENT_SCHEME);
        context.startActivity(intent);
    }
}
