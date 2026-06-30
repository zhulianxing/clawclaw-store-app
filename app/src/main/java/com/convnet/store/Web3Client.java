package com.convnet.store;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Web3 RPC 客户端 — 直接调用 Polygon JSON-RPC，不依赖 ethers 库
 * 支持只读查询 + WalletConnect 签名交易
 */
public class Web3Client {

    private static final String TAG = "Web3Client";
    private final OkHttpClient client;
    private final String rpcUrl;
    private int nextId = 1;

    public Web3Client(String rpcUrl) {
        this.rpcUrl = rpcUrl;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    // ===== RPC 调用 =====

    private String rpcCall(String method, JSONArray params) throws Exception {
        JSONObject req = new JSONObject();
        req.put("jsonrpc", "2.0");
        req.put("id", nextId++);
        req.put("method", method);
        req.put("params", params);

        RequestBody body = RequestBody.create(req.toString(),
                MediaType.parse("application/json"));
        Request request = new Request.Builder().url(rpcUrl).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            String respStr = response.body().string();
            JSONObject resp = new JSONObject(respStr);
            if (resp.has("error")) {
                throw new Exception("RPC error: " + resp.get("error"));
            }
            return resp.getString("result");
        }
    }

    // ===== eth_call (只读合约调用) =====

    private String ethCall(String to, String data) throws Exception {
        JSONObject callObj = new JSONObject();
        callObj.put("to", to);
        callObj.put("data", data);
        JSONArray params = new JSONArray();
        params.put(callObj);
        params.put("latest");
        return rpcCall("eth_call", params);
    }

    // ===== 编码工具 =====

    /** 编码函数选择器 (4 bytes) */
    public static String selector(String sig) {
        // 简化实现：用预计算的 keccak256 前4字节
        // 实际使用中预存常见函数签名
        return keccak256Selector(sig);
    }

    private static String keccak256Selector(String sig) {
        // 正确的 keccak256 前4字节选择器 (通过 ethers.js 计算)
        switch (sig) {
            case "nextAppId()": return "0x38226c2a";
            case "developerCount()": return "0x92a7a197";
            case "totalSupply()": return "0x18160ddd";
            case "activeAppIds(uint256)": return "0xfbbc7d85";
            case "apps(uint256)": return "0x61acc37e";
            case "isRegistered(address)": return "0xc3c5a547";
            case "ownerOf(uint256)": return "0x6352211e";
            case "balanceOf(address)": return "0x70a08231";
            case "tokenOfOwnerByIndex(address,uint256)": return "0x2f745c59";
            case "allowance(address,address)": return "0xdd62ed3e";
            case "approve(address,uint256)": return "0x095ea7b3";
            case "mintLicense(uint256,address)": return "0x52e6c1de";
            case "licenses(uint256)": return "0x1e084558";
            case "getLicense(uint256)": return "0x2c89a91a";
            case "activateLicense(uint256,string)": return "0x8c9fb31a";
            case "getActiveApps()": return "0x4e53a444";
            case "getApp(uint256)": return "0x24f3a51b";
            default: return "0x00000000";
        }
    }

    /** 编码 uint256 参数 */
    public static String encodeUint(long value) {
        return String.format("%064x", value);
    }

    /** 编码 address 参数 */
    public static String encodeAddress(String addr) {
        String clean = addr.startsWith("0x") ? addr.substring(2) : addr;
        return String.format("%64s", clean).replace(' ', '0');
    }

    /** 解码 uint256 返回值 */
    public static BigInteger decodeUint(String hex) {
        if (hex.startsWith("0x")) hex = hex.substring(2);
        return new BigInteger(hex, 16);
    }

