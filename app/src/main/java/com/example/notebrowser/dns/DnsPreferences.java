package com.example.notebrowser.dns;

import android.content.Context;
import android.content.SharedPreferences;

public class DnsPreferences {
    private static final String PREF_NAME = "notebrowser_dns_prefs";
    private static final String KEY_DNS_PROVIDER = "key_dns_provider";
    private static final String KEY_CUSTOM_DOH = "key_custom_doh";
    private static final String KEY_CUSTOM_IP = "key_custom_ip";

    private final SharedPreferences prefs;

    public DnsPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public DnsProvider getCurrentProvider() {
        String name = prefs.getString(KEY_DNS_PROVIDER, DnsProvider.SYSTEM.name());
        try {
            return DnsProvider.valueOf(name);
        } catch (Exception e) {
            return DnsProvider.SYSTEM;
        }
    }

    public void setCurrentProvider(DnsProvider provider) {
        prefs.edit().putString(KEY_DNS_PROVIDER, provider.name()).apply();
    }

    public String getCustomDohUrl() {
        return prefs.getString(KEY_CUSTOM_DOH, "https://cloudflare-dns.com/dns-query");
    }

    public void setCustomDohUrl(String url) {
        prefs.edit().putString(KEY_CUSTOM_DOH, url).apply();
    }

    public String getCustomDnsIp() {
        return prefs.getString(KEY_CUSTOM_IP, "1.1.1.1");
    }

    public void setCustomDnsIp(String ip) {
        prefs.edit().putString(KEY_CUSTOM_IP, ip).apply();
    }
}
