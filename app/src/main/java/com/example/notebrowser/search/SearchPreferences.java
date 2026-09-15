package com.example.notebrowser.search;

import android.content.Context;
import android.content.SharedPreferences;

public class SearchPreferences {
    private static final String PREF_NAME = "notebrowser_search_prefs";
    private static final String KEY_SEARCH_ENGINE = "key_search_engine";

    private final SharedPreferences prefs;

    public SearchPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public SearchEngine getCurrentEngine() {
        String name = prefs.getString(KEY_SEARCH_ENGINE, SearchEngine.GOOGLE.name());
        try {
            return SearchEngine.valueOf(name);
        } catch (Exception e) {
            return SearchEngine.GOOGLE;
        }
    }

    public void setCurrentEngine(SearchEngine engine) {
        prefs.edit().putString(KEY_SEARCH_ENGINE, engine.name()).apply();
    }
}
