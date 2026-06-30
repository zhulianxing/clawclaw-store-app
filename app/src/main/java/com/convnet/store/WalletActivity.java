package com.convnet.store;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 钱包导入页 — 支持私钥导入（加密存储）
 */
public class WalletActivity extends AppCompatActivity {

    private EditText editKey;
    private Button btnImport, btnClear;
    private TextView txtInfo;
    private Web3Client web3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        web3 = new Web3Client(ContractConfig.RPC_URL);

        editKey = findViewById(R.id.edit_private_key);
        btnImport = findViewById(R.id.btn_import);
        btnClear = findViewById(R.id.btn_clear);
        txtInfo = findViewById(R.id.wallet_info);

        // 密码输入模式
        editKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        WalletManager wm = WalletManager.getInstance(this);
        if (wm.hasWallet()) {
            showWalletInfo(wm.getAddress());
        }

        btnImport.setOnClickListener(v -> importWallet());
        btnClear.setOnClickListener(v -> {
            wm.clearWallet();
            txtInfo.setText("未导入钱包");
            editKey.setText("");
            Toast.makeText(this, "已清除", Toast.LENGTH_SHORT).show();
        });
    }

    private void importWallet() {
        String key = editKey.getText().toString().trim();
        if (key.isEmpty()) {
            Toast.makeText(this, "请输入私钥", Toast.LENGTH_SHORT).show();
            return;
        }

        // 标准化私钥
        if (key.startsWith("0x")) key = key.substring(2);
        if (key.length() != 64) {
            Toast.makeText(this, "私钥格式错误 (应为64位十六进制)", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证私钥有效性并推导地址
        String address = WalletManager.addressFromPrivateKey(key);
        if (address == null) {
            Toast.makeText(this, "私钥无效，无法推导地址", Toast.LENGTH_SHORT).show();
            return;
        }

        // 保存钱包（WalletManager 会自动加密私钥）
        WalletManager wm = WalletManager.getInstance(this);
        wm.saveWallet(address, key);

        editKey.setText("");
        Toast.makeText(this, "✅ 钱包导入成功", Toast.LENGTH_SHORT).show();
        showWalletInfo(address);
    }

    private void showWalletInfo(String address) {
        txtInfo.setText("钱包地址: " + address.substring(0, 10) + "..." + address.substring(address.length() - 6));

        // 查询余额
        web3.getUsdcBalance(address).thenAccept(balance -> {
            double usdc = balance.doubleValue() / 1e6;
            runOnUiThread(() -> {
                txtInfo.append("\nUSDC 余额: " + usdc);
            });
        });

        web3.getBalance(address).thenAccept(balance -> {
            double pol = balance.doubleValue() / 1e18;
            runOnUiThread(() -> {
                txtInfo.append("\nPOL 余额: " + String.format("%.4f", pol));
            });
        });
    }
}
