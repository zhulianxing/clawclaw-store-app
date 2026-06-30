package com.convnet.store;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import okhttp3.*;

/**
 * APP 详情页 — 展示APP信息 + 下载/购买
 */
public class AppDetailActivity extends AppCompatActivity {

    private long appId;
    private String pkgName, appName;
    private Web3Client web3;
    private AppInfo appInfo;

    private TextView txtName, txtIcon, txtPkg, txtDesc, txtPrice, txtDev, txtTrial, txtVersion;
    private Button btnDownload, btnPurchase;
    private ProgressBar progressBar;
    private LinearLayout purchaseSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        web3 = new Web3Client(ContractConfig.RPC_URL);

        // 获取参数
        appId = getIntent().getLongExtra("appId", 0);
        pkgName = getIntent().getStringExtra("package");
        appName = getIntent().getStringExtra("name");

        initViews();
        loadAppInfo();
    }

    private void initViews() {
        txtName = findViewById(R.id.detail_name);
        txtIcon = findViewById(R.id.detail_icon);
        txtPkg = findViewById(R.id.detail_pkg);
        txtDesc = findViewById(R.id.detail_desc);
        txtPrice = findViewById(R.id.detail_price);
        txtDev = findViewById(R.id.detail_dev);
        txtTrial = findViewById(R.id.detail_trial);
        txtVersion = findViewById(R.id.detail_version);

        btnDownload = findViewById(R.id.btn_download);
        btnPurchase = findViewById(R.id.btn_purchase);
        progressBar = findViewById(R.id.progress_bar);
        purchaseSection = findViewById(R.id.purchase_section);

        // 下载试用
        btnDownload.setOnClickListener(v -> downloadApk(false));

        // 购买许可证
        btnPurchase.setOnClickListener(v -> {
            if (!WalletManager.getInstance(this).hasWallet()) {
                Toast.makeText(this, "请先导入钱包", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, WalletActivity.class));
                return;
            }
            showPurchaseDialog();
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(appName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadAppInfo() {
        web3.getApp(appId).thenAccept(app -> {
            if (app == null) {
                runOnUiThread(() -> Toast.makeText(this, "加载失败", Toast.LENGTH_SHORT).show());
                return;
            }
            appInfo = app;
            runOnUiThread(() -> {
                txtIcon.setText(app.getIcon());
                txtName.setText(app.getLocalName());
                txtPkg.setText(app.packageName);
                txtDesc.setText(app.getLocalDesc());
                txtPrice.setText(app.getPriceDisplay());
                txtDev.setText("创作者: " + app.getShortDeveloper());
                txtTrial.setText("试用 " + app.trialDays + " 天");
                txtVersion.setText("版本 " + (app.version != null ? app.version : "v1.0.0"));
            });
        });
    }

    /** 显示购买确认弹窗 */
    private void showPurchaseDialog() {
        if (appInfo == null) return;

        String price = appInfo.getPriceDisplay();
        String msg = "应用: " + appInfo.getLocalName() + "\n"
                + "价格: " + price + "\n"
                + "收益分配:\n"
                + "  创作者 60%: " + (appInfo.price.doubleValue() / 1e6 * 0.6) + " USDC\n"
                + "  平台 10%: " + (appInfo.price.doubleValue() / 1e6 * 0.1) + " USDC\n"
                + "  Gas补贴 5%: " + (appInfo.price.doubleValue() / 1e6 * 0.05) + " USDC\n"
                + "  推广者 25%: " + (appInfo.price.doubleValue() / 1e6 * 0.25) + " USDC\n\n"
                + "支付方式: USDC (Polygon链上)\n"
                + "获得: ERC-721 NFT 永久激活码";

        new AlertDialog.Builder(this)
                .setTitle("购买数字凭证")
                .setMessage(msg)
                .setPositiveButton("确认购买", (d, w) -> doPurchase())
                .setNegativeButton("取消", null)
                .show();
    }

    /** 执行链上购买 — USDC approve + mintLicense 两步交易 */
    private void doPurchase() {
        if (appInfo == null) return;
        WalletManager wm = WalletManager.getInstance(this);
        if (!wm.hasWallet()) {
            Toast.makeText(this, "请先导入钱包", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, WalletActivity.class));
            return;
        }

        btnPurchase.setEnabled(false);
        btnPurchase.setText("授权 USDC...");

        String from = wm.getAddress();

        // Step 1: USDC approve(LicenseNFT, price)
        String approveData = Web3Client.selector("approve(address,uint256)")
                + Web3Client.encodeAddress(ContractConfig.LICENSE_NFT)
                + Web3Client.encodeUint(appInfo.price.longValue());

        wm.sendTransaction(ContractConfig.USDC, BigInteger.ZERO, approveData,
            (success1, result1) -> {
                if (!success1) {
                    runOnUiThread(() -> {
                        btnPurchase.setEnabled(true);
                        btnPurchase.setText("购买数字凭证");
                        Toast.makeText(this, "❌ 授权失败: " + result1, Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                runOnUiThread(() -> btnPurchase.setText("等待确认..."));

                // 等待 approve 交易确认（Polygon ~2s）
                new Thread(() -> {
                    try { Thread.sleep(5000); } catch (InterruptedException ignored) {}

                    runOnUiThread(() -> btnPurchase.setText("铸造 NFT..."));

                    // Step 2: LicenseNFT.mintLicense(appId, promoterAddress)
                    // promoterAddress = address(0) 表示无推广者
                    String mintData = Web3Client.selector("mintLicense(uint256,address)")
                            + Web3Client.encodeUint(appInfo.appId)
                            + Web3Client.encodeAddress("0x0000000000000000000000000000000000000000");

                    wm.sendTransaction(ContractConfig.LICENSE_NFT, BigInteger.ZERO, mintData,
                        (success2, result2) -> {
                            runOnUiThread(() -> {
                                btnPurchase.setEnabled(true);
                                btnPurchase.setText("购买数字凭证");
                                if (success2) {
                                    new AlertDialog.Builder(this)
                                            .setTitle("🎉 购买成功")
                                            .setMessage("NFT 凭证已铸造！\n交易哈希: " + result2.substring(0, Math.min(20, result2.length())) + "...")
                                            .setPositiveButton("查看我的凭证", (d, w) -> {
                                                startActivity(new Intent(this, MyLicensesActivity.class));
                                            })
                                            .setNegativeButton("关闭", null)
                                            .show();
                                } else {
                                    Toast.makeText(this, "❌ 铸造失败: " + result2, Toast.LENGTH_LONG).show();
                                }
                            });
                        });
                }).start();
            });
    }

    /** 下载 APK */
    private void downloadApk(boolean isPaid) {
        if (appInfo == null) return;

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        btnDownload.setEnabled(false);
        btnDownload.setText("下载中...");

        new Thread(() -> {
            try {
                String apkUrl = appInfo.getApkUrl();
                String fileName = ContractConfig.getApkName(appInfo.packageName);

                // 下载
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(apkUrl).build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new Exception("下载失败: " + response.code());
                }

                // 保存到缓存
                File dir = new File(getCacheDir(), "downloads");
                if (!dir.exists()) dir.mkdirs();
                File apkFile = new File(dir, fileName);

                InputStream is = response.body().byteStream();
                FileOutputStream fos = new FileOutputStream(apkFile);
                byte[] buffer = new byte[8192];
                int len;
                long total = response.body().contentLength();
                long downloaded = 0;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    downloaded += len;
                    if (total > 0) {
                        int progress = (int) (downloaded * 100 / total);
                        runOnUiThread(() -> progressBar.setProgress(progress));
                    }
                }
                fos.close();
                is.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnDownload.setEnabled(true);
                    btnDownload.setText("下载完成");
                    Toast.makeText(this, "下载完成，正在安装...", Toast.LENGTH_SHORT).show();
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

    /** 安装 APK */
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
            Toast.makeText(this, "安装失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
