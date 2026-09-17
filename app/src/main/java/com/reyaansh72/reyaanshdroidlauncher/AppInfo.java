package com.reyaansh72.reyaanshdroidlauncher;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private String label;
    private String packageName;
    private Drawable icon;
    private String category;

    public AppInfo(String label, String packageName, Drawable icon, String category) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
        this.category = category;
    }

    public String getLabel() {
        return label;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getCategory() {
        return category;
    }
}
