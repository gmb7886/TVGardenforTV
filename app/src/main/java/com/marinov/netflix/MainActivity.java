package com.marinov.netflix;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class MainActivity extends Activity {
    private CustomWebView webView;
    private ImageView cursorView;
    private float cursorX;
    private float cursorY;
    private static final int STEP = 40;
    private static final int SCROLL_STEP = 200;
    private int screenWidth;
    private int screenHeight;
    private boolean initialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Garantir hardware acceleration para playback de mídia
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        cursorView = findViewById(R.id.cursor);

        // Configura WebView
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportMultipleWindows(true);
        settings.setAllowContentAccess(true);
        // Permite autoplay de mídia sem gesto do usuário
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Define User-Agent de desktop
        String desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36";
        settings.setUserAgentString(desktopUA);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        // Habilita permissões de mídia protegida (Widevine)
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                for (String resource : request.getResources()) {
                    if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resource)) {
                        request.grant(new String[]{PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID});
                        return;
                    }
                }
                request.deny();
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.requestFocusFromTouch();
            }
        });
        webView.loadUrl("https://www.netflix.com");

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        FrameLayout root = findViewById(android.R.id.content);
        root.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (!initialized) {
                            cursorX = screenWidth / 2f;
                            cursorY = screenHeight / 2f;
                            updateCursor();
                            initialized = true;
                            root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                }
        );
    }

    private void updateCursor() {
        float x = Math.min(Math.max(0, cursorX - cursorView.getWidth() / 2f), screenWidth - cursorView.getWidth());
        float y = Math.min(Math.max(0, cursorY - cursorView.getHeight() / 2f), screenHeight - cursorView.getHeight());
        cursorView.setX(x);
        cursorView.setY(y);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (cursorY > STEP) {
                        cursorY -= STEP;
                        updateCursor();
                    } else {
                        webView.scrollBy(0, -SCROLL_STEP);
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (cursorY < screenHeight - STEP) {
                        cursorY += STEP;
                        updateCursor();
                    } else {
                        webView.scrollBy(0, SCROLL_STEP);
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (cursorX > STEP) {
                        cursorX -= STEP;
                        updateCursor();
                    } else {
                        webView.scrollBy(-SCROLL_STEP, 0);
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (cursorX < screenWidth - STEP) {
                        cursorX += STEP;
                        updateCursor();
                    } else {
                        webView.scrollBy(SCROLL_STEP, 0);
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_BUTTON_SELECT:
                    long now = System.currentTimeMillis();
                    MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0);
                    MotionEvent up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, cursorX, cursorY, 0);
                    webView.dispatchTouchEvent(down);
                    webView.dispatchTouchEvent(up);
                    down.recycle();
                    up.recycle();

                    WebView.HitTestResult hit = webView.getHitTestResult();
                    if (hit.getType() == WebView.HitTestResult.EDIT_TEXT_TYPE) {
                        webView.requestFocusFromTouch();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT);
                    }
                    return true;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
