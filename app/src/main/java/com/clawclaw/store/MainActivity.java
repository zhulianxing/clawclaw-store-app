package com.clawclaw.store;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AppAdapter.OnAppClickListener {

    private ApiClient api;
    private AppAdapter adapter;
    private SwipeRefreshLayout refresh;
    private TextView emptyText, statProducts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new ApiClient();
        initViews();
        loadData();
    }

    private void initViews() {
        statProducts = findViewById(R.id.stat_apps);

        refresh = findViewById(R.id.refresh_layout);
        refresh.setOnRefreshListener(this::loadData);

        RecyclerView recyclerView = findViewById(R.id.recycler_apps);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        adapter = new AppAdapter(this);
        recyclerView.setAdapter(adapter);

        emptyText = findViewById(R.id.empty_text);
    }

    private void loadData() {
        refresh.setRefreshing(true);
        emptyText.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                List<AppInfo> apps = api.getProducts();
                runOnUiThread(() -> {
                    refresh.setRefreshing(false);
                    statProducts.setText(String.valueOf(apps.size()));

                    if (apps.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        emptyText.setText("暂无产品");
                    } else {
                        emptyText.setVisibility(View.GONE);
                    }
                    adapter.setApps(apps);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    refresh.setRefreshing(false);
                    emptyText.setVisibility(View.VISIBLE);
                    emptyText.setText("加载失败: " + e.getMessage());
                    Toast.makeText(this, "网络错误，下拉重试", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public void onAppClick(AppInfo app) {
        Intent intent = new Intent(this, AppDetailActivity.class);
        intent.putExtra("productId", app.id);
        intent.putExtra("name", app.name_cn != null ? app.name_cn : app.name);
        intent.putExtra("package", app.package_name);
        intent.putExtra("price", app.getPriceDisplay());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
