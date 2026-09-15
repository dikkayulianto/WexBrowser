package com.example.notebrowser.dns;

import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalDnsProxy {
    private static final String TAG = "LocalDnsProxy";
    private final DnsPreferences prefs;
    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private int listeningPort = 0;

    public LocalDnsProxy(DnsPreferences prefs) {
        this.prefs = prefs;
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public synchronized void start() {
        if (isRunning) return;
        try {
            serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            listeningPort = serverSocket.getLocalPort();
            isRunning = true;

            threadPool.execute(() -> {
                while (isRunning && serverSocket != null && !serverSocket.isClosed()) {
                    try {
                        Socket client = serverSocket.accept();
                        threadPool.execute(() -> handleClient(client));
                    } catch (Exception e) {
                        if (!isRunning) break;
                    }
                }
            });
            Log.i(TAG, "Local DNS Proxy berjalan di port: " + listeningPort);
        } catch (Exception e) {
            Log.e(TAG, "Gagal memulai Local DNS Proxy", e);
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            clientSocket.setSoTimeout(15000);
            InputStream clientIn = clientSocket.getInputStream();
            OutputStream clientOut = clientSocket.getOutputStream();

            String firstLine = readLine(clientIn);
            if (firstLine == null || firstLine.trim().isEmpty()) {
                clientSocket.close();
                return;
            }

            String[] parts = firstLine.split(" ");
            if (parts.length < 2) {
                clientSocket.close();
                return;
            }

            String method = parts[0].toUpperCase();
            String uri = parts[1];

            if ("CONNECT".equals(method)) {
                String[] hostPort = uri.split(":");
                String host = hostPort[0];
                int port = hostPort.length > 1 ? parseInt(hostPort[1], 443) : 443;

                while (true) {
                    String line = readLine(clientIn);
                    if (line == null || line.isEmpty()) break;
                }

                String resolvedIp = DnsResolver.resolve(host, prefs.getCurrentProvider(), prefs.getCustomDohUrl(), prefs.getCustomDnsIp());
                if (resolvedIp == null) resolvedIp = host;

                Socket targetSocket;
                try {
                    targetSocket = new Socket(resolvedIp, port);
                    targetSocket.setSoTimeout(30000);
                } catch (Exception connErr) {
                    Log.e(TAG, "Gagal terhubung ke host: " + host + " IP: " + resolvedIp + ":" + port);
                    clientOut.write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n".getBytes());
                    clientOut.flush();
                    clientSocket.close();
                    return;
                }

                clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
                clientOut.flush();

                pipeSockets(clientSocket, targetSocket);
            } else {
                String host = "";
                int port = 80;
                if (uri.startsWith("http://")) {
                    String clean = uri.substring(7);
                    int slashIdx = clean.indexOf('/');
                    String hostPart = slashIdx != -1 ? clean.substring(0, slashIdx) : clean;
                    int colonIdx = hostPart.indexOf(':');
                    if (colonIdx != -1) {
                        host = hostPart.substring(0, colonIdx);
                        port = parseInt(hostPart.substring(colonIdx + 1), 80);
                    } else {
                        host = hostPart;
                    }
                }

                String resolvedIp = DnsResolver.resolve(host, prefs.getCurrentProvider(), prefs.getCustomDohUrl(), prefs.getCustomDnsIp());
                if (resolvedIp == null) resolvedIp = host;

                Socket targetSocket;
                try {
                    targetSocket = new Socket(resolvedIp, port);
                    targetSocket.setSoTimeout(30000);
                } catch (Exception connErr) {
                    clientOut.write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n".getBytes());
                    clientOut.flush();
                    clientSocket.close();
                    return;
                }

                OutputStream targetOut = targetSocket.getOutputStream();
                targetOut.write((firstLine + "\r\n").getBytes());

                pipeSockets(clientSocket, targetSocket);
            }
        } catch (Exception e) {
            try { clientSocket.close(); } catch (Exception ignored) {}
        }
    }

    private void pipeSockets(Socket a, Socket b) {
        threadPool.execute(() -> {
            try {
                copyStream(a.getInputStream(), b.getOutputStream());
            } catch (Exception ignored) {}
            try {
                if (!b.isOutputShutdown()) b.shutdownOutput();
            } catch (Exception ignored) {}
            try {
                if (!a.isInputShutdown()) a.shutdownInput();
            } catch (Exception ignored) {}
        });
        threadPool.execute(() -> {
            try {
                copyStream(b.getInputStream(), a.getOutputStream());
            } catch (Exception ignored) {}
            try {
                if (!a.isOutputShutdown()) a.shutdownOutput();
            } catch (Exception ignored) {}
            try {
                if (!b.isInputShutdown()) b.shutdownInput();
            } catch (Exception ignored) {}
        });
    }

    private void copyStream(InputStream in, OutputStream out) {
        byte[] buf = new byte[8192];
        try {
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
                out.flush();
            }
        } catch (Exception ignored) {}
    }

    private String readLine(InputStream in) {
        StringBuilder sb = new StringBuilder();
        try {
            int c;
            while ((c = in.read()) != -1) {
                if (c == '\n') break;
                if (c != '\r') sb.append((char) c);
            }
            return (sb.length() == 0 && c == -1) ? null : sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private int parseInt(String str, int defaultVal) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    public synchronized void stop() {
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}
        serverSocket = null;
    }
}
