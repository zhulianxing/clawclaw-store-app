package com.clawclaw.store;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    private List<AppInfo> apps = new ArrayList<>();
    private final OnAppClickListener listener;

    public interface OnAppClickListener {
        void onAppClick(AppInfo app);
    }

    public AppAdapter(OnAppClickListener listener) {
        this.listener = listener;
    }

    public void setApps(List<AppInfo> apps) {
        this.apps = apps;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_card, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = apps.get(position);
        holder.bind(app, listener);
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        TextView icon, name, pkg, desc, price, dev, badge, btnGet;
        LinearLayout container;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            name = itemView.findViewById(R.id.app_name);
            pkg = itemView.findViewById(R.id.app_pkg);
            desc = itemView.findViewById(R.id.app_desc);
            price = itemView.findViewById(R.id.app_price);
            dev = itemView.findViewById(R.id.app_dev);
            badge = itemView.findViewById(R.id.app_badge);
            btnGet = itemView.findViewById(R.id.btn_get);
            container = itemView.findViewById(R.id.app_container);
        }

        void bind(AppInfo app, OnAppClickListener listener) {
            icon.setText(app.icon_emoji != null ? app.icon_emoji : "📦");
            name.setText(app.name_cn != null ? app.name_cn : app.name);

            // 描述
            String descText = app.description != null ? app.description : "";
            desc.setText(descText);
            desc.setVisibility(descText.isEmpty() ? View.GONE : View.VISIBLE);

            // 价格
            price.setText(app.getPriceDisplay());

            // 平台+大小
            String sizeText = app.apk_size != null ? app.apk_size : "--";
            String platformText = app.platform != null ? app.platform : "Android";
            dev.setText(platformText + " · " + sizeText);

            // 试用 badge
            String trial = app.getTrialText();
            if (trial != null && !trial.isEmpty()) {
                badge.setText(trial);
                badge.setVisibility(View.VISIBLE);
            } else {
                badge.setVisibility(View.GONE);
            }

            // 包名隐藏（保留 binding 不崩）
            pkg.setText(app.package_name);

            container.setOnClickListener(v -> {
                if (listener != null) listener.onAppClick(app);
            });
        }
    }
}
