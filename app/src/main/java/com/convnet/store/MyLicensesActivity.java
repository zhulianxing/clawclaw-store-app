package com.convnet.store;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * 我的许可证 — 显示用户持有的 NFT 激活码
 */
public class MyLicensesActivity extends AppCompatActivity {

    private Web3Client web3;
    private TextView txtEmpty, txtTitle;
    private LinearLayout licenseContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);

        web3 = new Web3Client(ContractConfig.RPC_URL);

        txtTitle = findViewById(R.id.licenses_title);
        txtEmpty = findViewById(R.id.licenses_empty);
        licenseContainer = findViewById(R.id.license_container);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("我的数字凭证");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loadLicenses();
    }

    private void loadLicenses() {
        WalletManager wm = WalletManager.getInstance(this);
        if (!wm.hasWallet()) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtEmpty.setText("请先导入钱包");
            return;
        }

        String address = wm.getAddress();
        txtTitle.setText("钱包: " + address.substring(0, 6) + "..." + address.substring(address.length() - 4));

        web3.getUserLicenses(address).thenAccept(tokenIds -> {
            runOnUiThread(() -> {
                if (tokenIds.isEmpty()) {
                    txtEmpty.setVisibility(View.VISIBLE);
                    txtEmpty.setText("暂无数字凭证\n在商店购买APP后，NFT激活码会显示在这里");
                } else {
                    txtEmpty.setVisibility(View.GONE);
                    for (Long tokenId : tokenIds) {
                        addLicenseCard(tokenId);
                    }
                }
            });
        }).exceptionally(e -> {
            runOnUiThread(() -> {
                txtEmpty.setVisibility(View.VISIBLE);
                txtEmpty.setText("加载失败: " + e.getMessage());
            });
            return null;
        });
    }

    private void addLicenseCard(long tokenId) {
        TextView card = new TextView(this);
        card.setText("🎫 NFT #" + tokenId + "\nToken ID: " + Long.toHexString(tokenId).toUpperCase());
        card.setPadding(32, 32, 32, 32);
        card.setBackgroundResource(R.drawable.card_bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);

        licenseContainer.addView(card);

        // 查询该 NFT 对应的 APP 信息
        // TODO: 调用 LicenseNFT.licenses(tokenId) 获取 appId
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
