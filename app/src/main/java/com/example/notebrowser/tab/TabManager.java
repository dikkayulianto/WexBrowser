package com.example.notebrowser.tab;

import java.util.ArrayList;
import java.util.List;

public class TabManager {
    public interface TabListener {
        void onTabChanged(BrowserTab tab, int index);
        void onTabAdded(BrowserTab tab, int index);
        void onTabClosed(BrowserTab tab, int index);
        void onTabsUpdated();
    }

    private final List<BrowserTab> tabs = new ArrayList<>();
    private int activeIndex = 0;
    private TabListener listener;

    public void setListener(TabListener listener) {
        this.listener = listener;
    }

    public List<BrowserTab> getTabs() {
        return tabs;
    }

    public int getTabCount() {
        return tabs.size();
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public BrowserTab getActiveTab() {
        if (tabs.isEmpty()) {
            return null;
        }
        if (activeIndex < 0 || activeIndex >= tabs.size()) {
            activeIndex = 0;
        }
        return tabs.get(activeIndex);
    }

    public BrowserTab createTab(String url, boolean isIncognito, boolean isHome) {
        String title = isHome ? (isIncognito ? "Tab Samaran" : "Halaman Utama") : "Tab Baru";
        BrowserTab newTab = new BrowserTab(title, url, isIncognito, isHome);
        tabs.add(newTab);
        activeIndex = tabs.size() - 1;

        if (listener != null) {
            listener.onTabAdded(newTab, activeIndex);
            listener.onTabChanged(newTab, activeIndex);
            listener.onTabsUpdated();
        }
        return newTab;
    }

    public void selectTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            activeIndex = index;
            if (listener != null) {
                listener.onTabChanged(tabs.get(index), index);
                listener.onTabsUpdated();
            }
        }
    }

    public void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        BrowserTab closedTab = tabs.remove(index);
        closedTab.destroyWebView();

        if (tabs.isEmpty()) {
            // Jika semua tab ditutup, buat tab beranda baru secara otomatis
            BrowserTab freshTab = new BrowserTab("Halaman Utama", "", false, true);
            tabs.add(freshTab);
            activeIndex = 0;
            if (listener != null) {
                listener.onTabClosed(closedTab, index);
                listener.onTabChanged(freshTab, 0);
                listener.onTabsUpdated();
            }
        } else {
            if (activeIndex >= tabs.size()) {
                activeIndex = tabs.size() - 1;
            } else if (activeIndex > index) {
                activeIndex--;
            }
            if (listener != null) {
                listener.onTabClosed(closedTab, index);
                listener.onTabChanged(tabs.get(activeIndex), activeIndex);
                listener.onTabsUpdated();
            }
        }
    }

    public void closeAllTabs() {
        for (BrowserTab tab : tabs) {
            tab.destroyWebView();
        }
        tabs.clear();
        BrowserTab freshTab = new BrowserTab("Halaman Utama", "", false, true);
        tabs.add(freshTab);
        activeIndex = 0;
        if (listener != null) {
            listener.onTabChanged(freshTab, 0);
            listener.onTabsUpdated();
        }
    }
}
