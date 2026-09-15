package com.example.notebrowser.shield;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class VexShield {
    private static final String TAG = "VexShield";
    private static final String PREF_NAME = "vex_shield_prefs";
    private static final String KEY_TOTAL_BLOCKED = "total_blocked";
    private static final String KEY_WHITELIST = "shield_whitelist";

    private static VexShield instance;
    private final SharedPreferences prefs;

    // Daftar komprehensif domain jaringan iklan, tracker, dan pop-up video player (Global & Indonesia)
    private static final Set<String> AD_DOMAINS = new HashSet<>(Arrays.asList(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "google-analytics.com",
            "adservice.google.com",
            "pagead2.googlesyndication.com",
            "tpc.googlesyndication.com",
            "taboola.com",
            "outbrain.com",
            "criteo.com",
            "criteo.net",
            "mgid.com",
            "adnow.com",
            "popads.net",
            "propellerads.com",
            "adnxs.com",
            "rubiconproject.com",
            "pubmatic.com",
            "openx.net",
            "casalemedia.com",
            "scorecardresearch.com",
            "quantserve.com",
            "zedo.com",
            "bidswitch.net",
            "smartadserver.com",
            "inmobi.com",
            "chartbeat.com",
            "hotjar.com",
            "segment.io",
            "mixpanel.com",
            "adroll.com",
            "amazon-adsystem.com",
            "serving-sys.com",
            "moatads.com",
            "ads-twitter.com",
            "adnxs-simple.com",
            "advertising.com",
            "admob.com",
            "adtech.de",
            "revcontent.com",
            "media.net",
            "exponential.com",
            "tribalfusion.com",
            "trafficjunky.com",
            "exoclick.com",
            "juicyads.com",
            "adnium.com",
            "innity.com",
            "innity.net",
            "clicksor.com",
            "yllix.com",
            "adsterra.com",
            "zeroredirect1.com",
            "vdo.ai",
            "aniview.com",
            "teads.tv",
            "teads.com",
            "yadro.ru",
            "popmyads.com",
            "adman.gr",
            "adform.net",
            "monetag.com",
            "hilltopads.net",
            "clickadu.com",
            "adtrue.com",
            "propellerclick.com",
            "onclickalgo.com",
            "onclicksuper.com",
            "onclickperformance.com",
            "syndication.realsrv.com",
            "exdynsrv.com",
            "realsrv.com",
            "servebom.com",
            "bidgear.com",
            "admaven.com",
            "galaksion.com",
            "richpush.co",
            "evadav.com",
            "rollerads.com",
            "kadam.net",
            "popcash.net",
            "directrev.com",
            "propu.sh",
            "propush.net",
            "deloplen.com",
            "gloaphoo.net",
            "whomeeno.net",
            "al5sm.com",
            "asoulthrow.com",
            "ayewhoo.com",
            "surreals.top",
            "ouo.io",
            "ouo.press",
            "sfl.gl",
            "shorturl.at",
            "adf.ly",
            "shope.ee",
            "tokopedia.link",
            "lazada.co.id"
    ));

    private VexShield(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized VexShield getInstance(Context context) {
        if (instance == null) {
            instance = new VexShield(context);
        }
        return instance;
    }

    public boolean shouldBlock(String requestUrl, String pageUrl) {
        if (requestUrl == null || requestUrl.isEmpty()) return false;

        String pageHost = extractHost(pageUrl);
        if (pageHost != null && isWhitelisted(pageHost)) {
            return false;
        }

        String requestHost = extractHost(requestUrl);
        if (requestHost != null) {
            // Whitelist mesin pencari & portal utama agar tidak pernah terganggu
            if (requestHost.contains("duckduckgo.com") || requestHost.contains("google.com") ||
                requestHost.contains("google.co.") || requestHost.contains("bing.com") ||
                requestHost.contains("wikipedia.org") || requestHost.contains("yahoo.com") ||
                requestHost.contains("ecosia.org") || requestHost.contains("startpage.com") ||
                requestHost.contains("linktr.ee") || requestHost.contains("linktree.com")) {
                return false;
            }

            // Jangan pernah blokir CDN atau server pemutar video streaming & safelink ketik.live
            if (requestHost.contains("iamcdn.net") || requestHost.contains("abyssplayer") ||
                requestHost.contains("streamwish") || requestHost.contains("filelions") ||
                requestHost.contains("dood") || requestHost.contains("vidhide") ||
                requestHost.contains("streamtape") || requestHost.contains("ketik.live")) {
                return false;
            }

            // 1. Cek awalan subdomain iklan
            if (requestHost.startsWith("ad.") || requestHost.startsWith("ads.") ||
                requestHost.startsWith("adserver.") || requestHost.startsWith("tracker.") ||
                requestHost.startsWith("analytics.")) {
                return true;
            }

            // 2. Cek domain blacklist
            for (String adDomain : AD_DOMAINS) {
                if (requestHost.equals(adDomain) || requestHost.endsWith("." + adDomain)) {
                    return true;
                }
            }

            // 3. Cek kata kunci jaringan iklan di host
            if (requestHost.contains("doubleclick") || requestHost.contains("googlesyndication") ||
                requestHost.contains("googleadservices") || requestHost.contains("adsystem") ||
                requestHost.contains("adnxs") || requestHost.contains("criteo") ||
                requestHost.contains("taboola") || requestHost.contains("outbrain") ||
                requestHost.contains("mgid.") || requestHost.contains("adnow.") ||
                requestHost.contains("innity") || requestHost.contains("popads") ||
                requestHost.contains("popcash") || requestHost.contains("adsterra") ||
                requestHost.contains("propellerads") || requestHost.contains("monetag") ||
                requestHost.contains("onclick") || requestHost.contains("clickadu") ||
                requestHost.contains("hilltopads") || requestHost.contains("admaven") ||
                requestHost.contains("galaksion") || requestHost.contains("propu.sh") ||
                requestHost.contains("deloplen") || requestHost.contains("whomeeno") ||
                requestHost.contains("gloaphoo") || requestHost.contains("realsrv")) {
                return true;
            }
        }

        // 4. Cek kata kunci URL iklan & banner judi streaming (DutaMovie21, LK21, dll)
        String lowerUrl = requestUrl.toLowerCase(Locale.ROOT);
        if (lowerUrl.contains("/pagead/") || lowerUrl.contains("/adservice") ||
            lowerUrl.contains("/adserver") || lowerUrl.contains("googleads.js") ||
            lowerUrl.contains("googleads") || lowerUrl.contains("show_ads.js") ||
            lowerUrl.contains("prebid.js") || lowerUrl.contains("/popunder") ||
            lowerUrl.contains("/popup") || lowerUrl.contains("fbevents.js") ||
            lowerUrl.contains("/banner_ad") || lowerUrl.contains("/banners/") ||
            lowerUrl.contains("/iklan/") || lowerUrl.contains("zeus") ||
            lowerUrl.contains("slot") || lowerUrl.contains("gacor") ||
            lowerUrl.contains("indokasino") || lowerUrl.contains("ibosport") ||
            lowerUrl.contains("pusatmovie") || lowerUrl.contains("judi") ||
            lowerUrl.contains("kasino") || lowerUrl.contains("casino") ||
            lowerUrl.contains("taruhan") || lowerUrl.contains("popcash") ||
            lowerUrl.contains("ad_url=") || lowerUrl.contains("clickurl=") ||
            lowerUrl.contains("redirect_url=")) {
            return true;
        }

        return false;
    }

    public boolean isPopupOrAdUrl(String url) {
        return isPopupOrAdUrl(url, null);
    }

    public boolean isPopupOrAdUrl(String url, String currentUrl) {
        if (url == null || url.trim().isEmpty()) return false;
        String cleanUrl = url.trim();
        String lower = cleanUrl.toLowerCase(Locale.ROOT);

        // Jangan pernah blokir safelink ketik.live, mesin pencari, Linktree, tautan telegram, atau CDN player penting
        if (lower.contains("duckduckgo.com") || lower.contains("google.com") ||
            lower.contains("bing.com") || lower.contains("wikipedia.org") ||
            lower.contains("linktr.ee") || lower.contains("linktree.com") ||
            lower.contains("ketik.live") || lower.contains("iamcdn.net") ||
            lower.contains("abyssplayer") || lower.contains("t.me") ||
            lower.contains("telegram.org")) {
            return false;
        }

        // 1. Skema liar pengalih / about:blank / data URI
        if (lower.equals("about:blank") || lower.startsWith("data:text/html")) {
            return true;
        }

        // 2. Intent yang mencoba membajak ke Chrome atau browser luar
        if (lower.startsWith("intent://")) {
            if (lower.contains("package=com.android.chrome") || lower.contains("package=com.chrome") ||
                lower.contains("package=com.sec.android.app.sbrowser") || lower.contains("browser")) {
                return true;
            }
        }

        // 3. Deteksi standar melalui shouldBlock
        if (shouldBlock(cleanUrl, currentUrl)) {
            return true;
        }

        // 4. Cek domain host
        String host = extractHost(cleanUrl);
        if (host != null) {
            for (String adDomain : AD_DOMAINS) {
                if (host.equals(adDomain) || host.endsWith("." + adDomain)) {
                    return true;
                }
            }
        }

        // 5. Cek kata kunci URL pop-up & jaringan afiliasi video player
        return lower.contains("zeus") || lower.contains("slot") ||
               lower.contains("gacor") || lower.contains("judi") ||
               lower.contains("bet88") || lower.contains("slot88") ||
               lower.contains("casino") || lower.contains("kasino") ||
               lower.contains("depo") || lower.contains("togel") ||
               lower.contains("poker") || lower.contains("maxwin") ||
               lower.contains("pragmatic") || lower.contains("sbobet") ||
               lower.contains("popunder") || lower.contains("popads") ||
               lower.contains("adsterra") || lower.contains("monetag") ||
               lower.contains("onclick") || lower.contains("ouo.io") ||
               lower.contains("ouo.press") || lower.contains("sfl.gl") ||
               lower.contains("shorturl.at") || lower.contains("adf.ly") ||
               lower.contains("adtrue") || lower.contains("ibosport") ||
               lower.contains("klikzeus") || lower.contains("indokasino") ||
               lower.contains("pusatmovie") || lower.contains("propu.sh") || lower.contains("propush") ||
               lower.contains("deloplen") || lower.contains("gloaphoo") ||
               lower.contains("whomeeno") || lower.contains("al5sm") ||
               lower.contains("asoulthrow") || lower.contains("ayewhoo") ||
               lower.contains("surreals") || lower.contains("exdynsrv") ||
               lower.contains("realsrv") || lower.contains("admaven") ||
               lower.contains("galaksion") || lower.contains("richpush") ||
               lower.contains("evadav") || lower.contains("rollerads") ||
               lower.contains("popcash") || lower.contains("servebom") ||
               lower.contains("bidgear") || lower.contains("hilltopads");
    }

    public boolean isSuspiciousThirdPartyJump(String currentHost, String targetHost) {
        if (currentHost == null || targetHost == null) return false;
        String cur = currentHost.toLowerCase(Locale.ROOT);
        String tgt = targetHost.toLowerCase(Locale.ROOT);

        // Jika domain sama atau subdomain
        if (tgt.equals(cur) || tgt.endsWith("." + cur) || cur.endsWith("." + tgt)) {
            return false;
        }

        // Cek apakah targetHost cocok dengan domain iklan
        for (String adDomain : AD_DOMAINS) {
            if (tgt.equals(adDomain) || tgt.endsWith("." + adDomain)) {
                return true;
            }
        }

        // Cek kata kunci iklan/judi pada targetHost
        return tgt.contains("slot") || tgt.contains("zeus") || tgt.contains("gacor") ||
               tgt.contains("judi") || tgt.contains("bet") || tgt.contains("casino") ||
               tgt.contains("onclick") || tgt.contains("pop") || tgt.contains("monetag") ||
               tgt.contains("adsterra") || tgt.contains("propu") || tgt.contains("deloplen") ||
               tgt.contains("whomeeno") || tgt.contains("ouo.io") ||
               tgt.contains("sfl.gl") || tgt.contains("shorturl") || tgt.contains("ibosport") ||
               tgt.contains("klikzeus") || tgt.contains("indokasino");
    }

    public static String extractHost(String url) {
        if (url == null) return null;
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            return host != null ? host.toLowerCase(Locale.ROOT) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isSearchOrPortal(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.ROOT);
        return h.contains("duckduckgo.com") ||
               h.contains("google.com") ||
               h.contains("google.co.") ||
               h.contains("bing.com") ||
               h.contains("wikipedia.org") ||
               h.contains("yahoo.com") ||
               h.contains("ecosia.org") ||
               h.contains("startpage.com") ||
               h.contains("yandex.com") ||
               h.contains("github.com") ||
               h.contains("linktr.ee") ||
               h.contains("linktree.com") ||
               h.contains("heylink.me") ||
               h.contains("bio.link") ||
               h.contains("beacons.ai");
    }

    public synchronized void recordBlock() {
        long current = getTotalBlocked();
        prefs.edit().putLong(KEY_TOTAL_BLOCKED, current + 1).apply();
    }

    public long getTotalBlocked() {
        return prefs.getLong(KEY_TOTAL_BLOCKED, 0);
    }

    public String getFormattedDataSaved() {
        long blocked = getTotalBlocked();
        double kbSaved = blocked * 75.0;
        if (kbSaved < 1024) {
            return String.format(Locale.getDefault(), "%.0f KB", kbSaved);
        } else if (kbSaved < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", kbSaved / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", kbSaved / (1024.0 * 1024.0));
        }
    }

    public String getFormattedTimeSaved() {
        long blocked = getTotalBlocked();
        double seconds = (blocked * 70.0) / 1000.0;
        if (seconds < 60) {
            return String.format(Locale.getDefault(), "%.0f dtk", seconds);
        } else {
            return String.format(Locale.getDefault(), "%.1f mnt", seconds / 60.0);
        }
    }

    public boolean isWhitelisted(String host) {
        if (host == null) return false;
        Set<String> whitelist = prefs.getStringSet(KEY_WHITELIST, new HashSet<>());
        return whitelist.contains(host.toLowerCase(Locale.ROOT));
    }

    public void setShieldEnabled(String host, boolean enabled) {
        if (host == null) return;
        String cleanHost = host.toLowerCase(Locale.ROOT);
        Set<String> whitelist = new HashSet<>(prefs.getStringSet(KEY_WHITELIST, new HashSet<>()));
        if (enabled) {
            whitelist.remove(cleanHost);
        } else {
            whitelist.add(cleanHost);
        }
        prefs.edit().putStringSet(KEY_WHITELIST, whitelist).apply();
    }

    // Skrip Injeksi Kosmetik Anti-Iklan Streaming, Anti-Popup, & Proteksi Pemutar Video
    public static String getCosmeticHidingScript() {
        return "javascript:(function() {" +
                "function isAdUrl(u) {" +
                "    if (!u) return false;" +
                "    var l = ('' + u).toLowerCase();" +
                "    if (l.indexOf('ketik.live') !== -1 || l.indexOf('t.me') !== -1 || l.indexOf('telegram.org') !== -1 || l.indexOf('iamcdn.net') !== -1) return false;" +
                "    return l.indexOf('slot') !== -1 || l.indexOf('zeus') !== -1 || " +
                "           l.indexOf('gacor') !== -1 || l.indexOf('judi') !== -1 || " +
                "           l.indexOf('bet') !== -1 || l.indexOf('casino') !== -1 || " +
                "           l.indexOf('depo') !== -1 || l.indexOf('togel') !== -1 || " +
                "           l.indexOf('popunder') !== -1 || l.indexOf('popads') !== -1 || " +
                "           l.indexOf('adsterra') !== -1 || l.indexOf('monetag') !== -1 || " +
                "           l.indexOf('onclick') !== -1 || l.indexOf('propu.sh') !== -1 || " +
                "           l.indexOf('deloplen') !== -1 || l.indexOf('whomeeno') !== -1 || " +
                "           l.indexOf('al5sm.com') !== -1 || " +
                "           l.indexOf('ibosport') !== -1 || l.indexOf('klikzeus') !== -1 || " +
                "           l.indexOf('indokasino') !== -1 || l.indexOf('pusatmovie') !== -1 || " +
                "           l.indexOf('decafeligiblyhad') !== -1 || " +
                "           l.indexOf('intent://') !== -1 || l.indexOf('market://') !== -1;" +
                "}" +
                "try {" +
                // Anti-adblock defusers (Brave-style) agar video player tidak mendeteksi adblocker
                "    window.canRunAds = true;" +
                "    window.isAdBlockActive = false;" +
                "    window.adblock = false;" +
                "    window.adblocker = false;" +
                "} catch(e) {}" +
                "var adSel = '.adsbygoogle, [id*=\"google_ads\"], iframe[src*=\"doubleclick\"], ' + " +
                "'[class*=\"ad-banner\"], [class*=\"ad-container\"], [id*=\"banner_ad\"], ' + " +
                "'[class*=\"sponsor\"], [class*=\"native-ad\"], ins.adsbygoogle, ' + " +
                "'[id*=\"advertisement\"], [class*=\"advertisement\"], [class*=\"adbox\"], ' + " +
                "'[id*=\"adbox\"], .dfp-ad, [class*=\"flying-carpet\"], [class*=\"sticky-ad\"], ' + " +
                "'[id*=\"sticky-ad\"], [class*=\"banner-sticky\"], [id*=\"banner-sticky\"], ' + " +
                "'[class*=\"iklan\"], [id*=\"iklan\"], [class*=\"ad-banner\"], [id*=\"ad-banner\"], [class*=\"banner-ad\"], [id*=\"banner-ad\"], ' + " +
                "'[class*=\"slot\"], [id*=\"slot\"], [class*=\"judi\"], [id*=\"judi\"], ' + " +
                "'a[href*=\"zeus\"], a[href*=\"slot\"], a[href*=\"gacor\"], a[href*=\"judi\"], ' + " +
                "'a[href*=\"bet\"], a[href*=\"casino\"], a[href*=\"depo\"], a[href*=\"klikzeus\"], ' + " +
                "'a[href*=\"ibosport\"], a[href*=\"indokasino\"], a[href*=\"pusatmovie\"], ' + " +
                "'img[src*=\"banner-ad\"], img[src*=\"ad-banner\"], img[src*=\"banner_ad\"], img[src*=\"iklan\"], img[src*=\"slot\"], img[src*=\"zeus\"], ' + " +
                "'div[id^=\"div-gpt-ad\"], div[class^=\"ad-\"], div[id^=\"ad-\"], ' + " +
                "'div[class*=\"outstream\"], div[id*=\"outstream\"], iframe[id*=\"ad_\"], ' + " +
                "'#idmuvi-popup, .gmr-bannerpopup, .gmr-bannerpopup-inner, ' + " +
                "'[id*=\"play-overlay\"], [class*=\"play-overlay\"], [id*=\"player-overlay\"], [class*=\"player-overlay\"], ' + " +
                "'[id*=\"click-layer\"], [class*=\"click-layer\"]';" +
                "function cleanAllAds() {" +
                "    try {" +
                // Sembunyikan elemen iklan CSS
                "        var els = document.querySelectorAll(adSel);" +
                "        for (var i = 0; i < els.length; i++) {" +
                "            els[i].style.setProperty('display', 'none', 'important');" +
                "            els[i].style.setProperty('height', '0px', 'important');" +
                "            els[i].style.setProperty('visibility', 'hidden', 'important');" +
                "            els[i].style.setProperty('opacity', '0', 'important');" +
                "            els[i].style.setProperty('pointer-events', 'none', 'important');" +
                "        }" +
                // Hancurkan banner bergambar & tautan afiliasi judi/slot/iklan
                "        var links = document.getElementsByTagName('a');" +
                "        for (var j = 0; j < links.length; j++) {" +
                "            var h = links[j].href || '';" +
                "            if (isAdUrl(h)) {" +
                "                links[j].style.setProperty('display', 'none', 'important');" +
                "                if (links[j].parentElement && (links[j].parentElement.tagName === 'CENTER' || links[j].parentElement.children.length === 1)) {" +
                "                    links[j].parentElement.style.setProperty('display', 'none', 'important');" +
                "                }" +
                "            }" +
                "        }" +
                // Hancurkan pop-up banner judi & overlay transparan / fake player di atas pemutar video
                "        var pop = document.getElementById('idmuvi-popup'); if (pop) pop.remove();" +
                "        var overlays = document.querySelectorAll('[id*=\"play-overlay\"], [class*=\"play-overlay\"], [id*=\"player-overlay\"], [class*=\"player-overlay\"], [id*=\"fake-player\"], [class*=\"fake-player\"], [class*=\"vignette\"], [id*=\"vignette\"], [class*=\"jw-ad\"], [id*=\"jw-ad\"], .jw-ad-container, .jw-ad-layer, [class*=\"click-layer\"], [id*=\"click-layer\"], .gmr-bannerpopup');" +
                "        for (var k = 0; k < overlays.length; k++) {" +
                "            var el = overlays[k];" +
                "            if (el.tagName !== 'VIDEO' && el.tagName !== 'IFRAME') {" +
                "                el.style.setProperty('display', 'none', 'important');" +
                "                el.style.setProperty('pointer-events', 'none', 'important');" +
                "            }" +
                "        }" +
                // Bersihkan iframe iklan murni tanpa merusak iframe player video
                "        var iframes = document.getElementsByTagName('iframe');" +
                "        for (var f = 0; f < iframes.length; f++) {" +
                "            var ifr = iframes[f];" +
                "            var isrc = (ifr.src || '').toLowerCase();" +
                "            if (isAdUrl(isrc)) {" +
                "                ifr.style.setProperty('display', 'none', 'important');" +
                "                ifr.src = 'about:blank';" +
                "            }" +
                "        }" +
                "    } catch(e) {}" +
                "}" +
                "cleanAllAds();" +
                "if (!window.__vexShieldInit) {" +
                "    window.__vexShieldInit = true;" +
                "    var style = document.createElement('style');" +
                "    style.type = 'text/css';" +
                "    style.innerHTML = adSel + ' { display: none !important; height: 0 !important; width: 0 !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; }';" +
                "    (document.head || document.documentElement).appendChild(style);" +
                // Tangkap event klik di fase capturing untuk menggugurkan klik iklan sebelum diproses web
                "    window.addEventListener('click', function(e) {" +
                "        var target = e.target;" +
                "        var a = target ? (target.closest ? target.closest('a') : null) : null;" +
                "        if (a) {" +
                "            var href = (a.href || '').toLowerCase();" +
                "            var cur = (window.location.hostname || '').toLowerCase();" +
                "            var isHijack = target.closest && (target.closest('.player-wrap') || target.closest('#player-1') || target.closest('.gmr-embed-responsive') || target.closest('#idmuvi-popup')) && href.indexOf('http') === 0 && href.indexOf(cur) === -1 && href.indexOf('ketik.live') === -1 && href.indexOf('t.me') === -1;" +
                "            if (isAdUrl(href) || isHijack) {" +
                "                e.preventDefault();" +
                "                e.stopPropagation();" +
                "                e.stopImmediatePropagation();" +
                "                console.log('VexShield intercepted click on ad link: ' + href);" +
                "                return false;" +
                "            }" +
                "        }" +
                "    }, true);" +
                "    var obs = new MutationObserver(function() { cleanAllAds(); });" +
                "    obs.observe(document.documentElement || document.body, { childList: true, subtree: true });" +
                "    setInterval(cleanAllAds, 600);" +
                "}" +
                "})();";
    }
}
