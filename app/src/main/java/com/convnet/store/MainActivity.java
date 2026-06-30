package com.convnet.store;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.List;
import java.math.BigInteger;

public class MainActivity extends AppCompatActivity implements AppAdapter.OnAppClickListener {

    private Web3Client web3;
    private AppAdapter adapter;
    private SwipeRefreshLayout refresh;
    private TextView statApps, statDevs, statLicenses, emptyText;
    private LinearLayout walletBar;
    private TextView walletAddress, walletBalance, walletRole;
    private Button btnImportWallet;

    // Tabs
    private TextView tabApps, tabLicenses, tabPromoter;
    private ScrollView promoterPanel;
    private View recyclerApps;

    // Promoter
    private LinearLayout promoterRegisterCard, promoterStatusCard, promoterEarningsCard;
    private EditText etPromoterName, etPromoterCode;
    private Button btnRegisterPromoter, btnCopyLink;
    private TextView promoterName, promoterCode, promoterLink;
    private TextView promoterTotalSales, promoterTotalEarnings;

    private String currentTab = "apps";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        web3 = new Web3Client(ContractConfig.RPC_URL);

        initViews();
        setupTabs();
        loadData();
        checkPromoterStatus();
    }

    private void initViews() {
        // Stats
        statApps = findViewById(R.id.stat_apps);
        statDevs = findViewById(R.id.stat_devs);
        statLicenses = findViewById(R.id.stat_licenses);

        // Wallet
        walletBar = findViewById(R.id.wallet_bar);
        walletAddress = findViewById(R.id.wallet_address);
        walletBalance = findViewById(R.id.wallet_balance);
        walletRole = findViewById(R.id.wallet_role);
        btnImportWallet = findViewById(R.id.btn_import_wallet);

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

        // Tabs
        tabApps = findViewById(R.id.tab_apps);
        tabLicenses = findViewById(R.id.tab_licenses);
        tabPromoter = findViewById(R.id.tab_promoter);
        promoterPanel = findViewById(R.id.promoter_panel);
        recyclerApps = findViewById(R.id.refresh_layout);

        // Promoter views
        promoterRegisterCard = findViewById(R.id.promoter_register_card);
        promoterStatusCard = findViewById(R.id.promoter_status_card);
        promoterEarningsCard = findViewById(R.id.promoter_earnings_card);
        etPromoterName = findViewById(R.id.et_promoter_name);
        etPromoterCode = findViewById(R.id.et_promoter_code);
        btnRegisterPromoter = findViewById(R.id.btn_register_promoter);
        btnCopyLink = findViewById(R.id.btn_copy_link);
        promoterName = findViewById(R.id.promoter_name);
        promoterCode = findViewById(R.id.promoter_code);
        promoterLink = findViewById(R.id.promoter_link);
        promoterTotalSales = findViewById(R.id.promoter_total_sales);
        promoterTotalEarnings = findViewById(R.id.promoter_total_earnings);

        // Wallet click → go to licenses
        walletBar.setOnClickListener(v -> {
            if (WalletManager.getInstance(this).hasWallet()) {
                startActivity(new Intent(this, MyLicensesActivity.class));
            } else {
                openWalletActivity();
            }
        });

        // Import wallet button
        btnImportWallet.setOnClickListener(v -> openWalletActivity());

        // Promoter register
        btnRegisterPromoter.setOnClickListener(v -> registerPromoter());

        // Copy link
        btnCopyLink.setOnClickListener(v -> copyPromoterLink());
    }

    private void openWalletActivity() {
        startActivity(new Intent(this, WalletActivity.class));
    }

    private void setupTabs() {
        tabApps.setOnClickListener(v -> switchTab("apps"));
        tabLicenses.setOnClickListener(v -> switchTab("licenses"));
        tabPromoter.setOnClickListener(v -> switchTab("promoter"));
    }

    private void switchTab(String tab) {
        currentTab = tab;

        // Reset tab styles
        tabApps.setTextColor(getColor(R.color.text3));
        tabLicenses.setTextColor(getColor(R.color.text3));
        tabPromoter.setTextColor(getColor(R.color.text3));

        // Hide all panels
        refresh.setVisibility(View.GONE);
        promoterPanel.setVisibility(View.GONE);

        switch (tab) {
            case "apps":
                tabApps.setTextColor(getColor(R.color.accent));
                refresh.setVisibility(View.VISIBLE);
                break;
            case "licenses":
                tabLicenses.setTextColor(getColor(R.color.accent));
                if (WalletManager.getInstance(this).hasWallet()) {
                    startActivity(new Intent(this, MyLicensesActivity.class));
                } else {
                    Toast.makeText(this, "请先连接钱包", Toast.LENGTH_SHORT).show();
                    openWalletActivity();
                }
                break;
            case "promoter":
                tabPromoter.setTextColor(getColor(R.color.accent));
                promoterPanel.setVisibility(View.VISIBLE);
                checkPromoterStatus();
                break;
        }
    }

    private void loadData() {
        refresh.setRefreshing(true);
        emptyText.setVisibility(View.GONE);

        // Stats
        web3.getAppCount().thenAccept(count ->
            runOnUiThread(() -> statApps.setText(String.valueOf(count))));

        web3.getDevCount().thenAccept(count ->
            runOnUiThread(() -> statDevs.setText(String.valueOf(count))));

        web3.getLicenseCount().thenAccept(count ->
            runOnUiThread(() -> statLicenses.setText(String.valueOf(count))));

        // App list
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

        updateWalletInfo();
    }

    private void updateWalletInfo() {
        WalletManager wm = WalletManager.getInstance(this);
        if (wm.hasWallet()) {
            String addr = wm.getAddress();
            walletAddress.setText(addr.substring(0, 6) + "..." + addr.substring(addr.length() - 4));
            btnImportWallet.setText("切换钱包");

            // Check if registered as developer or promoter
            web3.isRegisteredDeveloper(addr).thenAccept(isDev -> {
                runOnUiThread(() -> {
                    if (isDev) {
                        walletRole.setVisibility(View.VISIBLE);
                        walletRole.setText("👨‍💻 开发者");
                    }
                });
            });

            web3.isRegisteredPromoter(addr).thenAccept(isPromo -> {
                runOnUiThread(() -> {
                    if (isPromo) {
                        walletRole.setVisibility(View.VISIBLE);
                        walletRole.setText(walletRole.getText() + " 📣 推广者");
                    }
                });
            });

            // Balances
            web3.getUsdcBalance(addr).thenAccept(balance -> {
                double usdc = balance.doubleValue() / 1000000.0;
                runOnUiThread(() -> walletBalance.setText(String.format("%.2f USDC", usdc)));
            });

            web3.getBalance(addr).thenAccept(balance -> {
                double pol = balance.doubleValue() / 1e18;
                runOnUiThread(() ->
                    walletBalance.setText(walletBalance.getText() + " · " + String.format("%.4f POL", pol)));
            });
        } else {
            walletAddress.setText("未连接钱包");
            walletBalance.setText("点击连接钱包体验去中心化软件商店");
            walletRole.setVisibility(View.GONE);
            btnImportWallet.setText("连接钱包");
        }
    }

    // ===== Promoter =====

    private void checkPromoterStatus() {
        WalletManager wm = WalletManager.getInstance(this);
        if (!wm.hasWallet()) {
            promoterRegisterCard.setVisibility(View.VISIBLE);
            promoterStatusCard.setVisibility(View.GONE);
            promoterEarningsCard.setVisibility(View.GONE);
            return;
        }

        String addr = wm.getAddress();
        web3.isRegisteredPromoter(addr).thenAccept(isPromo -> {
            runOnUiThread(() -> {
                if (isPromo) {
                    promoterRegisterCard.setVisibility(View.GONE);
                    promoterStatusCard.setVisibility(View.VISIBLE);
                    promoterEarningsCard.setVisibility(View.VISIBLE);
                    loadPromoterInfo(addr);
                } else {
                    promoterRegisterCard.setVisibility(View.VISIBLE);
                    promoterStatusCard.setVisibility(View.GONE);
                    promoterEarningsCard.setVisibility(View.GONE);
                }
            });
        });
    }

    private void loadPromoterInfo(String addr) {
        web3.getPromoterInfo(addr).thenAccept(info -> {
            runOnUiThread(() -> {
                if (info != null) {
                    promoterName.setText("名称: " + info.name);
                    promoterCode.setText("推广码: " + info.referralCode);
                    String link = "https://daix.fun/?agent=" + addr;
                    promoterLink.setText(link);
                }
            });
        });

        // TODO: Query actual earnings from chain events
        promoterTotalSales.setText("—");
        promoterTotalEarnings.setText("— USDC");
    }

    private void registerPromoter() {
        WalletManager wm = WalletManager.getInstance(this);
        if (!wm.hasWallet()) {
            Toast.makeText(this, "请先连接钱包", Toast.LENGTH_SHORT).show();
            openWalletActivity();
            return;
        }

        String name = etPromoterName.getText().toString().trim();
        String code = etPromoterCode.getText().toString().trim();

        if (name.isEmpty() || code.isEmpty()) {
            Toast.makeText(this, "请填写名称和推广码", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build the register transaction
        String data = Web3Client.selector("register(string,string)")
                + Web3Client.encodeString(name)
                + Web3Client.encodeString(code);

        // Send via WalletManager (signs and sends transaction)
        WalletManager.getInstance(this).sendTransaction(
            ContractConfig.PROMOTER_REGISTRY,
            BigInteger.ZERO,
            data,
            (success, txHash) -> {
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, "✅ 注册成功！tx: " + txHash.substring(0, 10) + "...", Toast.LENGTH_LONG).show();
                        checkPromoterStatus();
                    } else {
                        Toast.makeText(this, "❌ 注册失败: " + txHash, Toast.LENGTH_LONG).show();
                    }
                });
            }
        );
    }

    private void copyPromoterLink() {
        String link = promoterLink.getText().toString();
        if (link.isEmpty()) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("promoter_link", link));
        Toast.makeText(this, "✅ 推广链接已复制", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAppClick(AppInfo app) {
        Intent intent = new Intent(this, AppDetailActivity.class);
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
        if (currentTab.equals("promoter")) {
            checkPromoterStatus();
        }
    }
}
