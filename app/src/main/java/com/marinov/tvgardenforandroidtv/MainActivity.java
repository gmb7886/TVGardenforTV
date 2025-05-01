package com.marinov.tvgardenforandroidtv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
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
    private FrameLayout customViewContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalSystemUiVisibility;
    private int originalOrientation;

    private ImageView cursorView;
    private float cursorX;
    private float cursorY;
    private static final int STEP = 40;
    private static final int SCROLL_STEP = 200;
    private int screenWidth;
    private int screenHeight;
    private boolean initialized = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        // Garantir hardware acceleration para playback de mídia
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_main);

        // Container para fullscreen de vídeo
        customViewContainer = new FrameLayout(this);
        addContentView(customViewContainer,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        webView = findViewById(R.id.webview);
        cursorView = findViewById(R.id.cursor);

        // Configura WebView focus e teclado
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // Configurações do WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportMultipleWindows(true);
        settings.setAllowContentAccess(true);
        // Permite autoplay de mídia sem gesto do usuário
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Define User-Agent desktop
        String desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36";
        settings.setUserAgentString(desktopUA);

        // Configura cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // WebChromeClient com permissões e fullscreen
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

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Salva estado e exibe fullscreen
                originalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
                originalOrientation = getRequestedOrientation();

                customViewContainer.addView(view,
                        new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
                customView = view;
                customViewCallback = callback;

                webView.setVisibility(View.GONE);
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

            @Override
            public void onHideCustomView() {
                // Remove fullscreen e restaura estado
                customViewContainer.removeView(customView);
                customView = null;
                customViewCallback.onCustomViewHidden();

                webView.setVisibility(View.VISIBLE);
                getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
                setRequestedOrientation(originalOrientation);
            }
        });

        // WebViewClient básico
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.requestFocusFromTouch();
            }
        });

        // Carrega URL
        webView.loadUrl("https://tv.garden");

        // Obtém dimensões da tela
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        // Inicializa cursor no centro
        FrameLayout root = findViewById(android.R.id.content);
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
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
        });
    }

    private void updateCursor() {
        float x = Math.min(Math.max(0, cursorX - cursorView.getWidth() / 2f),
                screenWidth - cursorView.getWidth());
        float y = Math.min(Math.max(0, cursorY - cursorView.getHeight() / 2f),
                screenHeight - cursorView.getHeight());
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

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}