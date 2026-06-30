package com.convnet.store;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.math.BigInteger;

/**
 * 钱包导入页 — 支持私钥导入
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

        // 如果已有钱包，显示信息
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

        // TODO: 实际应通过 secp256k1 从私钥推导地址
        // 暂时提示用户输入地址
        Toast.makeText(this, "私钥导入功能需要集成加密库\n请使用网页版 daix.fun 连接钱包", Toast.LENGTH_LONG).show();

        // 临时方案：引导到网页版
        android.content.Intent browserIntent = new android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://daix.fun"));
        startActivity(browserIntent);
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
