package com.reyaansh72.reyaanshdroidlauncher;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
    private List<AppInfo> fullAppsList;
    private List<AppInfo> filteredAppsList;
    private Context context;
    private OnAppLaunchListener launchListener;
    
    private int iconScale = 100;
    private boolean showLabels = true;
    private int labelSize = 12;
    private String fontStyle = "sans-serif";
    private String iconPackStyle = "default";
    private boolean singleLine = true;
    private String currentQuery = "";
    private String currentCategory = "All";

    public interface OnAppLaunchListener {
        void onAppLaunched();
    }

    public AppAdapter(Context context, List<AppInfo> appsList, OnAppLaunchListener listener) {
        this.context = context;
        this.fullAppsList = new ArrayList<>(appsList);
        this.filteredAppsList = new ArrayList<>(appsList);
        this.launchListener = listener;
    }

    public void updateSettings(int iconScale, boolean showLabels, int labelSize, String fontStyle, String iconPackStyle, boolean singleLine) {
        this.iconScale = iconScale;
        this.showLabels = showLabels;
        this.labelSize = labelSize;
        this.fontStyle = fontStyle;
        this.iconPackStyle = iconPackStyle;
        this.singleLine = singleLine;
        applyFilter();
    }

    public void filter(String query, String category) {
        this.currentQuery = query == null ? "" : query.toLowerCase().trim();
        this.currentCategory = category == null ? "All" : category;
        applyFilter();
    }

    private void applyFilter() {
        filteredAppsList = fullAppsList.stream()
                .filter(app -> {
                    if (!currentCategory.equals("All") && !app.getCategory().equalsIgnoreCase(currentCategory)) return false;
                    if (!currentQuery.isEmpty() && !app.getLabel().toLowerCase().contains(currentQuery)) return false;
                    return true;
                })
                .collect(Collectors.toList());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = filteredAppsList.get(position);
        holder.appLabel.setText(app.getLabel());
        
        applyIconPackFramework(app, holder.appIcon);
        
        holder.appLabel.setVisibility(showLabels ? View.VISIBLE : View.GONE);
        holder.appLabel.setTextSize(labelSize);
        holder.appLabel.setTypeface(Typeface.create(fontStyle, Typeface.NORMAL));
        holder.appLabel.setSingleLine(singleLine);
        
        float scale = iconScale / 100f;
        holder.appIcon.setScaleX(scale);
        holder.appIcon.setScaleY(scale);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
            if (intent != null) {
                context.startActivity(intent);
                if (launchListener != null) launchListener.onAppLaunched();
            }
        });
    }

    private void applyIconPackFramework(AppInfo app, ImageView iconView) {
        iconView.setImageDrawable(app.getIcon());
        iconView.setColorFilter(null);
        iconView.setAlpha(1.0f);
        if ("minimal".equals(iconPackStyle)) iconView.setAlpha(0.7f);
        else if ("neon".equals(iconPackStyle)) iconView.setColorFilter(Color.argb(40, 0, 255, 200));
        else if ("glass".equals(iconPackStyle)) iconView.setAlpha(0.5f);
    }

    @Override
    public int getItemCount() { return filteredAppsList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appLabel;
        public ViewHolder(@NonNull View v) {
            super(v);
            appIcon = v.findViewById(R.id.app_icon);
            appLabel = v.findViewById(R.id.app_label);
        }
    }
}
