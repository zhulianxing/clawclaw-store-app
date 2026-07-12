package com.clawclaw.store;

/**
 * 产品信息模型 — 对应 MySQL products 表
 */
public class AppInfo {
    public int id;
    public String package_name;
    public String name;
    public String name_cn;
    public String icon_emoji;
    public String description;
    public double price;          // 人民币
    public String apk_url;
    public String version;
    public String platform;
    public String apk_size;
    public int trial_hours;
    public int active;

    /** 价格显示 */
    public String getPriceDisplay() {
        if (price == 0) return "免费";
        return "¥" + String.format("%.0f", price);
    }

    /** APK 下载链接 */
    public String getApkUrl() {
        return apk_url != null ? apk_url : "";
    }

    /** 试用说明 */
    public String getTrialText() {
        if (trial_hours <= 0) return "";
        if (trial_hours >= 168) return trial_hours / 24 + "天免费试用";
        return trial_hours + "小时免费试用";
    }
}
