package com.appmada.bainfrancais;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int MIC_REQUEST = 1001;
    private WebView webView;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    request.grant(request.getResources());
                } else {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
                    request.deny();
                }
            }
        });
        webView.addJavascriptInterface(new AppBridge(), "AppMada");
        loadAppPage();
    }

    private void loadAppPage() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("index.html"), StandardCharsets.UTF_8))) {
            StringBuilder html = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) html.append(line).append('\n');
            webView.loadDataWithBaseURL("https://appmada.pages.dev/", html.toString(), "text/html", "UTF-8", null);
        } catch (Exception e) {
            webView.loadData("<h3>Impossible de charger Bain de français.</h3>", "text/html", "UTF-8");
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    public class AppBridge {
        private final String endpoint = "https://xvwsdoqiesnqpyrqahwf.supabase.co/functions/v1/app-mada-admin-api";
        private final String publishableKey = "sb_publishable_DECsSkq8OiZ0x0pwI3xEZg_LfnT1T41";

        @JavascriptInterface public String deviceId() {
            String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            return id == null ? "unknown" : id;
        }

        @JavascriptInterface public void requestMicrophone() {
            runOnUiThread(() -> {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
                }
            });
        }

        @JavascriptInterface public void openExternal(String url) {
            if (url == null || !url.startsWith("https://")) return;
            runOnUiThread(() -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
            });
        }

        @JavascriptInterface public String request(String body) {
            try {
                String payload = body == null || body.isEmpty() ? "{}" : body;
                JSONObject requestJson = new JSONObject(payload);
                String token = requestJson.optString("_token", "");
                String action = requestJson.optString("action", "");
                String result = post(payload, token);

                if (("bain_login".equals(action) || "bain_signup".equals(action))) {
                    try {
                        JSONObject login = new JSONObject(result);
                        if (login.optBoolean("ok", false)) {
                            String newToken = login.optString("token", "");
                            if (!newToken.isEmpty()) {
                                JSONObject me = new JSONObject(post("{\"action\":\"bain_me\"}", newToken));
                                if (me.optBoolean("ok", false)) {
                                    if (me.has("user")) login.put("user", me.getJSONObject("user"));
                                    if (me.has("wallet")) login.put("wallet", me.getJSONObject("wallet"));
                                    result = login.toString();
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
                return result;
            } catch (Exception e) {
                return "{\"ok\":false,\"error\":\"network_error\",\"detail\":\"" + safe(e.getMessage()) + "\"}";
            }
        }

        private String post(String payload, String token) {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(endpoint).openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(15000);
                c.setReadTimeout(120000);
                c.setRequestProperty("Accept", "application/json");
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                c.setRequestProperty("apikey", publishableKey);
                c.setRequestProperty("X-App-Id", "bain-francais");
                c.setRequestProperty("X-Device-Id", deviceId());
                if (token != null && !token.isEmpty()) c.setRequestProperty("x-app-mada-token", token);
                c.setDoOutput(true);
                try (OutputStream os = c.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }
                int status = c.getResponseCode();
                InputStream in = status >= 200 && status < 400 ? c.getInputStream() : c.getErrorStream();
                StringBuilder sb = new StringBuilder();
                if (in != null) try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line; while ((line = br.readLine()) != null) sb.append(line);
                }
                if (sb.length() == 0) return "{\"ok\":false,\"error\":\"empty_response\",\"status\":" + status + "}";
                return sb.toString();
            } catch (Exception e) {
                return "{\"ok\":false,\"error\":\"network_error\",\"detail\":\"" + safe(e.getMessage()) + "\"}";
            } finally {
                if (c != null) c.disconnect();
            }
        }

        private String safe(String s) {
            if (s == null) return "Erreur réseau";
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
        }
    }
}
