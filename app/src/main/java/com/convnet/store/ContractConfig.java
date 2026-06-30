package com.convnet.store;

/**
 * 链上合约配置 — Polygon 主网
 */
public class ContractConfig {

    // Polygon 主网
    public static final int CHAIN_ID = 137;
    public static final String CHAIN_NAME = "Polygon";
    public static final String RPC_URL = "https://polygon-bor-rpc.publicnode.com";
    public static final String EXPLORER_URL = "https://polygonscan.com";

    // 合约地址
    public static final String DEVELOPER_REGISTRY = "0x8EE775e943C31EA7c4eF2B7bb83e0651c9d04001";
    public static final String APP_REGISTRY = "0xdFCFA7d871cCc3674d873d00192Ba26a685B2b68";
    public static final String LICENSE_NFT = "0x7Ed65226C66b188f66AA0e5483917B1C33a41225";
    public static final String USDC = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359";

    // 平台 APK 下载基地址
    public static final String APK_BASE_URL = "https://clawclaw.tech/apk";

    // APP 图标 emoji 映射
    public static String getIconForPackage(String pkg) {
        switch (pkg) {
            case "com.storeguard.vipreception": return "🤵";
            case "com.guardianfind.missingperson": return "👁️";
            case "com.lossguard.shopsecurity": return "🛡️";
            case "com.attendanceguard.clock": return "🕐";
            case "com.visitorcount.analysis": return "📊";
            case "com.golfranger.distancemeter": return "⛳";
            case "com.falldetect.guardian": return "🛡️";
            case "com.elderguard.care": return "👴";
            case "com.localguard.pedestriancapture": return "📸";
            case "com.dogathome": return "🐕";
            default: return "📦";
        }
    }

    // APP 中文名称映射
    public static String getNameCn(String pkg) {
        switch (pkg) {
            case "com.storeguard.vipreception": return "VIP迎宾";
            case "com.guardianfind.missingperson": return "走失守护";
            case "com.lossguard.shopsecurity": return "商铺防损";
            case "com.attendanceguard.clock": return "刷脸考勤";
            case "com.visitorcount.analysis": return "客流统计";
            case "com.golfranger.distancemeter": return "高尔夫测距";
            case "com.falldetect.guardian": return "跌倒守护";
            case "com.elderguard.care": return "长者守护";
            case "com.localguard.pedestriancapture": return "单点抓拍";
            case "com.dogathome": return "狗狗在家";
            default: return "未知应用";
        }
    }

    // APP 描述映射
    public static String getDescCn(String pkg) {
        switch (pkg) {
            case "com.storeguard.vipreception": return "VIP客户识别迎宾系统，自动识别到店VIP客户并通知员工";
            case "com.guardianfind.missingperson": return "走失人员搜寻守护，基于AI视觉帮助寻找走失人员";
            case "com.lossguard.shopsecurity": return "商铺防损监控，实时检测异常行为防盗防损";
            case "com.attendanceguard.clock": return "刷脸考勤打卡，无接触式人脸识别考勤系统";
            case "com.visitorcount.analysis": return "客流量统计分析，实时统计进出客流并生成报表";
            case "com.golfranger.distancemeter": return "高尔夫测距助手，通过摄像头精准测量距离";
            case "com.falldetect.guardian": return "跌倒检测守护，自动检测老人跌倒并报警";
            case "com.elderguard.care": return "长者守护系统，全方位关爱老人安全";
            case "com.localguard.pedestriancapture": return "单点行人抓拍，指定区域行人检测与抓拍";
            case "com.dogathome": return "离线AI焦虑监测，24h自学习基线，双条件触发自动安抚";
            default: return "";
        }
    }

    // APK 文件名映射 (与服务器实际文件名一致)
    public static String getApkName(String pkg) {
        switch (pkg) {
            case "com.storeguard.vipreception": return "storeguard.apk";
            case "com.guardianfind.missingperson": return "guardianfind.apk";
            case "com.lossguard.shopsecurity": return "lossguard.apk";
            case "com.attendanceguard.clock": return "attendanceguard.apk";
            case "com.visitorcount.analysis": return "visitorcount.apk";
            case "com.golfranger.distancemeter": return "golfranger.apk";
            case "com.falldetect.guardian": return "falldetect.apk";
            case "com.elderguard.care": return "elderguard.apk";
            case "com.localguard.pedestriancapture": return "pedestriancapture.apk";
            case "com.dogathome": return "dogathome-v1.0.0.apk";
            default: return "unknown.apk";
        }
    }
}