    /** 解码 string 返回值 (ABI 编码) */
    public static String decodeString(String hex) {
        if (hex.startsWith("0x")) hex = hex.substring(2);
        if (hex.length() < 128) return "";
        // offset (32 bytes) + length (32 bytes) + data
        int offset = Integer.parseInt(hex.substring(0, 64), 16);
        int len = Integer.parseInt(hex.substring(64, 128), 16);
        if (len == 0) return "";
        int dataStart = offset * 2 + 64; // offset in bytes * 2 + 64 hex chars for offset+length
        // Actually offset is from start of return data
        dataStart = (offset + 32) * 2; // skip offset itself, then length is at offset position
        // Simpler: data starts at position (offset+32)*2, but length is at offset*2
        String lengthHexStr = hex.substring(offset * 2, offset * 2 + 64);
        int strLen = Integer.parseInt(lengthHexStr, 16);
        int strStart = (offset + 32) * 2;
        if (strStart + strLen * 2 > hex.length()) strLen = (hex.length() - strStart) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strLen; i++) {
            int c = Integer.parseInt(hex.substring(strStart + i * 2, strStart + i * 2 + 2), 16);
            sb.append((char) c);
        }
        return sb.toString();
    }

    /** 解码 bool 返回值 */
    public static boolean decodeBool(String hex) {
        if (hex.startsWith("0x")) hex = hex.substring(2);
        return !hex.equals("0000000000000000000000000000000000000000000000000000000000000000");
    }

    /** 解码 address 返回值 */
    public static String decodeAddress(String hex) {
        if (hex.startsWith("0x")) hex = hex.substring(2);
        if (hex.length() < 64) return "0x0";
        return "0x" + hex.substring(24, 64).toLowerCase();
    }

    // ===== 合约查询方法 =====

    /** 获取APP总数 */
    public CompletableFuture<Long> getAppCount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String data = selector("nextAppId()");
                String result = ethCall(ContractConfig.APP_REGISTRY, data);
                return decodeUint(result).longValue();
            } catch (Exception e) {
                return 0L;
            }
        });
    }

    /** 获取开发者总数 */
    public CompletableFuture<Long> getDevCount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String data = selector("developerCount()");
                String result = ethCall(ContractConfig.DEVELOPER_REGISTRY, data);
                return decodeUint(result).longValue();
            } catch (Exception e) {
                return 0L;
            }
        });
    }

    /** 获取已售许可证总数 */
    public CompletableFuture<Long> getLicenseCount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String data = selector("totalSupply()");
                String result = ethCall(ContractConfig.LICENSE_NFT, data);
                return decodeUint(result).longValue();
            } catch (Exception e) {
                return 0L;
            }
        });
    }

    /** 获取APP详情 (链上读取) */
    public CompletableFuture<AppInfo> getApp(long appId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String data = selector("apps(uint256)") + encodeUint(appId);
                String result = ethCall(ContractConfig.APP_REGISTRY, data);
                // 解码 ABI 编码的 App struct (12个字段)
                if (result.length() < 20) return null;

                // 去掉 0x
                String hex = result.startsWith("0x") ? result.substring(2) : result;

                // ABI 编码: 每个动态类型存 offset，静态类型直接存
                // struct App {
                //   uint256 appId;        0
                //   address developer;    1
                //   string name;          2 (offset)
                //   string packageName;   3 (offset)
                //   string description;   4 (offset)
                //   string apkURI;        5 (offset)
                //   string iconURI;       6 (offset)
                //   uint256 price;        7
                //   uint256 trialDays;    8
                //   string version;       9 (offset)
                //   uint256 createdAt;    10
                //   uint256 updatedAt;    11
                //   bool active;          12
                // }

                AppInfo app = new AppInfo();
                app.appId = decodeUint(hex.substring(0, 64)).longValue();
                app.developer = decodeAddress(hex.substring(64, 128));

                // 读取 offset 值
                long offsetName = decodeUint(hex.substring(128, 192)).longValue();
                long offsetPkg = decodeUint(hex.substring(192, 256)).longValue();
                long offsetDesc = decodeUint(hex.substring(256, 320)).longValue();
                long offsetApk = decodeUint(hex.substring(320, 384)).longValue();
                long offsetIcon = decodeUint(hex.substring(384, 448)).longValue();

                app.price = decodeUint(hex.substring(448, 512));
                app.trialDays = decodeUint(hex.substring(512, 576)).longValue();

                long offsetVer = decodeUint(hex.substring(576, 640)).longValue();
                app.createdAt = decodeUint(hex.substring(640, 704)).longValue() * 1000;
                app.updatedAt = decodeUint(hex.substring(704, 768)).longValue() * 1000;
                app.active = decodeBool(hex.substring(768, 832));

                // 读取动态字符串
                app.name = readStringAt(hex, (int) offsetName);
                app.packageName = readStringAt(hex, (int) offsetPkg);
                app.description = readStringAt(hex, (int) offsetDesc);
                app.apkURI = readStringAt(hex, (int) offsetApk);
                app.iconURI = readStringAt(hex, (int) offsetIcon);
                app.version = readStringAt(hex, (int) offsetVer);

                return app;
            } catch (Exception e) {
                return null;
            }
        });
    }

    /** 从 hex 数据中读取字符串 */
    private String readStringAt(String hex, int offset) {
        try {
            int pos = offset * 2;
            if (pos + 64 > hex.length()) return "";
            int len = Integer.parseInt(hex.substring(pos, pos + 64), 16);
            if (len == 0) return "";
            int strStart = pos + 64;
            int strLen = Math.min(len, (hex.length() - strStart) / 2);
            byte[] bytes = new byte[strLen];
            for (int i = 0; i < strLen; i++) {
                bytes[i] = (byte) Integer.parseInt(hex.substring(strStart + i * 2, strStart + i * 2 + 2), 16);
            }
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    /** 获取用户持有的 NFT 数量 */
    public CompletableFuture<Long> balanceOf(String address) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String data = selector("balanceOf(address)") + encodeAddress(address);
                String result = ethCall(ContractConfig.LICENSE_NFT, data);
                return decodeUint(result).longValue();
            } catch (Exception e) {
                return 0L;
            }
        });
    }

    /** 获取用户第 index 个 NFT 的 tokenId */
    public CompletableFuture<Long> tokenOfOwnerByIndex(String address, long index) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String data = selector("tokenOfOwnerByIndex(address,uint256)")
                        + encodeAddress(address) + encodeUint(index);
                String result = ethCall(ContractConfig.LICENSE_NFT, data);
                return decodeUint(result).longValue();
            } catch (Exception e) {
                return -1L;
            }
        });
    }

    /** 批量获取所有APP */
    public CompletableFuture<List<AppInfo>> getAllApps() {
        return getAppCount().thenCompose(count -> {
            List<CompletableFuture<AppInfo>> futures = new ArrayList<>();
            for (long i = 1; i <= count; i++) {
                futures.add(getApp(i));
            }
            CompletableFuture<Void> all = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
            return all.thenApply(v -> {
                List<AppInfo> apps = new ArrayList<>();
                for (CompletableFuture<AppInfo> f : futures) {
                    AppInfo app = f.join();
                    if (app != null && app.active) apps.add(app);
                }
                return apps;
            });
        });
    }

    /** 获取用户所有许可证 (NFT tokenIds) */
    public CompletableFuture<List<Long>> getUserLicenses(String address) {
        return balanceOf(address).thenCompose(balance -> {
            if (balance == 0) {
                return CompletableFuture.completedFuture(new ArrayList<>());
            }
            List<CompletableFuture<Long>> futures = new ArrayList<>();
            for (long i = 0; i < balance; i++) {
                futures.add(tokenOfOwnerByIndex(address, i));
            }
            CompletableFuture<Void> all = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
            return all.thenApply(v -> {
                List<Long> ids = new ArrayList<>();
                for (CompletableFuture<Long> f : futures) {
                    long id = f.join();
                    if (id >= 0) ids.add(id);
                }
                return ids;
            });
        });
    }

    /** 查询 USDC 授权额度 */
    public CompletableFuture<BigInteger> usdcAllowance(String owner, String spender) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String data = selector("allowance(address,address)")
                        + encodeAddress(owner) + encodeAddress(spender);
                String result = ethCall(ContractConfig.USDC, data);
                return decodeUint(result);
            } catch (Exception e) {
                return BigInteger.ZERO;
            }
        });
    }

    /** 获取 POL 余额 */
    public CompletableFuture<BigInteger> getBalance(String address) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONArray params = new JSONArray();
                params.put(address);
                params.put("latest");
                String result = rpcCall("eth_getBalance", params);
                return decodeUint(result);
            } catch (Exception e) {
                return BigInteger.ZERO;
            }
        });
    }

    /** 获取 USDC 余额 */
    public CompletableFuture<BigInteger> getUsdcBalance(String address) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String data = selector("balanceOf(address)") + encodeAddress(address);
                String result = ethCall(ContractConfig.USDC, data);
                return decodeUint(result);
            } catch (Exception e) {
                return BigInteger.ZERO;
            }
        });
    }
}
