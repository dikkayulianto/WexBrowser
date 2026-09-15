package com.example.notebrowser.search;

public enum SearchEngine {
    GOOGLE("Google", "https://www.google.com/search?q=", "https://www.google.com"),
    DUCKDUCKGO("DuckDuckGo (Privasi)", "https://duckduckgo.com/?q=", "https://duckduckgo.com"),
    BING("Bing (Microsoft)", "https://www.bing.com/search?q=", "https://www.bing.com"),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p=", "https://search.yahoo.com");

    public final String title;
    public final String searchUrl;
    public final String homeUrl;

    SearchEngine(String title, String searchUrl, String homeUrl) {
        this.title = title;
        this.searchUrl = searchUrl;
        this.homeUrl = homeUrl;
    }
}
