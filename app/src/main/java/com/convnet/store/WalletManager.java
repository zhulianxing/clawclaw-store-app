package com.convnet.store;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 钱包管理 — 管理本地私钥 (加密存储) + 链上交互
 * 使用 WalletConnect 方式或本地私钥签名
 */
public class WalletManager {

    private static final String PREFS_NAME = "convnet_wallet";
    private static final String KEY_ADDRESS = "wallet_address";
    private static final String KEY_PRIVATE = "wallet_private_key"; // 加密存储
    private static WalletManager instance;
    private final SharedPreferences prefs;

    private WalletManager(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized WalletManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new WalletManager(ctx);
        }
        return instance;
    }

    /** 是否有钱包 */
    public boolean hasWallet() {
        return prefs.contains(KEY_ADDRESS);
    }

    /** 获取钱包地址 */
    public String getAddress() {
        return prefs.getString(KEY_ADDRESS, null);
    }

    /** 保存钱包 (地址 + 私钥) */
    public void saveWallet(String address, String privateKey) {
        prefs.edit()
                .putString(KEY_ADDRESS, address)
                .putString(KEY_PRIVATE, privateKey) // TODO: 用 Android Keystore 加密
                .apply();
    }

    /** 获取私钥 (用于签名交易) */
    public String getPrivateKey() {
        return prefs.getString(KEY_PRIVATE, null);
    }

    /** 清除钱包 */
    public void clearWallet() {
        prefs.edit().clear().apply();
    }

    /**
     * 从私钥推导地址 (简单实现，不依赖 Web3j)
     * 实际使用时通过 RPC 发送交易自动暴露地址
     */
    public static String addressFromPrivateKey(String privateKey) {
        // 简化：用户导入私钥后，通过 eth_getTransactionCount 验证
        // 实际实现需要 secp256k1 椭圆曲线运算
        // 这里返回 null，由实际签名时确定
        return null;
    }
}
