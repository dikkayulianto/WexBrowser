package com.example.notebrowser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.notebrowser.dns.DnsController;
import com.example.notebrowser.dns.DnsProvider;
import com.example.notebrowser.history.HistoryItem;
import com.example.notebrowser.history.HistoryManager;
import com.example.notebrowser.search.SearchEngine;
import com.example.notebrowser.search.SearchPreferences;
import com.example.notebrowser.shield.VexShield;
import com.example.notebrowser.tab.BrowserTab;
import com.example.notebrowser.tab.TabManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private DnsController dnsController;
    private SearchPreferences searchPrefs;
    private TabManager tabManager;
    private VexShield vexShield;
    private HistoryManager historyManager;

    // Kontainer Tampilan
    private View layoutStartPage;
    private View layoutBrowserView;
    private FrameLayout webViewContainer;

    // Komponen Start Page
    private EditText etHomeSearch;
    private ImageButton btnHomeSearchGo;
    private TextView tvActiveDnsBadge;

    // Komponen Brave Privacy Dashboard di Start Page
    private TextView tvStatsBlocked;
    private TextView tvStatsData;
    private TextView tvStatsTime;

    // Komponen Browser View
    private EditText etBrowserUrl;
    private ProgressBar progressBar;
    private ImageView ivBrowserLock;
    private View btnBrowserShield;
    private TextView tvShieldBadgeCount;
    private ImageButton btnBrowserReload;

    // Komponen Bottom Navigation Bar
    private ImageButton btnNavBack;
    private ImageButton btnNavForward;
    private ImageButton btnNavHome;
    private View btnNavTabs;
    private TextView tvTabCount;
    private ImageButton btnNavMenu;

    private boolean isDesktopMode = false;
    private long backPressedTime = 0;

    // Komponen Pemutaran Video Layar Penuh (Fullscreen)
    private View fullscreenCustomView;
    private WebChromeClient.CustomViewCallback fullscreenCustomViewCallback;
    private FrameLayout fullscreenContainer;

    // Komponen Monetisasi Google AdMob
    private com.google.android.gms.ads.AdView adViewHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            dnsController = new DnsController(this);
            searchPrefs = new SearchPreferences(this);
            tabManager = new TabManager();
            vexShield = VexShield.getInstance(this);
            historyManager = HistoryManager.getInstance(this);
        } catch (Throwable t) {
            Log.e(TAG, "Init controller error", t);
        }

        initViews();
        setupTabManager();
        setupQuickAccess();
        setupBottomNav();
        setupSearchActions();
        setupAdMob();

        updateDnsBadgeUI();
        updateShieldDashboardUI();

        try {
            if (dnsController != null) {
                dnsController.applyDnsSettings(null);
            }
        } catch (Throwable t) {
            Log.e(TAG, "applyDnsSettings error", t);
        }

        // Inisialisasi halaman awal (Membuka Komivex secara default atau Start Page)
        if (com.example.notebrowser.komivex.KomivexConfig.DEFAULT_LOAD_KOMIVEX_ON_START) {
            openUrl(com.example.notebrowser.komivex.KomivexConfig.KOMIVEX_URL);
        } else {
            showStartPage();
            if (tabManager != null) {
                tabManager.createTab("", false, true);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adViewHome != null) {
            adViewHome.resume();
        }
        updateShieldDashboardUI();
    }

    @Override
    protected void onPause() {
        if (adViewHome != null) {
            adViewHome.pause();
        }
        super.onPause();
    }

    private void initViews() {
        layoutStartPage = findViewById(R.id.layoutStartPage);
        layoutBrowserView = findViewById(R.id.layoutBrowserView);
        webViewContainer = findViewById(R.id.webViewContainer);

        etHomeSearch = findViewById(R.id.etHomeSearch);
        btnHomeSearchGo = findViewById(R.id.btnHomeSearchGo);
        tvActiveDnsBadge = findViewById(R.id.tvActiveDnsBadge);

        tvStatsBlocked = findViewById(R.id.tvStatsBlocked);
        tvStatsData = findViewById(R.id.tvStatsData);
        tvStatsTime = findViewById(R.id.tvStatsTime);

        etBrowserUrl = findViewById(R.id.etBrowserUrl);
        progressBar = findViewById(R.id.progressBar);
        ivBrowserLock = findViewById(R.id.ivBrowserLock);
        btnBrowserShield = findViewById(R.id.btnBrowserShield);
        tvShieldBadgeCount = findViewById(R.id.tvShieldBadgeCount);
        btnBrowserReload = findViewById(R.id.btnBrowserReload);

        btnNavBack = findViewById(R.id.btnNavBack);
        btnNavForward = findViewById(R.id.btnNavForward);
        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavTabs = findViewById(R.id.btnNavTabs);
        tvTabCount = findViewById(R.id.tvTabCount);
        btnNavMenu = findViewById(R.id.btnNavMenu);

        if (btnBrowserShield != null) {
            btnBrowserShield.setOnClickListener(v -> showShieldBottomSheet());
        }
    }

    private void setupTabManager() {
        if (tabManager == null) return;
        tabManager.setListener(new TabManager.TabListener() {
            @Override
            public void onTabChanged(BrowserTab tab, int index) {
                attachTabToView(tab);
            }

            @Override
            public void onTabAdded(BrowserTab tab, int index) {
                updateTabBadgeUI();
            }

            @Override
            public void onTabClosed(BrowserTab tab, int index) {
                updateTabBadgeUI();
            }

            @Override
            public void onTabsUpdated() {
                updateTabBadgeUI();
            }
        });
    }

    private void updateTabBadgeUI() {
        if (tabManager == null) return;
        BrowserTab active = tabManager.getActiveTab();
        int count = tabManager.getTabCount();
        if (active != null && active.isIncognito()) {
            tvTabCount.setText("🕶️");
            btnNavTabs.setBackgroundResource(R.drawable.bg_tab_badge);
        } else {
            tvTabCount.setText(String.valueOf(count));
        }
    }

    private void attachTabToView(BrowserTab tab) {
        if (tab == null) return;

        updateShieldBadgeUI(tab.getBlockedCount());

        if (tab.isHome()) {
            showStartPage();
            return;
        }

        showBrowserView();

        webViewContainer.removeAllViews();

        WebView wv = tab.getWebView();
        if (wv == null) {
            try {
                wv = createWebViewForTab(tab);
                tab.setWebView(wv);
            } catch (Throwable t) {
                Log.e(TAG, "Gagal membuat WebView", t);
                Toast.makeText(this, "Gagal inisialisasi WebView: " + t.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (wv.getParent() != null) {
            ((ViewGroup) wv.getParent()).removeView(wv);
        }

        webViewContainer.addView(wv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        String url = tab.getUrl();
        if (url != null && !url.isEmpty()) {
            etBrowserUrl.setText(url);
            updateSslLock(url);
        }
        updateNavButtons();
    }

    private void injectCosmeticScriptIfNeeded(WebView view, String url) {
        if (view == null) return;
        String targetUrl = (url != null && !url.isEmpty()) ? url : view.getUrl();
        if (targetUrl == null) return;
        String host = VexShield.extractHost(targetUrl);
        if (host != null && (VexShield.isSearchOrPortal(host) || host.contains("komivex"))) {
            return;
        }
        view.loadUrl(VexShield.getCosmeticHidingScript());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView createWebViewForTab(BrowserTab tab) {
        WebView wv = new WebView(this);
        WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        if (isDesktopMode) {
            settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        }

        if (tab.isIncognito()) {
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setSaveFormData(false);
            settings.setSavePassword(false);
            settings.setGeolocationEnabled(false);
            try {
                CookieManager.getInstance().setAcceptCookie(false);
            } catch (Exception ignored) {}
        } else {
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            try {
                CookieManager.getInstance().setAcceptCookie(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true);
                }
            } catch (Exception ignored) {}
        }

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (url != null) {
                    tab.setUrl(url);
                }
                tab.resetBlockedCount();
                injectCosmeticScriptIfNeeded(view, url);

                if (tabManager.getActiveTab() == tab) {
                    progressBar.setVisibility(View.VISIBLE);
                    if (url != null) {
                        etBrowserUrl.setText(url);
                        updateSslLock(url);
                    }
                    updateShieldBadgeUI(0);
                    updateNavButtons();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null) {
                    tab.setUrl(url);
                }
                injectCosmeticScriptIfNeeded(view, url);

                // Catat riwayat jika bukan mode samaran & bukan halaman beranda
                if (url != null && !tab.isIncognito() && !tab.isHome() && historyManager != null) {
                    String title = view.getTitle();
                    if (title == null || title.isEmpty()) title = url;
                    historyManager.addHistory(title, url);
                }

                if (tabManager.getActiveTab() == tab) {
                    progressBar.setVisibility(View.GONE);
                    if (url != null) {
                        etBrowserUrl.setText(url);
                        updateSslLock(url);
                    }
                    updateShieldBadgeUI(tab.getBlockedCount());
                    updateShieldDashboardUI();
                    updateNavButtons();
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                try {
                    if (request != null && request.getUrl() != null) {
                        String reqUrl = request.getUrl().toString();
                        String currentUrl = (tab != null) ? tab.getUrl() : null;
                        Map<String, String> headers = request.getRequestHeaders();
                        String referer = (headers != null) ? headers.get("Referer") : null;
                        if (referer == null && headers != null) referer = headers.get("referer");

                        boolean isKomivex = (currentUrl != null && currentUrl.toLowerCase(Locale.ROOT).contains("komivex")) ||
                                            (referer != null && referer.toLowerCase(Locale.ROOT).contains("komivex")) ||
                                            reqUrl.toLowerCase(Locale.ROOT).contains("komivex");

                        if (!isKomivex && vexShield != null && vexShield.shouldBlock(reqUrl, currentUrl)) {
                            tab.incrementBlockedCount();
                            vexShield.recordBlock();
                            runOnUiThread(() -> {
                                if (tabManager.getActiveTab() == tab) {
                                    updateShieldBadgeUI(tab.getBlockedCount());
                                    updateShieldDashboardUI();
                                }
                            });
                            return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                        }

                        // Kompatibilitas script Komivex di WebView versi lama (Android 5.0 - 7.1.2)
                        WebResourceResponse komivexResp = handleKomivexScriptIntercept(reqUrl);
                        if (komivexResp != null) {
                            return komivexResp;
                        }

                        // Sterilkan halaman pemutar video streaming dari overlay iklan dan script popup liar
                        WebResourceResponse playerResp = handlePlayerIntercept(reqUrl, currentUrl, request.getRequestHeaders(), request.getMethod());
                        if (playerResp != null) {
                            return playerResp;
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Error in shouldInterceptRequest", t);
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                try {
                    if (url != null) {
                        String currentUrl = (tab != null) ? tab.getUrl() : null;
                        boolean isKomivex = (currentUrl != null && currentUrl.toLowerCase(Locale.ROOT).contains("komivex")) ||
                                            url.toLowerCase(Locale.ROOT).contains("komivex");

                        if (!isKomivex && vexShield != null && vexShield.shouldBlock(url, currentUrl)) {
                            tab.incrementBlockedCount();
                            vexShield.recordBlock();
                            runOnUiThread(() -> {
                                if (tabManager.getActiveTab() == tab) {
                                    updateShieldBadgeUI(tab.getBlockedCount());
                                    updateShieldDashboardUI();
                                }
                            });
                            return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                        }

                        // Kompatibilitas script Komivex di WebView versi lama (Android 5.0 - 7.1.2)
                        WebResourceResponse komivexResp = handleKomivexScriptIntercept(url);
                        if (komivexResp != null) {
                            return komivexResp;
                        }

                        // Sterilkan halaman pemutar video streaming dari overlay iklan dan script popup liar
                        WebResourceResponse playerResp = handlePlayerIntercept(url, currentUrl, null, "GET");
                        if (playerResp != null) {
                            return playerResp;
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Error in shouldInterceptRequest(String)", t);
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrlRequest(view, url, tab, null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) return false;
                return handleUrlRequest(view, request.getUrl().toString(), tab, request);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.w(TAG, "WebView Error (" + errorCode + "): " + description + " URL: " + failingUrl);

                String errorHtml = "<html><body style='background-color:#0F1016;color:#F3F4F6;font-family:sans-serif;padding:24px;text-align:center;'>"
                        + "<div style='font-size:42px;margin-bottom:12px;'>🌐⚠️</div>"
                        + "<h2 style='color:#EF4444;margin-bottom:8px;'>Halaman Gagal Dimuat</h2>"
                        + "<p style='color:#9CA3AF;font-size:13px;line-height:1.5;word-break:break-all;'>" + failingUrl + "</p>"
                        + "<div style='margin:20px 0;padding:14px;background:#181A24;border-radius:10px;text-align:left;border:1px solid #282A3A;'>"
                        + "<p style='margin:0;font-size:13px;color:#818CF8;font-weight:bold;'>💡 Tips Akses Situs Diblokir:</p>"
                        + "<p style='margin:6px 0 0 0;font-size:12px;color:#D1D5DB;line-height:1.5;'>DuckDuckGo atau situs ini mungkin diblokir oleh ISP Anda.<br>"
                        + "1. Buka menu <b>☰</b> di pojok kanan bawah.<br>"
                        + "2. Pilih <b>Pengaturan DNS</b> lalu aktifkan <b>Cloudflare (1.1.1.1)</b> atau <b>AdGuard</b>.<br>"
                        + "3. Tekan tombol Muat Ulang di bawah ini.</p>"
                        + "</div>"
                        + "<a href='" + failingUrl + "' style='display:inline-block;padding:12px 24px;background:#6366F1;color:white;text-decoration:none;border-radius:8px;font-weight:bold;font-size:14px;'>Coba Muat Ulang</a>"
                        + "</body></html>";
                view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
            }
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                String currentUrl = (tab != null) ? tab.getUrl() : view.getUrl();
                String currentHost = VexShield.extractHost(currentUrl);

                // 1. Blokir mutlak pembukaan jendela pop-up baru pada situs streaming / pemutar film ilegal
                if (currentHost != null && (isStreamingOrMovieSite(currentHost) || isPlayerHost(currentHost, null))) {
                    Log.w(TAG, "VexShield memblokir onCreateWindow pada situs streaming/player: " + currentHost);
                    return false;
                }

                // 2. Blokir pop-up otomatis tanpa sentuhan pengguna (misal script timer iklan popunder)
                if (!isUserGesture) {
                    return false;
                }

                // 3. Untuk sentuhan pengguna di situs normal (seperti Linktree, sosmed, portal, blog):
                // Periksa apakah target URL langsung terdeteksi dari elemen yang disentuh (HitTestResult)
                WebView.HitTestResult hitTest = view.getHitTestResult();
                if (hitTest != null) {
                    int type = hitTest.getType();
                    if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                        String hitUrl = hitTest.getExtra();
                        if (hitUrl != null && !hitUrl.trim().isEmpty()) {
                            if (!handleUrlRequest(view, hitUrl, tab, null)) {
                                view.loadUrl(hitUrl);
                            }
                            return false;
                        }
                    }
                }

                // 4. Jika URL tidak tersedia di HitTestResult (misal dipicu via window.open oleh framework React/Next.js/Linktree):
                // Tangkap navigasi target menggunakan WebView perantara ringan (tempWv)
                if (resultMsg != null && resultMsg.obj instanceof WebView.WebViewTransport) {
                    try {
                        WebView tempWv = new WebView(MainActivity.this);
                        tempWv.getSettings().setJavaScriptEnabled(true);
                        tempWv.setWebViewClient(new WebViewClient() {
                            private boolean handled = false;

                            private void processTarget(String url, WebResourceRequest req) {
                                if (handled) return;
                                handled = true;
                                tempWv.stopLoading();
                                tempWv.destroy();
                                if (url != null && !url.trim().isEmpty()) {
                                    if (!handleUrlRequest(view, url, tab, req)) {
                                        view.loadUrl(url);
                                    }
                                }
                            }

                            @Override
                            public boolean shouldOverrideUrlLoading(WebView wv, String url) {
                                processTarget(url, null);
                                return true;
                            }

                            @Override
                            public boolean shouldOverrideUrlLoading(WebView wv, WebResourceRequest request) {
                                if (request != null && request.getUrl() != null) {
                                    processTarget(request.getUrl().toString(), request);
                                }
                                return true;
                            }

                            @Override
                            public void onPageStarted(WebView wv, String url, Bitmap favicon) {
                                processTarget(url, null);
                            }
                        });

                        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                        transport.setWebView(tempWv);
                        resultMsg.sendToTarget();
                        return true;
                    } catch (Exception e) {
                        Log.e(TAG, "Gagal menangani onCreateWindow", e);
                        return false;
                    }
                }

                return false;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress >= 25) {
                    injectCosmeticScriptIfNeeded(view, view.getUrl());
                }
                if (tabManager.getActiveTab() == tab) {
                    progressBar.setProgress(newProgress);
                    progressBar.setVisibility((newProgress > 0 && newProgress < 100) ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (title != null && !title.isEmpty()) {
                    tab.setTitle(title);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                showFullscreenVideo(view, callback);
            }

            @Override
            public void onHideCustomView() {
                hideFullscreenVideo();
            }
        });

        wv.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (url == null) return;
            if (vexShield != null && vexShield.isPopupOrAdUrl(url)) {
                Log.w(TAG, "VexShield memblokir unduhan iklan liar: " + url);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Unduhan pop-up otomatis diblokir 🛡️", Toast.LENGTH_SHORT).show());
                return;
            }
            runOnUiThread(() -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Konfirmasi Unduhan")
                        .setMessage("Apakah Anda ingin mengunduh file ini?\n\n" + url)
                        .setPositiveButton("Unduh", (dialog, which) -> {
                            try {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(url));
                                startActivity(i);
                            } catch (Exception e) {
                                Toast.makeText(MainActivity.this, "Gagal mengunduh file", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            });
        });

        return wv;
    }

    private boolean handleUrlRequest(WebView view, String url, BrowserTab tab, WebResourceRequest request) {
        if (url == null || url.trim().isEmpty()) return false;
        String cleanUrl = url.trim();

        // 1. Tangani skema aplikasi eksternal (intent://, market://, tg://, whatsapp://, tel:, mailto:, sms:)
        if (cleanUrl.startsWith("intent://") || cleanUrl.startsWith("market://") ||
            cleanUrl.startsWith("tg://") || cleanUrl.startsWith("whatsapp://") ||
            cleanUrl.startsWith("line://") || cleanUrl.startsWith("viber://") ||
            cleanUrl.startsWith("tel:") || cleanUrl.startsWith("mailto:") || cleanUrl.startsWith("sms:")) {
            return handleExternalScheme(cleanUrl);
        }

        String currentUrl = (tab != null) ? tab.getUrl() : null;
        String currentHost = VexShield.extractHost(currentUrl);

        boolean isKomivex = (currentUrl != null && currentUrl.toLowerCase(Locale.ROOT).contains("komivex")) ||
                            cleanUrl.toLowerCase(Locale.ROOT).contains("komivex");

        if (isKomivex) {
            return false;
        }

        // 2. Jika pengguna sedang berada di situs shortlink/safelink (misal ketik.live),
        // jangan diblokir agar bisa meneruskan (redirect) ke alamat aslinya!
        if (isRedirectorOrSafelink(currentHost)) {
            return false;
        }

        // 3. Blokir jika VexShield mendeteksi sebagai URL iklan atau pop-up
        if (vexShield != null && vexShield.isPopupOrAdUrl(cleanUrl, currentUrl)) {
            if (tab != null) {
                tab.incrementBlockedCount();
            }
            vexShield.recordBlock();
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "Pop-up iklan diblokir 🛡️", Toast.LENGTH_SHORT).show();
                if (tabManager != null && tabManager.getActiveTab() == tab) {
                    updateShieldBadgeUI(tab.getBlockedCount());
                    updateShieldDashboardUI();
                }
            });
            return true; // DIBLOKIR!
        }

        // 4. Pencegahan Pembajakan Lompatan Domain dari situs streaming film (Brave-style Shield)
        if (currentHost != null && isStreamingOrMovieSite(currentHost)) {
            String targetHost = VexShield.extractHost(cleanUrl);
            if (targetHost != null && !targetHost.equalsIgnoreCase(currentHost)) {
                // Di situs film, JANGAN PERNAH izinkan lompatan tab utama ke domain pihak ketiga manapun (seperti decafeligiblyhad, whomeeno, al5sm, dll)
                // KECUALI jika pengguna mengeklik shortlink resmi (seperti ketik.live) atau aplikasi luar (tg://, whatsapp://)
                if (!isRedirectorOrSafelink(targetHost) && !isWhitelistedThirdParty(targetHost)) {
                    if (tab != null) {
                        tab.incrementBlockedCount();
                    }
                    vexShield.recordBlock();
                    Log.w(TAG, "VexShield memblokir pembajakan tab film: " + currentHost + " -> " + targetHost);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Pengalihan iklan diblokir 🛡️", Toast.LENGTH_SHORT).show();
                        if (tabManager != null && tabManager.getActiveTab() == tab) {
                            updateShieldBadgeUI(tab.getBlockedCount());
                            updateShieldDashboardUI();
                        }
                    });
                    return true; // DIBLOKIR MUTLAK! Tab film tetap berada di halaman film!
                }
            }
        }

        return false;
    }

    private boolean isRedirectorOrSafelink(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(java.util.Locale.ROOT);
        return h.contains("ketik.live") || h.contains("shortlink") ||
               h.contains("safelink") || h.contains("coeg.in") ||
               h.contains("linkduit") || h.contains("aneka.link");
    }

    private boolean isStreamingOrMovieSite(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(java.util.Locale.ROOT);
        return h.contains("dutamovie") || h.contains("algarvebuzz") ||
               h.contains("actors-pictures") || h.contains("lk21") ||
               h.contains("layarkaca") || h.contains("rebahin") ||
               h.contains("indoxxi") || h.contains("idlix");
    }

    private boolean isSearchOrPortal(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(java.util.Locale.ROOT);
        return h.contains("google.") || h.contains("duckduckgo.") ||
               h.contains("bing.") || h.contains("yahoo.") ||
               h.contains("wikipedia.") || h.contains("twitter.") ||
               h.contains("reddit.") || h.contains("detik.") ||
               h.contains("kompas.") || h.contains("youtube.") ||
               h.contains("linktr.ee") || h.contains("linktree.") ||
               h.contains("heylink.me") || h.contains("bio.link") ||
               h.contains("beacons.ai");
    }

    private boolean isWhitelistedThirdParty(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.ROOT);
        return h.contains("telegram.org") || h.contains("t.me") ||
               h.contains("whatsapp.com") || h.contains("google.com") ||
               h.contains("accounts.google.com");
    }

    private boolean isPlayerHost(String host, String url) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.ROOT);
        if (h.contains("ketik.live") || h.contains("t.me") || h.contains("telegram.org") || isSearchOrPortal(h)) {
            return false;
        }
        return h.contains("abyssplayer") || h.contains("abyss.to") ||
               h.contains("embed4me") || h.contains("streamwish") ||
               h.contains("filelions") || h.contains("vidhide") ||
               h.contains("dood") || h.contains("streamtape") ||
               h.contains("dropload") || h.contains("streamvid") ||
               h.contains("turbovid") || h.contains("vidspeed") ||
               h.contains("playercdn") || h.contains("player-v2") ||
               h.contains("embedpyrox") || h.contains("iamcdn.net") ||
               h.startsWith("player.") ||
               (url != null && (url.contains("/e/") || url.contains("/embed/") || url.contains("/video/")));
    }

    private WebResourceResponse handlePlayerIntercept(String url, String pageUrl, Map<String, String> requestHeaders, String method) {
        if (url == null || !url.startsWith("http")) return null;
        if (method != null && !"GET".equalsIgnoreCase(method)) return null;

        String lower = url.toLowerCase(Locale.ROOT);
        // Lewati aset statis media & biner murni
        if (lower.endsWith(".m3u8") || lower.endsWith(".ts") || lower.endsWith(".mp4") ||
            lower.endsWith(".webm") || lower.endsWith(".js") || lower.endsWith(".css") ||
            lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".svg") ||
            lower.endsWith(".woff") || lower.endsWith(".woff2") || lower.endsWith(".ttf") ||
            lower.endsWith(".ico") || lower.contains("/hls/") || lower.contains(".m3u8?")) {
            return null;
        }

        String host = VexShield.extractHost(url);
        if (host == null) return null;

        if (!isPlayerHost(host, lower)) return null;

        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                try {
                    httpsConn.setSSLSocketFactory(com.example.notebrowser.dns.DnsResolver.getLenientSslSocketFactory());
                    httpsConn.setHostnameVerifier((hostname, session) -> true);
                } catch (Exception ignored) {}
            }
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);

            String ua = "Mozilla/5.0 (Linux; Android 7.1.2; SM-N900 Build/N2G47H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36";
            conn.setRequestProperty("User-Agent", ua);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7");

            if (pageUrl != null && !pageUrl.isEmpty()) {
                conn.setRequestProperty("Referer", pageUrl);
            } else {
                conn.setRequestProperty("Referer", "https://algarvebuzz.com/");
            }

            if (requestHeaders != null) {
                for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                    String k = entry.getKey();
                    if (!"User-Agent".equalsIgnoreCase(k) && !"Accept-Encoding".equalsIgnoreCase(k) && !"Referer".equalsIgnoreCase(k)) {
                        conn.setRequestProperty(k, entry.getValue());
                    }
                }
            }

            try {
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null && !cookie.isEmpty()) {
                    conn.setRequestProperty("Cookie", cookie);
                }
            } catch (Exception ignored) {}

            int code = conn.getResponseCode();
            if (code >= 400) {
                return null;
            }

            String contentType = conn.getContentType();
            if (contentType != null && !contentType.toLowerCase(Locale.ROOT).contains("html")) {
                return null;
            }

            try {
                Map<String, List<String>> headerFields = conn.getHeaderFields();
                if (headerFields != null && headerFields.containsKey("Set-Cookie")) {
                    List<String> cookies = headerFields.get("Set-Cookie");
                    if (cookies != null) {
                        for (String c : cookies) {
                            CookieManager.getInstance().setCookie(url, c);
                        }
                    }
                }
            } catch (Exception ignored) {}

            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            conn.disconnect();

            String html = sb.toString();
            String sanitized = sanitizePlayerHtml(html, url);
            return new WebResourceResponse("text/html", "UTF-8", new ByteArrayInputStream(sanitized.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            Log.e(TAG, "Gagal meng-intercept player: " + url, e);
            return null;
        }
    }

    private WebResourceResponse handleKomivexScriptIntercept(String url) {
        if (url == null || !url.contains("komivex.my.id") || !url.contains("app.js")) {
            return null;
        }
        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                try {
                    httpsConn.setSSLSocketFactory(com.example.notebrowser.dns.DnsResolver.getLenientSslSocketFactory());
                    httpsConn.setHostnameVerifier((hostname, session) -> true);
                } catch (Exception ignored) {}
            }
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 7.1.2; SM-N900 Build/N2G47H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36");

            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            conn.disconnect();

            String js = sb.toString();
            // Patch optional chaining yang menyebabkan error sintaks di WebView Android lama
            js = js.replace("mangaDetailsCache[mangaId]?.cover", "((mangaDetailsCache[mangaId] && mangaDetailsCache[mangaId].cover))");
            js = js.replaceAll("([a-zA-Z0-9_\\]]+)\\?\\.([a-zA-Z0-9_]+)", "($1 ? $1.$2 : undefined)");

            byte[] bytes = js.getBytes(StandardCharsets.UTF_8);
            return new WebResourceResponse("application/javascript", "UTF-8", new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            Log.e(TAG, "Gagal meng-intercept script Komivex: " + e.getMessage());
            return null;
        }
    }

    private String sanitizePlayerHtml(String html, String playerUrl) {
        if (html == null) return "";

        // 1. Matikan elemen overlay transparan & fake playback icon
        html = html.replace("<div id=\"overlay\">", "<div id=\"overlay\" style=\"display:none!important;pointer-events:none!important;visibility:hidden!important;width:0!important;height:0!important;z-index:-99999!important;\">");
        html = html.replace("<div id='overlay'>", "<div id='overlay' style='display:none!important;pointer-events:none!important;visibility:hidden!important;width:0!important;height:0!important;z-index:-99999!important;'>");
        html = html.replace("<div id=\"playback\">", "<div id=\"playback\" style=\"display:none!important;pointer-events:none!important;visibility:hidden!important;width:0!important;height:0!important;\">");
        html = html.replace("<div id='playback'>", "<div id='playback' style='display:none!important;pointer-events:none!important;visibility:hidden!important;width:0!important;height:0!important;'>");

        // 2. Kosongkan daftar URL iklan popup agar tidak ada link iklan yang dipicu
        html = html.replaceAll("(var|let|const)\\s+urls\\s*=\\s*\\[", "var urls = []; var _orig_urls = [");
        html = html.replaceAll("urls\\s*=\\s*\\[", "urls = []; var _orig_urls2 = [");
        html = html.replaceAll("popups:\\s*\\[[^\\]]*\\]", "popups: []");

        // 3. Matikan jebakan anti-adblock 'track.window >= 2' yang menghancurkan jwplayer
        html = html.replace("track.window >= 2", "false");
        html = html.replace("track.window>=2", "false");
        html = html.replace("track.window > 1", "false");
        html = html.replace("track.window>1", "false");
        html = html.replace("track.window++", "/* track.window++ */");
        html = html.replace("track.window ++", "/* track.window ++ */");
        html = html.replace("track.close++", "/* track.close++ */");

        // 4. Izinkan pemutaran pada perangkat layar sentuh Android (hilangkan !isTouchScreen blocker)
        html = html.replace("&& !isTouchScreen", "");
        html = html.replace("&&!isTouchScreen", "");

        // 5. Cegah pengalihan top.location liar
        html = html.replace("top.location == self.location", "false");
        html = html.replace("top.location==self.location", "false");
        html = html.replace("top.location === self.location", "false");

        // 6. Matikan FuckAdBlock detector
        html = html.replace("https://cdnjs.cloudflare.com/ajax/libs/fuckadblock/3.2.1/fuckadblock.min.js", "data:text/javascript;base64,");

        // 7. Injeksi skrip pelindung pemutar & stylesheet pembersih overlay ke dalam <head>
        String injection = "<style>"
                + "#overlay, #playback, [id*='play-overlay'], [class*='play-overlay'], "
                + "[id*='player-overlay'], [class*='player-overlay'], [id*='click-layer'], "
                + "[class*='click-layer'], [class*='jw-ad'], [id*='jw-ad'], .jw-ad-container, "
                + ".jw-ad-layer, [class*='vignette'], [id*='vignette'] {"
                + "    display: none !important;"
                + "    visibility: hidden !important;"
                + "    pointer-events: none !important;"
                + "    width: 0 !important;"
                + "    height: 0 !important;"
                + "    opacity: 0 !important;"
                + "    z-index: -99999 !important;"
                + "}"
                + "</style>"
                + "<script>"
                + "window.urls = [];"
                + "window.canRunAds = true;"
                + "window.isAdBlockActive = false;"
                + "window.adblock = false;"
                + "window.adblocker = false;"
                + "window.isTouchScreen = false;"
                + "window.fuckAdBlock = { onDetected: function(){}, onNotDetected: function(f){ if(typeof f==='function') f(); } };"
                + "window.FuckAdBlock = window.fuckAdBlock;"
                + "window.open = function() { return { closed: true, focus: function(){} }; };"
                + "try { Object.defineProperty(window, 'open', { value: function() { return { closed: true, focus: function(){} }; }, writable: false }); } catch(e) {}"
                + "function purgeOverlays() {"
                + "    try {"
                + "        var ov = document.getElementById('overlay');"
                + "        if (ov) { ov.style.display='none'; ov.style.pointerEvents='none'; ov.remove(); }"
                + "        var pb = document.getElementById('playback');"
                + "        if (pb) { pb.style.display='none'; pb.style.pointerEvents='none'; pb.remove(); }"
                + "        var all = document.querySelectorAll('[id*=\"overlay\"], [class*=\"overlay\"], [id*=\"click-layer\"], [class*=\"click-layer\"]');"
                + "        for (var i = 0; i < all.length; i++) {"
                + "            if (all[i].id !== 'player' && all[i].tagName !== 'VIDEO' && all[i].tagName !== 'BODY') {"
                + "                all[i].style.setProperty('display', 'none', 'important');"
                + "                all[i].style.setProperty('pointer-events', 'none', 'important');"
                + "            }"
                + "        }"
                + "    } catch(e) {}"
                + "}"
                + "window.addEventListener('click', function(e) {"
                + "    purgeOverlays();"
                + "    try {"
                + "        if (typeof jwplayer !== 'undefined' && typeof jwplayer().play === 'function') {"
                + "            var s = jwplayer().getState();"
                + "            if (s !== 'playing') { jwplayer().play(); }"
                + "        }"
                + "    } catch(err) {}"
                + "}, true);"
                + "document.addEventListener('DOMContentLoaded', purgeOverlays);"
                + "setInterval(purgeOverlays, 300);"
                + "</script>";

        if (html.contains("</head>")) {
            html = html.replace("</head>", injection + "</head>");
        } else {
            html = injection + html;
        }

        return html;
    }

    public void showFullscreenVideo(View view, WebChromeClient.CustomViewCallback callback) {
        if (fullscreenCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }
        fullscreenCustomView = view;
        fullscreenCustomViewCallback = callback;

        if (fullscreenContainer == null) {
            fullscreenContainer = new FrameLayout(this);
            fullscreenContainer.setBackgroundColor(Color.BLACK);
            ((ViewGroup) getWindow().getDecorView()).addView(fullscreenContainer,
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        fullscreenContainer.addView(fullscreenCustomView,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fullscreenContainer.setVisibility(View.VISIBLE);

        if (layoutBrowserView != null) layoutBrowserView.setVisibility(View.GONE);
        if (layoutStartPage != null) layoutStartPage.setVisibility(View.GONE);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    public void hideFullscreenVideo() {
        if (fullscreenCustomView == null) return;

        if (fullscreenContainer != null) {
            fullscreenContainer.removeView(fullscreenCustomView);
            fullscreenContainer.setVisibility(View.GONE);
        }
        fullscreenCustomView = null;

        if (fullscreenCustomViewCallback != null) {
            fullscreenCustomViewCallback.onCustomViewHidden();
            fullscreenCustomViewCallback = null;
        }

        if (layoutBrowserView != null) layoutBrowserView.setVisibility(View.VISIBLE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    private boolean handleExternalScheme(String url) {
        if (url == null) return false;

        // 1. Skema aplikasi media & chatting (Telegram, WhatsApp, Line, dll)
        if (url.startsWith("tg://") || url.startsWith("whatsapp://") ||
            url.startsWith("line://") || url.startsWith("viber://")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            } catch (Exception e) {
                Toast.makeText(this, "Aplikasi tidak ditemukan untuk membuka tautan ini", Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        // 2. Penanganan Intent (Cegah pembajakan ke Chrome / browser eksternal)
        if (url.startsWith("intent://")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

                // Blokir jika intent menargetkan browser eksternal (Chrome, Browser bawaan, dll)
                String pkg = intent.getPackage();
                if (pkg != null) {
                    String lowerPkg = pkg.toLowerCase(java.util.Locale.ROOT);
                    if (lowerPkg.contains("chrome") || lowerPkg.contains("browser") ||
                        lowerPkg.contains("sbrowser") || lowerPkg.contains("opera") ||
                        lowerPkg.contains("firefox") || lowerPkg.contains("webview")) {
                        Log.w(TAG, "VexShield memblokir intent pembajakan ke Chrome/Browser: " + pkg);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Pop-up pengalihan ke Chrome diblokir 🛡️", Toast.LENGTH_SHORT).show());
                        return true; // DIBLOKIR!
                    }
                }

                // Cek fallback URL & data URI di dalam intent
                String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                if (fallbackUrl != null && vexShield != null && vexShield.isPopupOrAdUrl(fallbackUrl)) {
                    Log.w(TAG, "VexShield memblokir fallback URL iklan di intent: " + fallbackUrl);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Pop-up iklan diblokir 🛡️", Toast.LENGTH_SHORT).show());
                    return true;
                }

                Uri dataUri = intent.getData();
                if (dataUri != null) {
                    String dataString = dataUri.toString();
                    if (vexShield != null && vexShield.isPopupOrAdUrl(dataString)) {
                        Log.w(TAG, "VexShield memblokir data URI iklan di intent: " + dataString);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Pop-up iklan diblokir 🛡️", Toast.LENGTH_SHORT).show());
                        return true;
                    }
                }

                // Jika intent mengarah ke skema http / https murni, muat di VexBrowser sendiri
                if ("http".equalsIgnoreCase(intent.getScheme()) || "https".equalsIgnoreCase(intent.getScheme())) {
                    String webUrl = intent.getDataString();
                    if (webUrl != null) {
                        if (vexShield != null && vexShield.isPopupOrAdUrl(webUrl)) {
                            return true;
                        }
                        openUrl(webUrl);
                        return true;
                    }
                }

                // Hanya jalankan jika aplikasi spesifik non-browser terpasang (misal Telegram, YouTube, Drive, dll)
                if (pkg != null && !pkg.isEmpty()) {
                    if (pkg.contains("vending") && url.toLowerCase(java.util.Locale.ROOT).contains("slot")) {
                        return true; // Blokir tautan Play Store iklan judi
                    }
                    try {
                        startActivity(intent);
                        return true;
                    } catch (Exception ex) {
                        if (fallbackUrl != null && !fallbackUrl.isEmpty()) {
                            openUrl(fallbackUrl);
                            return true;
                        }
                    }
                }

                return true; // Konsumsi intent agar tidak dialihkan sistem ke Chrome
            } catch (Exception e) {
                Log.w(TAG, "Gagal mengurai URI intent: " + url, e);
                return true;
            }
        }

        // 3. Penanganan Market (Google Play Store)
        if (url.startsWith("market://")) {
            if (vexShield != null && vexShield.isPopupOrAdUrl(url)) {
                Log.w(TAG, "VexShield memblokir iklan tautan market: " + url);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Pop-up Play Store iklan diblokir 🛡️", Toast.LENGTH_SHORT).show());
                return true;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            } catch (Exception ignored) {
                return true;
            }
        }

        // 4. Skema komunikasi sah (telepon, email, sms)
        if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            } catch (Exception ignored) {
                return true;
            }
        }

        return false;
    }

    private void setupQuickAccess() {
        View cardKomivex = findViewById(R.id.cardKomivexFeatured);
        if (cardKomivex != null) {
            cardKomivex.setOnClickListener(v -> openUrl(com.example.notebrowser.komivex.KomivexConfig.KOMIVEX_URL));
        }
        View qaKomivex = findViewById(R.id.qaKomivex);
        if (qaKomivex != null) {
            qaKomivex.setOnClickListener(v -> openUrl(com.example.notebrowser.komivex.KomivexConfig.KOMIVEX_URL));
        }

        findViewById(R.id.qaGoogle).setOnClickListener(v -> openUrl("https://www.google.com"));
        findViewById(R.id.qaYoutube).setOnClickListener(v -> openUrl("https://www.youtube.com"));
        findViewById(R.id.qaWikipedia).setOnClickListener(v -> openUrl("https://www.wikipedia.org"));
        findViewById(R.id.qaTwitter).setOnClickListener(v -> openUrl("https://twitter.com"));
        findViewById(R.id.qaReddit).setOnClickListener(v -> openUrl("https://www.reddit.com"));
        findViewById(R.id.qaIndodax).setOnClickListener(v -> openUrl("https://indodax.com"));
        findViewById(R.id.qaDetik).setOnClickListener(v -> openUrl("https://www.detik.com"));
        findViewById(R.id.qaTradingView).setOnClickListener(v -> openUrl("https://www.tradingview.com"));

        // Pintasan Afiliasi & Belanja Hemat (Monetisasi)
        findViewById(R.id.qaShopee).setOnClickListener(v -> openUrl(com.example.notebrowser.monetization.AffiliateConfig.SHOPEE_URL));
        findViewById(R.id.qaTokopedia).setOnClickListener(v -> openUrl(com.example.notebrowser.monetization.AffiliateConfig.TOKOPEDIA_URL));
        findViewById(R.id.qaLazada).setOnClickListener(v -> openUrl(com.example.notebrowser.monetization.AffiliateConfig.LAZADA_URL));
        findViewById(R.id.qaTraveloka).setOnClickListener(v -> openUrl(com.example.notebrowser.monetization.AffiliateConfig.TRAVELOKA_URL));
        findViewById(R.id.btnPromoShopee).setOnClickListener(v -> openUrl(com.example.notebrowser.monetization.AffiliateConfig.SHOPEE_PROMO_URL));
        findViewById(R.id.btnPromoTokopedia).setOnClickListener(v -> openUrl(com.example.notebrowser.monetization.AffiliateConfig.TOKOPEDIA_PROMO_URL));

        tvActiveDnsBadge.setOnClickListener(v -> showDnsSelectionDialog());
    }

    private void setupAdMob() {
        if (!com.example.notebrowser.monetization.AdMobConfig.ENABLE_ADMOB_BANNER) {
            View container = findViewById(R.id.layoutAdMobContainer);
            if (container != null) container.setVisibility(View.GONE);
            return;
        }

        try {
            com.google.android.gms.ads.MobileAds.initialize(this, initializationStatus -> {
                Log.d(TAG, "AdMob MobileAds initialized successfully");
            });

            adViewHome = findViewById(R.id.adViewHome);
            if (adViewHome != null) {
                adViewHome.setAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        Log.d(TAG, "AdMob Banner loaded successfully!");
                        View container = findViewById(R.id.layoutAdMobContainer);
                        if (container != null) container.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        Log.w(TAG, "AdMob Banner failed to load: " + loadAdError.getMessage() + " (Code: " + loadAdError.getCode() + ")");
                    }
                });
                com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
                adViewHome.loadAd(adRequest);
            }
        } catch (Exception e) {
            Log.e(TAG, "Gagal menginisialisasi AdMob", e);
        }
    }

    private void setupBottomNav() {
        btnNavBack.setOnClickListener(v -> handleBackNavigation());

        btnNavForward.setOnClickListener(v -> {
            BrowserTab active = tabManager.getActiveTab();
            if (active != null && active.getWebView() != null && active.getWebView().canGoForward()) {
                active.getWebView().goForward();
            }
        });

        btnNavHome.setOnClickListener(v -> {
            BrowserTab active = tabManager.getActiveTab();
            if (active != null) {
                String currentUrl = active.getUrl();
                if (com.example.notebrowser.komivex.KomivexConfig.DEFAULT_LOAD_KOMIVEX_ON_START &&
                        (currentUrl == null || !currentUrl.contains("komivex.my.id"))) {
                    openUrl(com.example.notebrowser.komivex.KomivexConfig.KOMIVEX_URL);
                } else {
                    active.setHome(true);
                    showStartPage();
                }
            } else {
                showStartPage();
            }
        });

        btnNavHome.setOnLongClickListener(v -> {
            BrowserTab active = tabManager.getActiveTab();
            if (active != null) {
                active.setHome(true);
            }
            showStartPage();
            return true;
        });

        btnNavTabs.setOnClickListener(v -> showTabsDialog());

        btnNavMenu.setOnClickListener(v -> showBottomSheetMenu());

        btnBrowserReload.setOnClickListener(v -> {
            BrowserTab active = tabManager.getActiveTab();
            if (active != null && active.getWebView() != null) {
                active.getWebView().reload();
            }
        });
    }

    private void setupSearchActions() {
        btnHomeSearchGo.setOnClickListener(v -> processSearchInput(etHomeSearch.getText().toString().trim()));

        etHomeSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                processSearchInput(etHomeSearch.getText().toString().trim());
                return true;
            }
            return false;
        });

        etBrowserUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                processSearchInput(etBrowserUrl.getText().toString().trim());
                return true;
            }
            return false;
        });
    }

    private void processSearchInput(String input) {
        if (input.isEmpty()) return;

        SearchEngine engine = searchPrefs.getCurrentEngine();
        String targetUrl;

        boolean isSearchQuery = input.contains(" ") || (!input.contains(".") && !input.startsWith("http"));
        if (isSearchQuery) {
            targetUrl = engine.searchUrl + Uri.encode(input);
        } else if (input.startsWith("http://") || input.startsWith("https://")) {
            targetUrl = input;
        } else {
            targetUrl = "https://" + input;
        }

        etHomeSearch.setText("");
        openUrl(targetUrl);
    }

    public void openUrl(String url) {
        BrowserTab active = tabManager.getActiveTab();
        if (active == null) {
            active = tabManager.createTab(url, false, false);
        }
        active.setUrl(url);
        active.setHome(false);

        attachTabToView(active);

        etBrowserUrl.setText(url);
        etBrowserUrl.clearFocus();

        WebView wv = active.getWebView();
        if (wv != null) {
            wv.loadUrl(url);
        }
    }

    private void showStartPage() {
        layoutStartPage.setVisibility(View.VISIBLE);
        layoutBrowserView.setVisibility(View.GONE);
        btnNavHome.setColorFilter(Color.parseColor("#6366F1"));
        updateShieldDashboardUI();
        updateNavButtons();
    }

    private void showBrowserView() {
        layoutStartPage.setVisibility(View.GONE);
        layoutBrowserView.setVisibility(View.VISIBLE);
        btnNavHome.setColorFilter(Color.parseColor("#D1D5DB"));
        BrowserTab active = tabManager.getActiveTab();
        if (active != null) {
            updateShieldBadgeUI(active.getBlockedCount());
        }
        updateNavButtons();
    }

    private void updateNavButtons() {
        BrowserTab active = tabManager.getActiveTab();
        boolean isWeb = (layoutBrowserView.getVisibility() == View.VISIBLE && active != null && !active.isHome());
        btnNavBack.setEnabled(isWeb || (layoutBrowserView.getVisibility() == View.VISIBLE));
        btnNavBack.setAlpha(isWeb ? 1.0f : 0.4f);

        boolean canFwd = isWeb && active != null && active.getWebView() != null && active.getWebView().canGoForward();
        btnNavForward.setEnabled(canFwd);
        btnNavForward.setAlpha(canFwd ? 1.0f : 0.4f);
    }

    private void updateSslLock(String url) {
        if (url != null && url.startsWith("https://")) {
            ivBrowserLock.setColorFilter(Color.parseColor("#10B981"));
        } else {
            ivBrowserLock.setColorFilter(Color.parseColor("#888888"));
        }
    }

    private void updateDnsBadgeUI() {
        if (dnsController == null) return;
        DnsProvider provider = dnsController.prefs.getCurrentProvider();
        tvActiveDnsBadge.setText("DNS: " + provider.title);
        switch (provider) {
            case SYSTEM:
                tvActiveDnsBadge.setTextColor(Color.parseColor("#818CF8"));
                break;
            case CLOUDFLARE:
                tvActiveDnsBadge.setTextColor(Color.parseColor("#F97316"));
                break;
            case ADGUARD:
                tvActiveDnsBadge.setTextColor(Color.parseColor("#10B981"));
                break;
            case GOOGLE:
                tvActiveDnsBadge.setTextColor(Color.parseColor("#38BDF8"));
                break;
            default:
                tvActiveDnsBadge.setTextColor(Color.parseColor("#A855F7"));
                break;
        }
    }

    // ==================== BRAVE SHIELDS (PERISAI) ====================
    private void updateShieldDashboardUI() {
        if (tvStatsBlocked != null && vexShield != null) {
            tvStatsBlocked.setText(String.valueOf(vexShield.getTotalBlocked()));
        }
        if (tvStatsData != null && vexShield != null) {
            tvStatsData.setText(vexShield.getFormattedDataSaved());
        }
        if (tvStatsTime != null && vexShield != null) {
            tvStatsTime.setText(vexShield.getFormattedTimeSaved());
        }
    }

    private void updateShieldBadgeUI(int count) {
        if (tvShieldBadgeCount != null) {
            tvShieldBadgeCount.setText(String.valueOf(count));
        }
    }

    private void showShieldBottomSheet() {
        BrowserTab active = tabManager.getActiveTab();
        if (active == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_shield, null);

        TextView tvShieldSiteHost = view.findViewById(R.id.tvShieldSiteHost);
        TextView tvShieldCountBig = view.findViewById(R.id.tvShieldCountBig);
        TextView tvShieldStatusDesc = view.findViewById(R.id.tvShieldStatusDesc);
        SwitchCompat switchShieldMain = view.findViewById(R.id.switchShieldMain);
        View btnShieldReload = view.findViewById(R.id.btnShieldReload);

        String host = "Halaman Beranda";
        String url = active.getUrl();
        if (url != null && !url.isEmpty()) {
            try {
                Uri uri = Uri.parse(url);
                if (uri.getHost() != null) host = uri.getHost();
            } catch (Exception ignored) {}
        }

        final String finalHost = host;
        tvShieldSiteHost.setText(finalHost);
        tvShieldCountBig.setText(String.valueOf(active.getBlockedCount()));

        boolean isShieldEnabled = !vexShield.isWhitelisted(finalHost);
        switchShieldMain.setChecked(isShieldEnabled);
        tvShieldStatusDesc.setText(isShieldEnabled ? "Halaman ini terlindungi oleh Perisai Vex" : "Perisai dinonaktifkan untuk situs ini");

        switchShieldMain.setOnCheckedChangeListener((btn, isChecked) -> {
            vexShield.setShieldEnabled(finalHost, isChecked);
            tvShieldStatusDesc.setText(isChecked ? "Halaman ini terlindungi oleh Perisai Vex" : "Perisai dinonaktifkan untuk situs ini");
        });

        btnShieldReload.setOnClickListener(v -> {
            dialog.dismiss();
            if (active.getWebView() != null) {
                active.getWebView().reload();
            }
        });

        dialog.setContentView(view);
        dialog.show();
    }

    // ==================== RIWAYAT PENJELAJAHAN (HISTORY) ====================
    private void showHistoryBottomSheet() {
        if (historyManager == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_history, null);

        TextView tvHistoryEmpty = view.findViewById(R.id.tvHistoryEmpty);
        TextView btnHistoryClearAll = view.findViewById(R.id.btnHistoryClearAll);
        LinearLayout layoutHistoryContainer = view.findViewById(R.id.layoutHistoryContainer);

        Runnable refreshHistory = new Runnable() {
            @Override
            public void run() {
                layoutHistoryContainer.removeAllViews();
                List<HistoryItem> items = historyManager.getHistoryList();
                if (items.isEmpty()) {
                    tvHistoryEmpty.setVisibility(View.VISIBLE);
                    btnHistoryClearAll.setVisibility(View.GONE);
                } else {
                    tvHistoryEmpty.setVisibility(View.GONE);
                    btnHistoryClearAll.setVisibility(View.VISIBLE);

                    for (HistoryItem item : items) {
                        View card = getLayoutInflater().inflate(R.layout.item_history, layoutHistoryContainer, false);
                        TextView tvTitle = card.findViewById(R.id.tvHistoryTitle);
                        TextView tvUrl = card.findViewById(R.id.tvHistoryUrl);
                        TextView tvTime = card.findViewById(R.id.tvHistoryTime);
                        ImageButton btnDelete = card.findViewById(R.id.btnHistoryDelete);

                        tvTitle.setText(item.getTitle());
                        tvUrl.setText(item.getUrl());
                        tvTime.setText(item.getFormattedTime());

                        card.setOnClickListener(v -> {
                            dialog.dismiss();
                            openUrl(item.getUrl());
                        });

                        btnDelete.setOnClickListener(v -> {
                            historyManager.deleteItem(item.getId());
                            run();
                        });

                        layoutHistoryContainer.addView(card);
                    }
                }
            }
        };

        refreshHistory.run();

        btnHistoryClearAll.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Hapus Semua Riwayat")
                    .setMessage("Apakah Anda yakin ingin menghapus seluruh riwayat penjelajahan?")
                    .setPositiveButton("Hapus", (d, w) -> {
                        historyManager.clearHistory();
                        refreshHistory.run();
                        Toast.makeText(this, "Riwayat penjelajahan dibersihkan", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    // ==================== PENGELOLA TAB & TAB SAMARAN ====================
    private void showTabsDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_tabs, null);

        TextView tvSheetTabTitle = view.findViewById(R.id.tvSheetTabTitle);
        TextView btnSheetCloseAll = view.findViewById(R.id.btnSheetCloseAll);
        View btnSheetNewTab = view.findViewById(R.id.btnSheetNewTab);
        View btnSheetNewIncognito = view.findViewById(R.id.btnSheetNewIncognito);
        LinearLayout layoutTabListContainer = view.findViewById(R.id.layoutTabListContainer);

        Runnable populateTabs = new Runnable() {
            @Override
            public void run() {
                layoutTabListContainer.removeAllViews();
                List<BrowserTab> tabs = tabManager.getTabs();
                int activeIdx = tabManager.getActiveIndex();
                tvSheetTabTitle.setText("Tab Terbuka (" + tabs.size() + ")");

                for (int i = 0; i < tabs.size(); i++) {
                    final int index = i;
                    BrowserTab tab = tabs.get(i);

                    View card = getLayoutInflater().inflate(R.layout.item_tab_card, layoutTabListContainer, false);
                    TextView tvTabIcon = card.findViewById(R.id.tvTabIcon);
                    TextView tvTabTitle = card.findViewById(R.id.tvTabTitle);
                    TextView tvActiveBadge = card.findViewById(R.id.tvActiveBadge);
                    TextView tvTabUrl = card.findViewById(R.id.tvTabUrl);
                    ImageButton btnTabClose = card.findViewById(R.id.btnTabClose);

                    tvTabTitle.setText(tab.getTitle());
                    tvTabIcon.setText(tab.isIncognito() ? "🕶️" : "🌐");
                    if (tab.isHome()) {
                        tvTabUrl.setText(tab.isIncognito() ? "Beranda Samaran" : "Halaman Utama");
                    } else {
                        tvTabUrl.setText(tab.getUrl() != null ? tab.getUrl() : "");
                    }

                    if (index == activeIdx) {
                        tvActiveBadge.setVisibility(View.VISIBLE);
                        card.setBackgroundColor(Color.parseColor("#26293D"));
                    }

                    card.setOnClickListener(v -> {
                        tabManager.selectTab(index);
                        dialog.dismiss();
                    });

                    btnTabClose.setOnClickListener(v -> {
                        tabManager.closeTab(index);
                        if (tabManager.getTabCount() == 0) {
                            dialog.dismiss();
                        } else {
                            run();
                        }
                    });

                    layoutTabListContainer.addView(card);
                }
            }
        };

        populateTabs.run();

        btnSheetNewTab.setOnClickListener(v -> {
            dialog.dismiss();
            tabManager.createTab("", false, true);
        });

        btnSheetNewIncognito.setOnClickListener(v -> {
            dialog.dismiss();
            tabManager.createTab("", true, true);
            Toast.makeText(this, "Tab Samaran Aktif 🕶️ (Privasi Penuh)", Toast.LENGTH_SHORT).show();
        });

        btnSheetCloseAll.setOnClickListener(v -> {
            dialog.dismiss();
            tabManager.closeAllTabs();
            Toast.makeText(this, "Semua tab ditutup", Toast.LENGTH_SHORT).show();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    // ==================== BOTTOM SHEET MENU ====================
    private void showBottomSheetMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_menu, null);

        TextView tvMenuDnsSub = view.findViewById(R.id.tvMenuDnsSub);
        TextView tvMenuSearchSub = view.findViewById(R.id.tvMenuSearchSub);
        SwitchCompat switchDesktop = view.findViewById(R.id.switchDesktop);

        tvMenuDnsSub.setText("Aktif: " + dnsController.prefs.getCurrentProvider().title);
        tvMenuSearchSub.setText(searchPrefs.getCurrentEngine().title);
        switchDesktop.setChecked(isDesktopMode);

        view.findViewById(R.id.menuNewTab).setOnClickListener(v -> {
            dialog.dismiss();
            tabManager.createTab("", false, true);
        });

        View menuKomivex = view.findViewById(R.id.menuKomivex);
        if (menuKomivex != null) {
            menuKomivex.setOnClickListener(v -> {
                dialog.dismiss();
                openUrl(com.example.notebrowser.komivex.KomivexConfig.KOMIVEX_URL);
            });
        }

        view.findViewById(R.id.menuNewIncognitoTab).setOnClickListener(v -> {
            dialog.dismiss();
            tabManager.createTab("", true, true);
            Toast.makeText(this, "Tab Samaran Aktif 🕶️ (Privasi Penuh)", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.menuHistory).setOnClickListener(v -> {
            dialog.dismiss();
            showHistoryBottomSheet();
        });

        view.findViewById(R.id.menuDns).setOnClickListener(v -> {
            dialog.dismiss();
            showDnsSelectionDialog();
        });

        view.findViewById(R.id.menuSearchEngine).setOnClickListener(v -> {
            dialog.dismiss();
            showSearchEngineDialog();
        });

        switchDesktop.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isDesktopMode = isChecked;
            BrowserTab active = tabManager.getActiveTab();
            if (active != null && active.getWebView() != null) {
                WebSettings s = active.getWebView().getSettings();
                if (isDesktopMode) {
                    s.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                } else {
                    s.setUserAgentString(null);
                }
                if (layoutBrowserView.getVisibility() == View.VISIBLE) {
                    active.getWebView().reload();
                }
            }
        });

        view.findViewById(R.id.menuClearCache).setOnClickListener(v -> {
            dialog.dismiss();
            BrowserTab active = tabManager.getActiveTab();
            if (active != null && active.getWebView() != null) {
                active.getWebView().clearCache(true);
            }
            Toast.makeText(this, "Cache memori berhasil dibersihkan", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.menuExitApp).setOnClickListener(v -> {
            dialog.dismiss();
            confirmExitApp();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void showSearchEngineDialog() {
        SearchEngine[] engines = SearchEngine.values();
        String[] titles = new String[engines.length];
        for (int i = 0; i < engines.length; i++) {
            titles[i] = engines[i].title;
        }

        int currentIndex = 0;
        SearchEngine current = searchPrefs.getCurrentEngine();
        for (int i = 0; i < engines.length; i++) {
            if (engines[i] == current) {
                currentIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Pilih Mesin Pencari Default")
                .setSingleChoiceItems(titles, currentIndex, (dialog, which) -> {
                    SearchEngine selected = engines[which];
                    searchPrefs.setCurrentEngine(selected);
                    Toast.makeText(this, "Mesin pencari aktif: " + selected.title, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Tutup", null)
                .show();
    }

    private void showDnsSelectionDialog() {
        DnsProvider[] providers = DnsProvider.values();
        String[] titles = new String[providers.length];
        for (int i = 0; i < providers.length; i++) {
            titles[i] = providers[i].title + "\n" + providers[i].description;
        }

        int currentIndex = 0;
        DnsProvider current = dnsController.prefs.getCurrentProvider();
        for (int i = 0; i < providers.length; i++) {
            if (providers[i] == current) {
                currentIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Pilih Pengaturan DNS / DoH")
                .setSingleChoiceItems(titles, currentIndex, (dialog, which) -> {
                    DnsProvider selected = providers[which];
                    if (selected == DnsProvider.CUSTOM) {
                        dialog.dismiss();
                        showCustomDnsDialog();
                    } else {
                        applySelectedDns(selected);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showCustomDnsDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_custom_dns, null);
        EditText etDoh = view.findViewById(R.id.etCustomDoh);
        EditText etIp = view.findViewById(R.id.etCustomIp);

        etDoh.setText(dnsController.prefs.getCustomDohUrl());
        etIp.setText(dnsController.prefs.getCustomDnsIp());

        new AlertDialog.Builder(this)
                .setTitle("DNS Kustom (DoH / IP)")
                .setView(view)
                .setPositiveButton("Simpan & Terapkan", (dialog, which) -> {
                    String doh = etDoh.getText().toString().trim();
                    String ip = etIp.getText().toString().trim();
                    dnsController.prefs.setCustomDohUrl(doh);
                    dnsController.prefs.setCustomDnsIp(ip);
                    applySelectedDns(DnsProvider.CUSTOM);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void applySelectedDns(DnsProvider provider) {
        dnsController.prefs.setCurrentProvider(provider);
        updateDnsBadgeUI();
        dnsController.applyDnsSettings(() -> runOnUiThread(() -> {
            BrowserTab active = tabManager.getActiveTab();
            if (active != null && active.getWebView() != null && layoutBrowserView.getVisibility() == View.VISIBLE) {
                active.getWebView().reload();
            }
            Toast.makeText(this, "DNS diubah ke: " + provider.title, Toast.LENGTH_SHORT).show();
        }));
    }

    // ==================== TOMBOL KELUAR & NAVIGASI BACK ====================
    private void confirmExitApp() {
        new AlertDialog.Builder(this)
                .setTitle("Keluar dari VexBrowser")
                .setMessage("Tutup semua tab dan keluar dari browser untuk menghemat RAM?")
                .setPositiveButton("Keluar", (d, w) -> exitApp())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void exitApp() {
        if (dnsController != null) dnsController.stopProxy();
        if (tabManager != null) tabManager.closeAllTabs();
        finishAffinity();
        System.exit(0);
    }

    private void handleBackNavigation() {
        if (fullscreenCustomView != null) {
            hideFullscreenVideo();
            return;
        }

        BrowserTab active = tabManager.getActiveTab();
        if (layoutBrowserView.getVisibility() == View.VISIBLE) {
            if (active != null && active.getWebView() != null && active.getWebView().canGoBack()) {
                active.getWebView().goBack();
            } else {
                if (active != null) active.setHome(true);
                showStartPage();
            }
        } else {
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                exitApp();
            } else {
                Toast.makeText(this, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show();
                backPressedTime = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adViewHome != null) {
            try {
                adViewHome.destroy();
            } catch (Exception ignored) {}
        }
        if (dnsController != null) dnsController.stopProxy();
        if (tabManager != null) tabManager.closeAllTabs();
    }
}
