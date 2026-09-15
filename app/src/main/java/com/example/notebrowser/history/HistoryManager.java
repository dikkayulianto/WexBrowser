package com.example.notebrowser.history;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final String PREF_NAME = "vex_browser_history";
    private static final String KEY_HISTORY_DATA = "history_json";
    private static final int MAX_HISTORY = 100;

    private static HistoryManager instance;
    private final SharedPreferences prefs;

    private HistoryManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized HistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new HistoryManager(context);
        }
        return instance;
    }

    public synchronized void addHistory(String title, String url) {
        if (url == null || url.isEmpty() || url.startsWith("about:") || url.startsWith("data:")) return;

        List<HistoryItem> list = getHistoryList();

        // Hapus duplikat URL terakhir jika ada
        for (int i = 0; i < list.size(); i++) {
            if (url.equals(list.get(i).getUrl())) {
                list.remove(i);
                break;
            }
        }

        // Tambahkan di urutan teratas
        list.add(0, new HistoryItem(title, url, System.currentTimeMillis()));

        // Batasi maksimal 100 entri
        while (list.size() > MAX_HISTORY) {
            list.remove(list.size() - 1);
        }

        saveList(list);
    }

    public synchronized List<HistoryItem> getHistoryList() {
        List<HistoryItem> result = new ArrayList<>();
        String jsonStr = prefs.getString(KEY_HISTORY_DATA, "[]");
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                result.add(new HistoryItem(
                        obj.optString("id"),
                        obj.optString("title"),
                        obj.optString("url"),
                        obj.optLong("time")
                ));
            }
        } catch (Exception ignored) {}
        return result;
    }

    public synchronized void deleteItem(String id) {
        if (id == null) return;
        List<HistoryItem> list = getHistoryList();
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).getId())) {
                list.remove(i);
                break;
            }
        }
        saveList(list);
    }

    public synchronized void clearHistory() {
        prefs.edit().remove(KEY_HISTORY_DATA).apply();
    }

    private void saveList(List<HistoryItem> list) {
        try {
            JSONArray arr = new JSONArray();
            for (HistoryItem item : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("title", item.getTitle());
                obj.put("url", item.getUrl());
                obj.put("time", item.getTimestamp());
                arr.put(obj);
            }
            prefs.edit().putString(KEY_HISTORY_DATA, arr.toString()).apply();
        } catch (Exception ignored) {}
    }
}
