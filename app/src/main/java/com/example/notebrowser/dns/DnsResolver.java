package com.example.notebrowser.dns;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DnsResolver {
    private static final String TAG = "DnsResolver";
    private static final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static SSLSocketFactory lenientSslSocketFactory;

    private static class CacheEntry {
        final String ip;
        final long expiryTime;
        CacheEntry(String ip, long expiryTime) {
            this.ip = ip;
            this.expiryTime = expiryTime;
        }
    }

    public static synchronized SSLSocketFactory getLenientSslSocketFactory() {
        if (lenientSslSocketFactory != null) return lenientSslSocketFactory;
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            lenientSslSocketFactory = sc.getSocketFactory();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SSL socket factory", e);
        }
        return lenientSslSocketFactory;
    }

    public static String resolve(String host, DnsProvider provider, String customDoh, String customIp) {
        if (host == null || host.isEmpty()) return null;
        if (isIpAddress(host)) return host;

        long now = System.currentTimeMillis();
        CacheEntry cached = cache.get(host);
        if (cached != null) {
            if (cached.expiryTime > now) {
                return cached.ip;
            } else {
                cache.remove(host);
            }
        }

        if (provider == DnsProvider.SYSTEM) {
            try {
                String ip = InetAddress.getByName(host).getHostAddress();
                if (ip != null) cache.put(host, new CacheEntry(ip, now + 600000));
                return ip;
            } catch (Exception e) {
                return null;
            }
        }

        // 1. Coba DoH (DNS-over-HTTPS via IP langsung dengan fallback)
        String[] dohEndpoints;
        String hostHeader = "";

        switch (provider) {
            case CLOUDFLARE:
                dohEndpoints = new String[]{"https://1.1.1.1/dns-query", "https://1.0.0.1/dns-query"};
                hostHeader = "cloudflare-dns.com";
                break;
            case GOOGLE:
                dohEndpoints = new String[]{"https://8.8.8.8/resolve", "https://8.8.4.4/resolve"};
                hostHeader = "dns.google";
                break;
            case ADGUARD:
                dohEndpoints = new String[]{"https://94.140.14.14/resolve", "https://94.140.15.15/resolve"};
                hostHeader = "dns.adguard-dns.com";
                break;
            case QUAD9:
                dohEndpoints = new String[]{"https://9.9.9.9:5053/dns-query", "https://149.112.112.112:5053/dns-query"};
                hostHeader = "dns.quad9.net";
                break;
            case CUSTOM:
                String endpoint = (customDoh != null && !customDoh.trim().isEmpty()) ? customDoh.trim() : "https://1.1.1.1/dns-query";
                dohEndpoints = new String[]{endpoint};
                break;
            default:
                dohEndpoints = new String[0];
                break;
        }

        for (String endpoint : dohEndpoints) {
            String dohResult = resolveViaDoH(host, endpoint, hostHeader);
            if (dohResult != null && !dohResult.isEmpty()) {
                Log.d(TAG, "DoH resolved " + host + " -> " + dohResult + " via " + endpoint);
                cache.put(host, new CacheEntry(dohResult, now + 600000));
                return dohResult;
            }
        }

        // 1.5 Emergency DoH Fallback (Google & Cloudflare) jika provider pilihan terblokir
        if (provider != DnsProvider.GOOGLE) {
            String emergencyResult = resolveViaDoH(host, "https://8.8.8.8/resolve", "dns.google");
            if (emergencyResult != null && !emergencyResult.isEmpty()) {
                Log.d(TAG, "Emergency Google DoH resolved " + host + " -> " + emergencyResult);
                cache.put(host, new CacheEntry(emergencyResult, now + 600000));
                return emergencyResult;
            }
        }
        if (provider != DnsProvider.CLOUDFLARE) {
            String emergencyResult = resolveViaDoH(host, "https://1.1.1.1/dns-query", "cloudflare-dns.com");
            if (emergencyResult != null && !emergencyResult.isEmpty()) {
                Log.d(TAG, "Emergency Cloudflare DoH resolved " + host + " -> " + emergencyResult);
                cache.put(host, new CacheEntry(emergencyResult, now + 600000));
                return emergencyResult;
            }
        }

        // 2. Fallback: UDP DNS Port 53
        String[] fallbackIps;
        switch (provider) {
            case CLOUDFLARE: fallbackIps = new String[]{"1.1.1.1", "1.0.0.1"}; break;
            case GOOGLE: fallbackIps = new String[]{"8.8.8.8", "8.8.4.4"}; break;
            case ADGUARD: fallbackIps = new String[]{"94.140.14.14", "94.140.15.15"}; break;
            case QUAD9: fallbackIps = new String[]{"9.9.9.9", "149.112.112.112"}; break;
            case CUSTOM:
                String ip = (customIp != null && !customIp.trim().isEmpty()) ? customIp.trim() : "1.1.1.1";
                fallbackIps = new String[]{ip};
                break;
            default:
                fallbackIps = new String[0];
                break;
        }

        for (String ip : fallbackIps) {
            String udpResult = resolveViaUdp(host, ip);
            if (udpResult != null && !udpResult.isEmpty()) {
                cache.put(host, new CacheEntry(udpResult, now + 600000));
                return udpResult;
            }
        }

        // 3. Fallback sistem bawaan jika DoH & UDP gagal
        try {
            String ip = InetAddress.getByName(host).getHostAddress();
            if (ip != null) cache.put(host, new CacheEntry(ip, now + 120000));
            return ip;
        } catch (Exception e) {
            Log.w(TAG, "Semua resolver gagal untuk host: " + host);
            return null;
        }
    }

    private static String resolveViaDoH(String host, String endpoint, String hostHeader) {
        try {
            String separator = endpoint.contains("?") ? "&" : "?";
            String urlString = endpoint + separator + "name=" + host + "&type=A";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/dns-json");
            conn.setRequestProperty("User-Agent", "VexBrowser/1.0");
            if (hostHeader != null && !hostHeader.isEmpty()) {
                conn.setRequestProperty("Host", hostHeader);
            }

            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                SSLSocketFactory ssf = getLenientSslSocketFactory();
                if (ssf != null) httpsConn.setSSLSocketFactory(ssf);
                httpsConn.setHostnameVerifier((hostname, session) -> true);
            }

            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                if (json.has("Answer")) {
                    JSONArray answers = json.getJSONArray("Answer");
                    for (int i = 0; i < answers.length(); i++) {
                        JSONObject ans = answers.getJSONObject(i);
                        if (ans.optInt("type") == 1 && ans.has("data")) {
                            String ip = ans.getString("data");
                            if (isIpAddress(ip)) return ip;
                        }
                    }
                }
            } else {
                Log.w(TAG, "DoH query returned HTTP " + code + " for " + endpoint);
            }
        } catch (Exception e) {
            Log.d(TAG, "DoH failed on " + endpoint + ": " + e.getMessage());
        }
        return null;
    }

    private static String resolveViaUdp(String host, String dnsServerIp) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(2500);

            byte[] query = buildDnsQuery(host);
            InetAddress dnsAddr = InetAddress.getByName(dnsServerIp);
            DatagramPacket packet = new DatagramPacket(query, query.length, dnsAddr, 53);
            socket.send(packet);

            byte[] buffer = new byte[512];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(receivePacket);
            socket.close();

            return parseDnsResponse(buffer, receivePacket.getLength());
        } catch (Exception ignored) {}
        return null;
    }

    private static byte[] buildDnsQuery(String host) {
        ArrayList<Byte> bytes = new ArrayList<>();
        bytes.add((byte) 0x12); bytes.add((byte) 0x34);
        bytes.add((byte) 0x01); bytes.add((byte) 0x00);
        bytes.add((byte) 0x00); bytes.add((byte) 0x01);
        bytes.add((byte) 0x00); bytes.add((byte) 0x00);
        bytes.add((byte) 0x00); bytes.add((byte) 0x00);
        bytes.add((byte) 0x00); bytes.add((byte) 0x00);

        String[] parts = host.split("\\.");
        for (String part : parts) {
            bytes.add((byte) part.length());
            for (char c : part.toCharArray()) {
                bytes.add((byte) c);
            }
        }
        bytes.add((byte) 0x00);
        bytes.add((byte) 0x00); bytes.add((byte) 0x01);
        bytes.add((byte) 0x00); bytes.add((byte) 0x01);

        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            result[i] = bytes.get(i);
        }
        return result;
    }

    private static String parseDnsResponse(byte[] buffer, int length) {
        if (length < 12) return null;
        int anCount = ((buffer[6] & 0xFF) << 8) | (buffer[7] & 0xFF);
        if (anCount == 0) return null;

        int pos = 12;
        while (pos < length && buffer[pos] != 0) {
            int len = buffer[pos] & 0xFF;
            pos += 1 + len;
        }
        pos += 5;

        for (int i = 0; i < anCount; i++) {
            if (pos >= length) break;
            if ((buffer[pos] & 0xC0) == 0xC0) {
                pos += 2;
            } else {
                while (pos < length && buffer[pos] != 0) pos++;
                pos++;
            }
            if (pos + 10 > length) break;
            int type = ((buffer[pos] & 0xFF) << 8) | (buffer[pos + 1] & 0xFF);
            pos += 8;
            int dataLen = ((buffer[pos] & 0xFF) << 8) | (buffer[pos + 1] & 0xFF);
            pos += 2;

            if (type == 1 && dataLen == 4 && pos + 4 <= length) {
                return (buffer[pos] & 0xFF) + "." + (buffer[pos + 1] & 0xFF) + "." + (buffer[pos + 2] & 0xFF) + "." + (buffer[pos + 3] & 0xFF);
            }
            pos += dataLen;
        }
        return null;
    }

    public static boolean isIpAddress(String text) {
        return text != null && text.matches("^(\\d{1,3}\\.){3}\\d{1,3}$");
    }

    public static void clearCache() {
        cache.clear();
    }
}
