package com.example.notebrowser.dns;

import android.content.Context;
import android.content.Intent;
import android.net.ProxyInfo;
import android.os.Build;
import android.util.Log;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class DnsController {
    private static final String TAG = "DnsController";
    public final DnsPreferences prefs;
    private final Context context;
    private LocalDnsProxy proxy;

    public DnsController(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = new DnsPreferences(context);
    }

    public void applyDnsSettings(Runnable onApplied) {
        DnsProvider provider = prefs.getCurrentProvider();
        DnsResolver.clearCache();

        if (provider == DnsProvider.SYSTEM) {
            stopProxy();
            clearProxy(context);
            if (onApplied != null) onApplied.run();
            return;
        }

        if (proxy == null) {
            proxy = new LocalDnsProxy(prefs);
            proxy.start();
        }

        int port = proxy.getListeningPort();
        if (port > 0) {
            setProxy(context, "127.0.0.1", port);
            Log.i(TAG, "Proxy diterapkan: 127.0.0.1:" + port + " untuk " + provider.title);
        }
        if (onApplied != null) onApplied.run();
    }

    public void stopProxy() {
        if (proxy != null) {
            proxy.stop();
            proxy = null;
        }
    }

    private void setProxy(Context ctx, String host, int port) {
        // 1. AndroidX ProxyController (jika didukung WebView modern)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            try {
                ProxyConfig proxyConfig = new ProxyConfig.Builder()
                        .addProxyRule(host + ":" + port)
                        .addBypassRule("localhost")
                        .addBypassRule("127.0.0.1")
                        .build();
                ProxyController.getInstance().setProxyOverride(proxyConfig, Runnable::run, () -> {
                    Log.d(TAG, "AndroidX ProxyOverride berhasil dipasang");
                });
            } catch (Throwable t) {
                Log.w(TAG, "AndroidX setProxyOverride: " + t.getMessage());
            }
        }

        // 2. System Properties (untuk koneksi HTTP bawaan)
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", String.valueOf(port));
        System.setProperty("https.proxyHost", host);
        System.setProperty("https.proxyPort", String.valueOf(port));

        // 3. Framework Broadcast & Reflection Fallback untuk Chromium WebView Android 5.0 - 9.0
        applyReflectionProxy(ctx, host, port);
    }

    private void clearProxy(Context ctx) {
        // 1. AndroidX ProxyController
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            try {
                ProxyController.getInstance().clearProxyOverride(Runnable::run, () -> {
                    Log.d(TAG, "AndroidX ProxyOverride dibersihkan");
                });
            } catch (Throwable ignored) {}
        }

        // 2. System Properties
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");

        // 3. Framework Reflection
        applyReflectionProxy(ctx, null, 0);
    }

    private void applyReflectionProxy(Context ctx, String host, int port) {
        try {
            Intent intent = new Intent(android.net.Proxy.PROXY_CHANGE_ACTION);
            if (host != null && port > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        ProxyInfo proxyInfo = ProxyInfo.buildDirectProxy(host, port);
                        intent.putExtra("android.intent.extra.PROXY_INFO", proxyInfo);
                    } catch (Throwable ignored) {}
                }
            }

            // Trigger internal Chromium ProxyChangeListener directly without global broadcast
            Class<?> activityThreadCls = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadCls.getMethod("currentActivityThread");
            Object currentActivityThread = currentActivityThreadMethod.invoke(null);
            if (currentActivityThread == null) return;

            Field mBoundApplicationField = activityThreadCls.getDeclaredField("mBoundApplication");
            mBoundApplicationField.setAccessible(true);
            Object mBoundApplication = mBoundApplicationField.get(currentActivityThread);
            if (mBoundApplication == null) return;

            Field infoField = mBoundApplication.getClass().getDeclaredField("info");
            infoField.setAccessible(true);
            Object loadedApk = infoField.get(mBoundApplication);
            if (loadedApk == null) return;

            Field mReceiversField = loadedApk.getClass().getDeclaredField("mReceivers");
            mReceiversField.setAccessible(true);
            Map<?, ?> receivers = (Map<?, ?>) mReceiversField.get(loadedApk);

            if (receivers != null) {
                for (Object mapObj : receivers.values()) {
                    if (mapObj instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) mapObj;
                        for (Object receiver : map.keySet()) {
                            if (receiver.getClass().getName().contains("ProxyChangeListener")) {
                                Method onReceiveMethod = receiver.getClass().getDeclaredMethod("onReceive", Context.class, Intent.class);
                                onReceiveMethod.invoke(receiver, ctx, intent);
                                Log.d(TAG, "ProxyChangeListener receiver dipicu langsung");
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "Reflection proxy notification: " + t.getMessage());
        }
    }
}
