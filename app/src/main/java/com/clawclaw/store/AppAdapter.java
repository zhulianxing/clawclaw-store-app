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
        TextView icon, name, pkg, desc, price, dev, badge;
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
            container = itemView.findViewById(R.id.app_container);
        }

        void bind(AppInfo app, OnAppClickListener listener) {
            icon.setText(app.icon_emoji != null ? app.icon_emoji : "📦");
            name.setText(app.name_cn != null ? app.name_cn : app.name);
            pkg.setText(app.package_name);
            desc.setText(app.description != null ? app.description : "");
            price.setText(app.getPriceDisplay());
            dev.setText(app.platform + " · " + (app.apk_size != null ? app.apk_size : "-"));

            String trial = app.getTrialText();
            if (trial != null && !trial.isEmpty()) {
                badge.setText("🕐 " + trial);
                badge.setVisibility(View.VISIBLE);
            } else {
                badge.setVisibility(View.GONE);
            }

            container.setOnClickListener(v -> {
                if (listener != null) listener.onAppClick(app);
            });
        }
    }
}
