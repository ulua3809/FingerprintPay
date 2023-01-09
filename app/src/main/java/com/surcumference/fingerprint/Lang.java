package com.surcumference.fingerprint;

import java.util.Locale;

/**
 * Created by Jason on 2017/9/17.
 */

public class Lang {

    private static int sLang;

    public static final int LANG_ZH_CN = 0;
    public static final int LANG_ZH_TW = 1;
    public static final int LANG_EN = 2;

    static {
        Locale locale = Locale.getDefault();
        if (locale.getLanguage().toLowerCase().contains("zh")) {
            String country = locale.getCountry().toLowerCase();
            if (country.contains("tw") || country.contains("hk")) {
                sLang = LANG_ZH_TW;
            } else {
                sLang = LANG_ZH_CN;
            }
        } else {
            sLang = LANG_EN;
        }
    }

    public static String getString(int res) {
        switch (res) {
            case R.string.app_name:
                return tr("指纹支付", "指纹支付", "Fingerprint Pay");
            case R.id.settings_title_help_wechat:
                return tr("微信指纹", "微信指纹", "WeChat fingerprint pay");
            case R.id.settings_title_help_alipay:
                return tr("支付宝指纹", "支付寶指纹", "Alipay fingerprint pay");
            case R.id.settings_title_help_taobao:
                return tr("淘宝指纹", "淘宝指纹", "Taobao fingerprint pay");
            case R.id.settings_title_help_qq:
                return tr("QQ指纹", "QQ指纹", "QQ fingerprint pay");
            case R.id.settings_title_help_unionpay:
                return tr("云闪付指纹", "雲閃付指纹", "UnionPay fingerprint pay");
            case R.id.settings_title_qq_group:
                return tr("QQ交流群", "QQ交流群", "QQ Group");
            case R.id.settings_title_help_faq:
                return tr("常见问题", "常見問題", "FAQ");
            case R.id.settings_title_license:
                return tr("许可协议", "許可協議", "License");
            case R.id.settings_title_checkupdate:
                return tr("检查更新", "檢查更新", "Check for update");
            case R.id.settings_title_webside:
                return tr("项目主页", "項目主頁", "Project homepage");
            case R.id.settings_title_version:
                return tr("当前版本", "当前版本", "Version");
            case R.id.settings_sub_title_help_wechat:
                return tr("查看使用教程", "查看使用教程", "Tutorial");
            case R.id.settings_sub_title_help_alipay:
                return tr("查看使用教程", "查看使用教程", "Tutorial");
            case R.id.settings_sub_title_help_taobao:
                return tr("查看使用教程", "查看使用教程", "Tutorial");
            case R.id.settings_sub_title_help_qq:
                return tr("查看使用教程", "查看使用教程", "Tutorial");
            case R.id.settings_sub_title_help_unionpay:
                return tr("查看使用教程", "查看使用教程", "Tutorial");
            case R.id.settings_sub_title_qq_group:
                return tr("665167891", "665167891", "665167891");
            case R.id.settings_sub_title_help_faq:
                return tr("出现问题请看这里", "出現問題請看這裏", "Having a problem?");
            case R.id.settings_sub_title_license:
                return tr("查看许可协议", "查看許可協議", "Check the License Agreement");
            case R.id.settings_sub_title_checkupdate:
                return tr("点击检查软件更新", "點擊檢查软件更新", "Press to begin");
            case R.id.settings_sub_title_webside:
                return tr("访问項目主页", "訪問項目主頁", "Home page");
            case R.id.found_new_version:
                return tr("发现新版本", "發現新版本 ", "New version: ");
            case R.id.skip_this_version:
                return tr("跳过这个版本", "跳過這個版本 ", "Skip");
            case R.id.cancel:
                return tr("取消", "取消", "Cancel");
            case R.id.goto_update_page:
                return tr("前往更新页", "前往更新頁 ", "Update page");
            case R.id.goto_update_page_mirror:
                return tr("(国内镜像)", "(國內鏡像) ", "(Mirror)");
            case R.id.update_now:
                return tr("立即更新", "立即更新 ", "UPDATE");
            case R.id.update_success_note:
                return tr("安装更新后, 重启手机使插件生效", "安裝更新後, 重啟手機使插件生效 ",
                        "After installing the update, restart the phone to enable the plug-in");
            case R.id.downloading:
                return tr("下载中", "下載中", "Downloading");
            case R.id.ok:
                return tr("确定", "确定", "OK");
            case R.id.settings_title_taobao:
                return tr("淘宝", "淘寶", "Taobao");
            case R.id.settings_title_alipay:
                return tr("支付宝", "支付寶", "Alipay");
            case R.id.settings_title_wechat:
                return tr("微信", "微信", "WeChat");
            case R.id.settings_title_qq:
                return tr("腾讯QQ", "騰訊QQ", "Tencent QQ");
            case R.id.settings_title_unionpay:
                return tr("云闪付", "雲閃付", "Union Pay");
            case R.id.enter_password:
                return tr("使用密码", "使用密碼", "Enter password");
            case R.id.settings_title_switch:
                return tr("启用", "啟用", "Enable");
            case R.id.settings_title_password:
                return tr("密码", "密碼", "Password");
            case R.id.settings_title_donate:
                return tr("赞助我", "贊助我", "Donate me");
            case R.id.settings_sub_title_switch_alipay:
                return tr("启用支付宝指纹支付", "啟用支付宝指紋支付", "Enable fingerprint payment for Alipay");
            case R.id.settings_sub_title_switch_wechat:
                return tr("启用微信指纹支付", "啟用微信指紋支付", "Enable fingerprint payment for WeChat");
            case R.id.settings_sub_title_switch_qq:
                return tr("启用QQ指纹支付", "啟用QQ指紋支付", "Enable fingerprint payment for QQ");
            case R.id.settings_sub_title_switch_unionpay:
                return tr("启用云闪付指纹支付", "啟用雲閃付指紋支付", "Enable fingerprint payment for Union Pay");
            case R.id.settings_sub_title_password_alipay:
                return tr("请输入支付宝的支付密码, 密码会加密后保存, 请放心", "請輸入支付宝的支付密碼, 密碼會加密后保存, 請放心", "Please enter your Payment password");
            case R.id.settings_sub_title_password_wechat:
                return tr("请输入微信的支付密码, 密码会加密后保存, 请放心", "請輸入微信的支付密碼, 密碼會加密后保存, 請放心", "Please enter your Payment password");
            case R.id.settings_sub_title_password_qq:
                return tr("请输入QQ的支付密码, 密码会加密后保存, 请放心", "請輸入QQ的支付密碼, 密碼會加密后保存, 請放心", "Please enter your Payment password");
            case R.id.settings_sub_title_password_unionpay:
                return tr("请输入云闪付的支付密码, 密码会加密后保存, 请放心", "請輸入雲閃付的支付密碼, 密碼會加密后保存, 請放心", "Please enter your Payment password");
            case R.id.settings_sub_title_donate:
                return tr("如果您觉得本软件好用, 欢迎赞助, 多少都是心意", "如果您覺得本軟件好用, 歡迎贊助, 多少都是心意", "Donate me, If you like this project");
            case R.id.settings_sub_title_update_modules_same_time:
                return tr("将同时升级以下模块", "將同時升級以下模塊", "The following modules will be upgraded at the same time");
            case R.id.fingerprint_verification:
                return tr("请验证指纹", "請驗證指紋", "Fingerprint verification");
            case R.id.wechat_general:
                return tr("通用", "一般", "General");
            case R.id.app_settings_name:
                return tr("指纹设置", "指紋設置", "Fingerprint");
            case R.id.wechat_payview_fingerprint_title:
                return tr("请验证指纹", "請驗證指紋", "Verify fingerprint");
            case R.id.wechat_payview_password_title:
                return tr("请输入支付密码", "請輸入付款密碼", "Enter payment password");
            case R.id.wechat_payview_password_switch_text:
                return tr("使用密码", "使用密碼", "Password");
            case R.id.wechat_payview_fingerprint_switch_text:
                return tr("使用指纹", "使用指紋", "Fingerprint");
            case R.id.qq_payview_fingerprint_title:
                return tr("请验证指纹", "請驗證指紋", "Verify fingerprint");
            case R.id.qq_payview_password_title:
                return tr("请输入支付密码", "請輸入付款密碼", "Enter payment password");
            case R.id.qq_payview_password_switch_text:
                return tr("使用密码", "使用密碼", "Password");
            case R.id.qq_payview_fingerprint_switch_text:
                return tr("使用指纹", "使用指紋", "Fingerprint");
            case R.id.disagree:
                return tr("不同意", "不同意", "Disagree");
            case R.id.agree:
                return tr("同意", "同意", "I agree");
            case R.id.update_time:
                return tr("更新日期", "更新日期", "Update time");
            case R.id.update_no_root:
                return tr("未获取到ROOT权限, 无法进行自动更新, 请前往更新页面手动获取更新", "未獲取到ROOT權限, 無法進行自動更新, 請前往更新頁面手動獲取更新", "Update failed, no root permission, please go to update page to manually obtain the updates");
            case R.id.update_at_least_select_one:
                return tr("请至少少选择一项", "請至少少選擇一項", "Please select at least one item");
            case R.id.update_file_corrupted:
                return tr("文件损坏了, 请重试", "文件損壞了, 請重試", "File is corrupted, please try again");
            case R.id.update_file_missing:
                return tr("更新文件丢失, 请前往更新页面手动获取更新", "更新文件丟失, 請前往更新頁面手動獲取更新", "Update file is missing, please go to update page to manually obtain the updates");
            case R.id.update_installation_failed:
                return tr("更新安装失败, 错误码:", "更新安裝失敗, 錯誤碼:", "Update installation failed, error code:");

            case R.id.toast_give_me_star:
                return tr("如果您拥有Github账户, 别忘了给我的项目+个Star噢", "如果您擁有Github賬戶, 別忘了給我的項目+個Star噢", "Give me a star, if you like this project");
            case R.id.toast_checking_update:
                return tr("正在检查更新", "正在檢查更新", "Checking");
            case R.id.toast_no_update:
                return tr("已经是最新版本了", "暫無更新", "You already have the latest version");
            case R.id.toast_check_update_fail_net_err:
                return tr("网络错误, 检查更新失败", "網絡錯誤, 檢查更新失敗", "Network error");
            case R.id.toast_fingerprint_not_match:
                return tr("指纹识别失败", "指紋識別失敗", "Fingerprint NOT MATCH");
            case R.id.toast_fingerprint_retry_ended:
                return tr("多次尝试错误，请使用密码输入", "多次嘗試錯誤，請使用密碼輸入", "Too many incorrect verification attempts, switch to password verification");
            case R.id.toast_fingerprint_unlock_reboot:
                return tr("系统限制，重启后必须验证密码后才能使用指纹验证", "系統限制，重啟後必須驗證密碼後才能使用指紋驗證", "Reboot and enable fingerprint verification with your PIN");
            case R.id.toast_fingerprint_not_enable:
                return tr("系统指纹功能未启用", "系統指紋功能未啟用", "Fingerprint verification has been closed by system");
            case R.id.toast_password_not_set_alipay:
                return tr("未设定支付密码，请前往設置->指紋設置中设定支付宝的支付密码", "未設定支付密碼，請前往設置 -> 指紋設置中設定支付寶的支付密碼", "Payment password not set, please goto Settings -> Fingerprint to enter you payment password");
            case R.id.toast_password_not_set_taobao:
                return tr("未设定支付密码，请前往設置->指紋設置中设定淘宝的支付密码", "未設定支付密碼，請前往設置 -> 指紋設置中設定淘寶的支付密碼", "Payment password not set, please goto Settings -> Fingerprint to enter you payment password");
            case R.id.toast_password_not_set_wechat:
                return tr("未设定支付密码，请前往設置->指紋設置中设定微信的支付密码", "未設定支付密碼，請前往設置 -> 指紋設置中設定微信的支付密碼", "Payment password not set, please goto Settings -> Fingerprint to enter you payment password");
            case R.id.toast_password_not_set_qq:
                return tr("未设定支付密码，请前往設置->指紋設置中设定QQ的支付密码", "未設定支付密碼，請前往設置 -> 指紋設置中設定QQ的支付密碼", "Payment password not set, please goto Settings -> Fingerprint to enter you payment password");
            case R.id.toast_password_not_set_generic:
                return tr("未设定支付密码，请前往設置->指紋設置中设定支付密码", "未設定支付密碼，請前往設置 -> 指紋設置中設定支付密碼", "Payment password not set, please goto Settings -> Fingerprint to enter you payment password");
            case R.id.toast_password_auto_enter_fail:
                return tr("Oops.. 输入失败了. 请手动输入密码", "Oops.. 輸入失敗了. 請手動輸入密碼", "Oops... auto input failure, switch to manual input");
            case R.id.toast_goto_donate_page_fail_alipay:
                return tr("调用支付宝捐赠页失败, 您可以手动转账捐赠哦, 帐号: " + Constant.AUTHOR_ALIPAY, "調用支付寶捐贈頁失敗, 您可以手動轉賬捐贈哦, 帳號: " + Constant.AUTHOR_ALIPAY, "Can't jump to Alipay donate page, You can do it manually by transfer to account: " + Constant.AUTHOR_ALIPAY);
            case R.id.toast_goto_donate_page_fail_wechat:
                return tr("调用微信捐赠页失败, 您可以手动转账捐赠哦, 帐号: " + Constant.AUTHOR_WECHAT, "調用微信捐贈頁失敗, 您可以手動轉賬捐贈哦, 帳號: " + Constant.AUTHOR_WECHAT, "Can't jump to WeChat donate page, You can do it manually by transfer to account: " + Constant.AUTHOR_WECHAT);
            case R.id.toast_goto_donate_page_fail_qq:
                return tr("调用QQ捐赠页失败, 您可以手动转账捐赠哦, 帐号: " + Constant.AUTHOR_QQ, "調用QQ捐贈頁失敗, 您可以手動轉賬捐贈哦, 帳號: " + Constant.AUTHOR_QQ, "Can't jump to QQ donate page, You can do it manually by transfer to account: " + Constant.AUTHOR_QQ);
            case R.id.toast_need_qq_7_2_5:
                return tr("您的QQ版本过低, 不支持指纹功能, 请升级至7.2.5以上的版本", "您的QQ版本過低, 不支持指紋功能, 請升級至7.2.5以上的版本", "Your QQ version is too low, does not support the fingerprint function, please upgrade to version 7.2.5 and above");
            case R.id.template:
                return tr("", "", "");
        }
        return "";
    }

    private static String tr(String ...c) {
        return c[sLang];
    }
}
