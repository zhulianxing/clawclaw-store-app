package com.convnet.store;

import java.math.BigInteger;

/**
 * APP 信息模型
 */
public class AppInfo {
    public long appId;
    public String developer;
    public String name;
    public String packageName;
    public String description;
    public String apkURI;
    public String iconURI;
    public BigInteger price;     // USDC 6 decimals
    public long trialDays;
    public String version;
    public long createdAt;
    public long updatedAt;
    public boolean active;

    /** 获取价格 (USDC) */
    public String getPriceUsdc() {
        if (price == null) return "0";
        long val = price.longValue();
        return String.format("%d.%06d", val / 1000000, val % 1000000).replaceAll("0+$", "").replaceAll("\\.$", ".0");
    }

    /** 简单格式化价格 — 使用 BigDecimal 避免科学计数法 */
    public String getPriceDisplay() {
        if (price == null) return "0 USDC";
        java.math.BigDecimal bd = new java.math.BigDecimal(price)
                .divide(new java.math.BigDecimal("1000000"))
                .stripTrailingZeros();
        String str = bd.toPlainString();
        if (str.equals("0")) str = "0";
        return str + " USDC";
    }

    /** 获取中文名 */
    public String getLocalName() {
        return ContractConfig.getNameCn(packageName);
    }

    /** 获取中文描述 */
    public String getLocalDesc() {
        return ContractConfig.getDescCn(packageName);
    }

    /** 获取图标 emoji */
    public String getIcon() {
        return ContractConfig.getIconForPackage(packageName);
    }

    /** 获取 APK 下载 URL */
    public String getApkUrl() {
        String apkName = ContractConfig.getApkName(packageName);
        return ContractConfig.APK_BASE_URL + "/" + apkName;
    }

    /** 获取开发者短地址 */
    public String getShortDeveloper() {
        if (developer == null || developer.length() < 10) return developer;
        return developer.substring(0, 6) + "..." + developer.substring(developer.length() - 4);
    }
}
