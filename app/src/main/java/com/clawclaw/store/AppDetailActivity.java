package com.clawclaw.store;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * APP 详情页 — 产品信息 + 激活码验证 + 下载
 */
public class AppDetailActivity extends AppCompatActivity {

    private int productId;
    private String pkgName, appName, priceText;
    private ApiClient api;
    private AppInfo appInfo;

    private TextView txtName, txtIcon, txtPkg, txtDesc, txtPrice, txtTrial, txtVersion, txtPlatform;
    private Button btnDownload, btnActivate, btnBuy;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        api = new ApiClient();

        productId = getIntent().getIntExtra("productId", 0);
        pkgName = getIntent().getStringExtra("package");
        appName = getIntent().getStringExtra("name");
        priceText = getIntent().getStringExtra("price");

        initViews();
        loadProductInfo();
    }

    private void initViews() {
        txtName = findViewById(R.id.detail_name);
        txtIcon = findViewById(R.id.detail_icon);
        txtPkg = findViewById(R.id.detail_pkg);
        txtDesc = findViewById(R.id.detail_desc);
        txtPrice = findViewById(R.id.detail_price);
        txtTrial = findViewById(R.id.detail_trial);
        txtVersion = findViewById(R.id.detail_version);
        txtPlatform = findViewById(R.id.detail_dev);

        btnDownload = findViewById(R.id.btn_download);
        btnActivate = findViewById(R.id.btn_purchase);
        btnBuy = findViewById(R.id.btn_buy_web);
        progressBar = findViewById(R.id.progress_bar);

        // 显示预填信息
        txtName.setText(appName);
        txtPkg.setText(pkgName);
        txtPrice.setText(priceText);

        btnDownload.setOnClickListener(v -> downloadApk());

        btnActivate.setOnClickListener(v -> showActivationDialog());

        btnBuy.setOnClickListener(v -> {
            // 跳转网页购买
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://clawclaw.tech/"));
            startActivity(intent);
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(appName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadProductInfo() {
        new Thread(() -> {
            try {
                appInfo = api.getProduct(productId);
                if (appInfo == null) {
                    runOnUiThread(() -> Toast.makeText(this, "加载失败", Toast.LENGTH_SHORT).show());
                    return;
                }
                runOnUiThread(() -> {
                    txtIcon.setText(appInfo.icon_emoji != null ? appInfo.icon_emoji : "📦");
                    txtName.setText(appInfo.name_cn);
                    txtDesc.setText(appInfo.description);
                    txtPrice.setText(appInfo.getPriceDisplay());
                    txtVersion.setText("版本 " + appInfo.version);
                    txtPlatform.setText(appInfo.platform + " · " + (appInfo.apk_size != null ? appInfo.apk_size : "-"));

                    String trial = appInfo.getTrialText();
                    if (trial != null && !trial.isEmpty()) {
                        txtTrial.setText(trial);
                        txtTrial.setVisibility(View.VISIBLE);
                    } else {
                        txtTrial.setVisibility(View.GONE);
                    }

                    // 免费应用隐藏购买按钮
                    if (appInfo.price == 0) {
                        btnActivate.setVisibility(View.GONE);
                        btnBuy.setVisibility(View.GONE);
                    }

                    // 检查激活状态
                    checkActivation();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void checkActivation() {
        String machineId = getMachineId();
        new Thread(() -> {
            try {
                org.json.JSONObject resp = api.checkActivation(machineId, pkgName);
                boolean activated = resp.optBoolean("activated", false);
                runOnUiThread(() -> {
                    if (activated) {
                        btnActivate.setText("✅ 已激活");
                        btnActivate.setEnabled(false);
                        btnBuy.setVisibility(View.GONE);
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    /** 激活码验证弹窗 */
    private void showActivationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("输入激活码");

        final EditText input = new EditText(this);
        input.setHint("请输入16位激活码");
        input.setPadding(60, 40, 60, 20);
        builder.setView(input);

        builder.setPositiveButton("激活", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(this, "请输入激活码", Toast.LENGTH_SHORT).show();
                return;
            }
            doActivate(code);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void doActivate(String code) {
        String machineId = getMachineId();
        btnActivate.setEnabled(false);
        btnActivate.setText("激活中...");

        new Thread(() -> {
            try {
                org.json.JSONObject resp = api.activate(code, machineId, pkgName);
                boolean success = resp.optBoolean("success", false);
                String msg = resp.optString("message", "未知错误");

                runOnUiThread(() -> {
                    btnActivate.setEnabled(true);
                    btnActivate.setText("输入激活码");
                    if (success) {
                        Toast.makeText(this, "✅ " + msg, Toast.LENGTH_LONG).show();
                        btnActivate.setText("✅ 已激活");
                        btnActivate.setEnabled(false);
                        btnBuy.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(this, "❌ " + msg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnActivate.setEnabled(true);
                    btnActivate.setText("输入激活码");
                    Toast.makeText(this, "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void downloadApk() {
        if (appInfo == null) {
            Toast.makeText(this, "正在加载产品信息...", Toast.LENGTH_SHORT).show();
            return;
        }

        String apkUrl = appInfo.getApkUrl();
        if (apkUrl.isEmpty()) {
            Toast.makeText(this, "暂无下载链接", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        btnDownload.setEnabled(false);
        btnDownload.setText("下载中...");

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request req = new Request.Builder().url(apkUrl).build();
                Response res = client.newCall(req).execute();

                if (!res.isSuccessful()) {
                    throw new Exception("HTTP " + res.code());
                }

                String fileName = pkgName + ".apk";
                File dir = new File(getCacheDir(), "downloads");
                if (!dir.exists()) dir.mkdirs();
                File apkFile = new File(dir, fileName);

                InputStream is = res.body().byteStream();
                FileOutputStream fos = new FileOutputStream(apkFile);
                byte[] buf = new byte[8192];
                int len;
                long total = res.body().contentLength();
                long downloaded = 0;
                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    downloaded += len;
                    if (total > 0) {
                        int pct = (int) (downloaded * 100 / total);
                        runOnUiThread(() -> progressBar.setProgress(pct));
                    }
                }
                fos.close();
                is.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnDownload.setEnabled(true);
                    btnDownload.setText("安装");
                    installApk(apkFile);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnDownload.setEnabled(true);
                    btnDownload.setText("下载试用");
                    Toast.makeText(this, "下载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void installApk(File apkFile) {
        try {
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "安装失败，请手动安装", Toast.LENGTH_LONG).show();
        }
    }

    /** 获取设备唯一 ID */
    private String getMachineId() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
