package com.convnet.store;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AppAdapter.OnAppClickListener {

    private Web3Client web3;
    private AppAdapter adapter;
    private SwipeRefreshLayout refresh;
    private TextView statApps, statDevs, statLicenses, emptyText;
    private LinearLayout walletBar;
    private TextView walletAddress, walletBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        web3 = new Web3Client(ContractConfig.RPC_URL);

        initViews();
        loadData();
    }

    private void initViews() {
        // Stats
        statApps = findViewById(R.id.stat_apps);
        statDevs = findViewById(R.id.stat_devs);
        statLicenses = findViewById(R.id.stat_licenses);

        // Wallet bar
        walletBar = findViewById(R.id.wallet_bar);
        walletAddress = findViewById(R.id.wallet_address);
        walletBalance = findViewById(R.id.wallet_balance);

        // Refresh
        refresh = findViewById(R.id.refresh_layout);
        refresh.setOnRefreshListener(this::loadData);

        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_apps);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        adapter = new AppAdapter(this);
        recyclerView.setAdapter(adapter);

        // Empty state
        emptyText = findViewById(R.id.empty_text);

        // Wallet click
        walletBar.setOnClickListener(v -> {
            if (WalletManager.getInstance(this).hasWallet()) {
                startActivity(new android.content.Intent(this, MyLicensesActivity.class));
            } else {
                Toast.makeText(this, "请先导入钱包", Toast.LENGTH_SHORT).show();
            }
        });

        // 导入钱包按钮
        findViewById(R.id.btn_import_wallet).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, WalletActivity.class));
        });
    }

    private void loadData() {
        refresh.setRefreshing(true);
        emptyText.setVisibility(View.GONE);

        // 加载统计数据
        web3.getAppCount().thenAccept(count -> {
            runOnUiThread(() -> statApps.setText(String.valueOf(count)));
        });

        web3.getDevCount().thenAccept(count -> {
            runOnUiThread(() -> statDevs.setText(String.valueOf(count)));
        });

        web3.getLicenseCount().thenAccept(count -> {
            runOnUiThread(() -> statLicenses.setText(String.valueOf(count)));
        });

        // 加载APP列表
        web3.getAllApps().thenAccept(apps -> {
            runOnUiThread(() -> {
                refresh.setRefreshing(false);
                if (apps.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    emptyText.setText("暂无应用");
                } else {
                    emptyText.setVisibility(View.GONE);
                }
                adapter.setApps(apps);
            });
        }).exceptionally(e -> {
            runOnUiThread(() -> {
                refresh.setRefreshing(false);
                emptyText.setVisibility(View.VISIBLE);
                emptyText.setText("加载失败: " + e.getMessage());
            });
            return null;
        });

        // 更新钱包信息
        updateWalletInfo();
    }

    private void updateWalletInfo() {
        WalletManager wm = WalletManager.getInstance(this);
        if (wm.hasWallet()) {
            String addr = wm.getAddress();
            walletAddress.setText(addr.substring(0, 6) + "..." + addr.substring(addr.length() - 4));

            // 查询 USDC 余额
            web3.getUsdcBalance(addr).thenAccept(balance -> {
                double usdc = balance.doubleValue() / 1000000.0;
                runOnUiThread(() -> {
                    walletBalance.setText(usdc + " USDC");
                });
            });

            // 查询 POL 余额
            web3.getBalance(addr).thenAccept(balance -> {
                double pol = balance.doubleValue() / 1e18;
                runOnUiThread(() -> {
                    walletBalance.setText(walletBalance.getText() + " · " + String.format("%.4f", pol) + " POL");
                });
            });
        } else {
            walletAddress.setText("未连接钱包");
            walletBalance.setText("");
        }
    }

    @Override
    public void onAppClick(AppInfo app) {
        android.content.Intent intent = new android.content.Intent(this, AppDetailActivity.class);
        intent.putExtra("appId", app.appId);
        intent.putExtra("name", app.getLocalName());
        intent.putExtra("package", app.packageName);
        intent.putExtra("desc", app.getLocalDesc());
        intent.putExtra("price", app.getPriceDisplay());
        intent.putExtra("developer", app.developer);
        intent.putExtra("trial", app.trialDays);
        intent.putExtra("version", app.version);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWalletInfo();
    }
}
