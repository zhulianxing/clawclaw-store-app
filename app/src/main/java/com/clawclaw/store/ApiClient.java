package com.clawclaw.store;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ClawClaw.tech 统一 API 客户端
 * 基地址: https://clawclaw.tech
 */
public class ApiClient {

    private static final String BASE_URL = "https://clawclaw.tech";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static OkHttpClient client;

    public ApiClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
        }
    }

    // ===== 基础请求 =====

    private JSONObject get(String path) throws Exception {
        Request req = new Request.Builder().url(BASE_URL + path).build();
        try (Response res = client.newCall(req).execute()) {
            return new JSONObject(res.body().string());
        }
    }

    private JSONObject post(String path, JSONObject body) throws Exception {
        RequestBody rb = RequestBody.create(body.toString(), JSON);
        Request req = new Request.Builder().url(BASE_URL + path).post(rb).build();
        try (Response res = client.newCall(req).execute()) {
            return new JSONObject(res.body().string());
        }
    }

    // ===== API 方法 =====

    /** 获取产品列表 */
    public List<AppInfo> getProducts() throws Exception {
        JSONObject resp = get("/api/products");
        JSONArray arr = resp.getJSONArray("data");
        List<AppInfo> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            AppInfo a = new AppInfo();
            a.id = obj.optInt("id");
            a.package_name = obj.optString("package_name");
            a.name = obj.optString("name");
            a.name_cn = obj.optString("name_cn");
            a.icon_emoji = obj.optString("icon_emoji", "📦");
            a.description = obj.optString("description");
            a.price = obj.optDouble("price");
            a.apk_url = obj.optString("apk_url");
            a.version = obj.optString("version", "1.0.0");
            a.platform = obj.optString("platform", "Android");
            a.apk_size = obj.optString("apk_size");
            a.trial_hours = obj.optInt("trial_hours", 24);
            a.active = obj.optInt("active", 1);
            list.add(a);
        }
        return list;
    }

    /** 获取产品详情 */
    public AppInfo getProduct(int id) throws Exception {
        JSONObject resp = get("/api/products/" + id);
        if (!resp.optBoolean("success")) return null;
        JSONObject obj = resp.getJSONObject("data");
        AppInfo a = new AppInfo();
        a.id = obj.optInt("id");
        a.package_name = obj.optString("package_name");
        a.name = obj.optString("name");
        a.name_cn = obj.optString("name_cn");
        a.icon_emoji = obj.optString("icon_emoji", "📦");
        a.description = obj.optString("description");
        a.price = obj.optDouble("price");
        a.apk_url = obj.optString("apk_url");
        a.version = obj.optString("version", "1.0.0");
        a.platform = obj.optString("platform", "Android");
        a.apk_size = obj.optString("apk_size");
        a.trial_hours = obj.optInt("trial_hours", 24);
        a.active = obj.optInt("active", 1);
        return a;
    }

    /** 检查激活状态 */
    public JSONObject checkActivation(String machineId, String packageName) throws Exception {
        JSONObject body = new JSONObject();
        body.put("machine_id", machineId);
        body.put("package_name", packageName);
        return post("/api/activate/check", body);
    }

    /** 验证激活码 */
    public JSONObject activate(String code, String machineId, String packageName) throws Exception {
        JSONObject body = new JSONObject();
        body.put("code", code);
        body.put("machine_id", machineId);
        body.put("package_name", packageName);
        return post("/api/activate", body);
    }

    /** 创建订单 */
    public JSONObject createOrder(int productId, String email, String machineId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("product_id", productId);
        body.put("email", email);
        body.put("machine_id", machineId);
        body.put("amount", 0); // 由服务端根据产品价格填充
        return post("/api/orders/create", body);
    }
}
