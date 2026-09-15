package com.example.notebrowser.history;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class HistoryItem {
    private final String id;
    private final String title;
    private final String url;
    private final long timestamp;

    public HistoryItem(String title, String url, long timestamp) {
        this(UUID.randomUUID().toString(), title, url, timestamp);
    }

    public HistoryItem(String id, String title, String url, long timestamp) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.title = (title != null && !title.isEmpty()) ? title : url;
        this.url = url;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd MMM", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }
}
