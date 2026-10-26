package com.martinb.marbleos;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.webkit.WebViewAssetLoader;

/**
 * MarbleOS: the game runs fully offline from the app's assets, under the gold MartinB window bar
 * (minimize, full screen, close) shared by every MartinB app.
 */
public class MainActivity extends Activity {
    private static final String HOME = "https://appassets.androidplatform.net/assets/index.html";
    private static final int BAR = Color.parseColor("#0B0A08");
    private static final int GOLD = Color.parseColor("#D4AD62");
    private static final int INK = Color.parseColor("#F3EFE6");

    private WebView web;
    private ImageButton maxButton;
    private boolean immersive = true; // the game opens full screen, as before

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BAR);
        root.addView(buildBar(), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        root.addView(web, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setTextZoom(100);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Stay inside the game; never open other pages.
                return !"appassets.androidplatform.net".equals(request.getUrl().getHost());
            }
        });

        if (state != null) web.restoreState(state);
        else web.loadUrl(HOME);
        applyImmersive();
        if (state == null) UpdateCheck.run(this);
    }

    // ---------- Gold window bar ----------

    private View buildBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(BAR);
        bar.setPadding(dp(12), 0, dp(2), 0);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(24), dp(24));
        ilp.rightMargin = dp(10);
        bar.addView(icon, ilp);

        TextView title = new TextView(this);
        SpannableString name = new SpannableString("MarbleOS");
        name.setSpan(new ForegroundColorSpan(GOLD), 6, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(name);
        title.setTextColor(INK);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        title.setSingleLine(true);
        bar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        boolean fr = java.util.Locale.getDefault().getLanguage().equals("fr");
        bar.addView(barButton(R.drawable.mb_min, fr ? "Réduire" : "Minimize", v -> moveTaskToBack(true)));
        maxButton = barButton(R.drawable.mb_restore, fr ? "Plein écran" : "Full screen", v -> {
            immersive = !immersive;
            applyImmersive();
        });
        bar.addView(maxButton);
        bar.addView(barButton(R.drawable.mb_close, fr ? "Fermer" : "Close", v -> finishAndRemoveTask()));
        return bar;
    }

    private ImageButton barButton(int drawable, String label, View.OnClickListener onClick) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(drawable);
        b.setContentDescription(label);
        b.setScaleType(ImageView.ScaleType.CENTER);
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true)) b.setBackgroundResource(tv.resourceId);
        else b.setBackgroundColor(Color.TRANSPARENT);
        b.setOnClickListener(onClick);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(46), dp(44)));
        return b;
    }

    @SuppressWarnings("deprecation")
    private void applyImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(immersive
                ? View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                : View.SYSTEM_UI_FLAG_VISIBLE);
        if (maxButton != null) maxButton.setImageResource(immersive ? R.drawable.mb_restore : R.drawable.mb_max);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ---------- Lifecycle ----------

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && immersive) applyImmersive();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onPause() {
        web.evaluateJavascript("window.__mosPause && window.__mosPause()", null);
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        // Back opens the pause menu or goes back a screen; on the home screen it closes the app.
        web.evaluateJavascript("window.__mosBack ? window.__mosBack() : 'exit'", value -> {
            if (value == null || value.contains("exit")) finish();
        });
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
