package com.example.notebrowser.tab;

import android.webkit.WebView;
import java.util.UUID;

public class BrowserTab {
    private final String id;
    private String title;
    private String url;
    private final boolean isIncognito;
    private boolean isHome;
    private WebView webView;
    private int blockedCount = 0;

    public BrowserTab(String title, String url, boolean isIncognito, boolean isHome) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.url = url;
        this.isIncognito = isIncognito;
        this.isHome = isHome;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        if (title == null || title.isEmpty()) {
            return isHome ? (isIncognito ? "Tab Samaran" : "Halaman Utama") : "Memuat...";
        }
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isIncognito() {
        return isIncognito;
    }

    public boolean isHome() {
        return isHome;
    }

    public void setHome(boolean home) {
        isHome = home;
    }

    public WebView getWebView() {
        return webView;
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    public int getBlockedCount() {
        return blockedCount;
    }

    public void incrementBlockedCount() {
        blockedCount++;
    }

    public void resetBlockedCount() {
        blockedCount = 0;
    }

    public void destroyWebView() {
        if (webView != null) {
            try {
                if (isIncognito) {
                    webView.clearCache(true);
                    webView.clearHistory();
                    webView.clearFormData();
                }
                webView.stopLoading();
                webView.loadUrl("about:blank");
                webView.destroy();
            } catch (Exception ignored) {}
            webView = null;
        }
    }
}
