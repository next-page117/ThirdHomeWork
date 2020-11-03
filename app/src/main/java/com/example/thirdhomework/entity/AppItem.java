package com.example.thirdhomework.entity;

import android.graphics.drawable.Drawable;

public class AppItem {
    public Drawable appIcon;
    public String appName;
    public String appUniqueName;

    public String getAppUniqueName() {
        return appUniqueName;
    }

    public void setAppUniqueName(String appUniqueName) {
        this.appUniqueName = appUniqueName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
