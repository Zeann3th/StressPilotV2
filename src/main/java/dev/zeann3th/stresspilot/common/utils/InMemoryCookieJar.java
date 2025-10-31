package dev.zeann3th.stresspilot.common.utils;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCookieJar implements CookieJar {
    private final Map<String, List<Cookie>> cookieStore = new ConcurrentHashMap<>();

    @Override
    public void saveFromResponse(HttpUrl url, @NotNull List<Cookie> cookies) {
        cookieStore.put(url.host(), new ArrayList<>(cookies));
    }

    @NotNull
    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = cookieStore.get(url.host());
        if (cookies == null) {
            return Collections.emptyList();
        }

        List<Cookie> validCookies = new ArrayList<>();
        for (Cookie cookie : cookies) {
            if (cookie.expiresAt() > System.currentTimeMillis()) {
                validCookies.add(cookie);
            }
        }
        return validCookies;
    }

    public void clear() {
        cookieStore.clear();
    }
}
