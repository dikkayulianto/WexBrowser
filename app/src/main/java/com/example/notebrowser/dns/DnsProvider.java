package com.example.notebrowser.dns;

public enum DnsProvider {
    SYSTEM("Default Sistem (ISP)", "Menggunakan DNS jaringan bawaan", "", "", ""),
    CLOUDFLARE("Cloudflare DNS (1.1.1.1)", "Sangat cepat, privasi tinggi, buka blokir situs", "1.1.1.1", "1.0.0.1", "https://cloudflare-dns.com/dns-query"),
    GOOGLE("Google Public DNS", "Keandalan tinggi dan latency global rendah", "8.8.8.8", "8.8.4.4", "https://dns.google/resolve"),
    ADGUARD("AdGuard DNS (Blokir Iklan)", "Memblokir iklan, pop-up, dan tracker otomatis", "94.140.14.14", "94.140.15.15", "https://dns.adguard-dns.com/resolve"),
    QUAD9("Quad9 DNS (Keamanan)", "Proteksi dari malware dan domain berbahaya", "9.9.9.9", "149.112.112.112", "https://dns.quad9.net/dns-query"),
    CUSTOM("DNS Kustom", "Tentukan server DoH atau alamat IP sendiri", "", "", "");

    public final String title;
    public final String description;
    public final String primaryIp;
    public final String secondaryIp;
    public final String dohUrl;

    DnsProvider(String title, String description, String primaryIp, String secondaryIp, String dohUrl) {
        this.title = title;
        this.description = description;
        this.primaryIp = primaryIp;
        this.secondaryIp = secondaryIp;
        this.dohUrl = dohUrl;
    }
}
