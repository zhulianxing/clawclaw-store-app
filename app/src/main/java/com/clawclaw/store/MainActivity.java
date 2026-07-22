package com.clawclaw.store;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AppAdapter.OnAppClickListener {

    private ApiClient api;
    private AppAdapter adapter;
    private SwipeRefreshLayout refresh;
    private TextView emptyText;
    private EditText searchInput;
    private ImageButton btnClear;
    private LinearLayout emptyView;
    private LinearLayout categoryTabs;

    private List<AppInfo> allApps = new ArrayList<>();
    private String currentCategory = "全部";
    private String searchQuery = "";

    // 分类列表
    private final String[] categories = {"全部", "安防监控", "迎宾考勤", "生活工具", "开发工具"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new ApiClient();
        initViews();
        loadData();
    }

    private void initViews() {
        refresh = findViewById(R.id.refresh_layout);
        refresh.setOnRefreshListener(this::loadData);

        emptyText = findViewById(R.id.empty_text);
        emptyView = findViewById(R.id.empty_view);
        searchInput = findViewById(R.id.search_input);
        btnClear = findViewById(R.id.btn_clear);
        categoryTabs = findViewById(R.id.category_tabs);

        RecyclerView recyclerView = findViewById(R.id.recycler_apps);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
            adapter = new AppAdapter(this);
            recyclerView.setAdapter(adapter);
        }

        // 搜索
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                applyFilter();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClear.setOnClickListener(v -> {
            searchInput.setText("");
            searchQuery = "";
            applyFilter();
        });

        // 分类标签
        buildCategoryTabs();

        // 底部导航
        findViewById(R.id.tab_search).setOnClickListener(v -> {
            searchInput.requestFocus();
        });
        findViewById(R.id.tab_agent).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://clawclaw.tech/agent/"));
            startActivity(intent);
        });
        findViewById(R.id.tab_about).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://clawclaw.tech"));
            startActivity(intent);
        });
    }

    private void buildCategoryTabs() {
        categoryTabs.removeAllViews();
        for (String cat : categories) {
            TextView tab = new TextView(this);
            tab.setText(cat);
            tab.setTextSize(13);
            tab.setPadding(28, 10, 28, 10);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(10);
            tab.setLayoutParams(lp);

            if (cat.equals(currentCategory)) {
                tab.setTextColor(getResources().getColor(R.color.text));
                tab.setBackgroundResource(R.drawable.tab_active_bg);
            } else {
                tab.setTextColor(getResources().getColor(R.color.text3));
                tab.setBackgroundResource(R.drawable.tab_inactive_bg);
            }

            tab.setOnClickListener(v -> {
                currentCategory = cat;
                buildCategoryTabs();
                applyFilter();
            });

            categoryTabs.addView(tab);
        }
    }

    private void loadData() {
        refresh.setRefreshing(true);
        emptyView.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                List<AppInfo> apps = api.getProducts();
                runOnUiThread(() -> {
                    refresh.setRefreshing(false);
                    allApps = apps;
                    applyFilter();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    refresh.setRefreshing(false);
                    emptyView.setVisibility(View.VISIBLE);
                    emptyText.setText("加载失败: " + e.getMessage());
                    Toast.makeText(this, "网络错误，下拉重试", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void applyFilter() {
        List<AppInfo> filtered = new ArrayList<>();
        for (AppInfo app : allApps) {
            // 分类过滤
            if (!currentCategory.equals("全部") && !matchesCategory(app, currentCategory)) {
                continue;
            }
            // 搜索过滤
            if (!searchQuery.isEmpty()) {
                String name = (app.name_cn != null ? app.name_cn : app.name).toLowerCase();
                String desc = app.description != null ? app.description.toLowerCase() : "";
                if (!name.contains(searchQuery) && !desc.contains(searchQuery)) {
                    continue;
                }
            }
            filtered.add(app);
        }

        adapter.setApps(filtered);

        if (filtered.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            emptyText.setText(searchQuery.isEmpty() ? "该分类暂无产品" : "未找到\"" + searchQuery + "\"");
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    private boolean matchesCategory(AppInfo app, String category) {
        String name = app.name_cn != null ? app.name_cn : "";
        switch (category) {
            case "安防监控":
                return name.contains("防损") || name.contains("守护") || name.contains("抓拍") || name.contains("跌倒");
            case "迎宾考勤":
                return name.contains("迎宾") || name.contains("考勤") || name.contains("客流");
            case "生活工具":
                return name.contains("高尔夫") || name.contains("狗狗") || name.contains("长者");
            case "开发工具":
                return name.contains("网桥") || name.contains("NAS") || name.contains("代理") || name.contains("商店");
            default:
                return true;
        }
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
}
