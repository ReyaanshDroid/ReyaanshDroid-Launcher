package com.reyaansh72.reyaanshdroidlauncher;

import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextClock;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AppAdapter adapter;
    private List<AppInfo> appsList = new ArrayList<>();
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private View homeScreenArea;
    private SharedPreferences prefs;
    private EditText searchBox;
    private String selectedCategory = "All";
    private ExecutorService bgExecutor;
    private Vibrator vibrator;

    private final BroadcastReceiver mediaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MediaListenerService.ACTION_MEDIA_UPDATE.equals(intent.getAction())) {
                String title = intent.getStringExtra(MediaListenerService.EXTRA_TITLE);
                String artist = intent.getStringExtra(MediaListenerService.EXTRA_ARTIST);
                boolean isPlaying = intent.getBooleanExtra(MediaListenerService.EXTRA_PLAYING, false);

                TextView mediaTitle = findViewById(R.id.media_title);
                TextView mediaArtist = findViewById(R.id.media_artist);
                ImageButton playPauseBtn = findViewById(R.id.media_play_pause);

                if (mediaTitle != null) mediaTitle.setText(title != null ? title : "No Media");
                if (mediaArtist != null) mediaArtist.setText(artist != null ? artist : "");
                if (playPauseBtn != null) {
                    playPauseBtn.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                }
            }
        }
    };

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            
            if (level != -1 && scale != -1) {
                int batteryPct = (int) ((level / (float) scale) * 100);
                TextView batteryText = findViewById(R.id.widget_battery_text);
                if (batteryText != null) {
                    StringBuilder sb = new StringBuilder("🔋 ").append(batteryPct).append("%");
                    if (status == BatteryManager.BATTERY_STATUS_CHARGING) sb.append(" (Charging)");
                    batteryText.setText(sb.toString());
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // SYNC THEME WITH SYSTEM SETTINGS (MATERIAL YOU)
        // We force dynamic colors to true to follow the Android System/Settings app color
        DynamicColors.applyToActivityIfAvailable(this);
        
        applyThemeMode();
        super.onCreate(savedInstanceState);
        
        // Full immersive experience for custom ROM
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        bgExecutor = Executors.newSingleThreadExecutor();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        homeScreenArea = findViewById(R.id.home_screen_layout);
        
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        recyclerView = findViewById(R.id.apps_recycler_view);
        searchBox = findViewById(R.id.search_box);
        
        setupHomeSearchBar();
        setupGestures();
        setupBottomSheet();
        setupSearch();
        setupCategoryTabs();
        loadAppsAsync();
        applySettings();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });
        
        syncWallpaperColors();
    }

    private void syncWallpaperColors() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            WallpaperManager wm = WallpaperManager.getInstance(this);
            wm.addOnColorsChangedListener((colors, which) -> {
                // If wallpaper colors change in system settings, restart activity to apply new Material You theme
                recreate();
            }, null);
        }
    }

    private void applyThemeMode() {
        String theme = prefs.getString("theme_mode", "system");
        switch (theme) {
            case "light": AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case "dark": AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
    }

    private void setupSearch() {
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) adapter.filter(s.toString(), selectedCategory);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupHomeSearchBar() {
        View searchBar = findViewById(R.id.home_search_bar);
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                searchBox.requestFocus();
            });
        }

        ImageView voiceBtn = findViewById(R.id.btn_voice_search);
        if (voiceBtn != null) {
            voiceBtn.setVisibility(prefs.getBoolean("show_voice_search", true) ? View.VISIBLE : View.GONE);
            voiceBtn.setOnClickListener(v -> {
                try {
                    startActivityForResult(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM), 101);
                } catch (Exception e) {
                    Toast.makeText(this, "Voice error", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupGestures() {
        GestureDetector gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                performVibration();
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
            @Override
            public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null) return false;
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dy) > 100 && Math.abs(vy) > 100) {
                    if (dy > 0 && prefs.getBoolean("swipe_down_notifications", true)) expandNotificationsPanel();
                    else if (dy < 0) bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    return true;
                }
                return false;
            }
        });
        homeScreenArea.setOnTouchListener((v, event) -> { gd.onTouchEvent(event); return true; });
    }

    private void performVibration() {
        if (prefs.getBoolean("vibration_feedback", true) && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(android.os.VibrationEffect.createOneShot(25, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(25);
        }
    }

    private void expandNotificationsPanel() {
        try {
            @SuppressWarnings("WrongConstant")
            Object service = getSystemService("statusbar");
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(service);
        } catch (Exception ignored) {}
    }

    private void setupCategoryTabs() {
        TabLayout tabs = findViewById(R.id.category_tabs);
        if (tabs != null) {
            tabs.setVisibility(prefs.getBoolean("show_category_tabs", true) ? View.VISIBLE : View.GONE);
            tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    selectedCategory = tab.getText() != null ? tab.getText().toString() : "All";
                    if (adapter != null) adapter.filter(searchBox.getText().toString(), selectedCategory);
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });
        }
    }

    private void applySettings() {
        findViewById(R.id.widgets_container).setVisibility(prefs.getBoolean("clean_mode", false) ? View.GONE : View.VISIBLE);
        
        // Search Bar Style & Color Sync
        View bar = findViewById(R.id.home_search_bar);
        if (bar != null) {
            String style = prefs.getString("home_search_style", "pill");
            if ("hidden".equals(style)) bar.setVisibility(View.GONE);
            else {
                bar.setVisibility(View.VISIBLE);
                GradientDrawable gd = new GradientDrawable();
                // Sync with system surface color
                int surface = MaterialColors.getColor(bar, com.google.android.material.R.attr.colorSurfaceContainer);
                gd.setColor("minimal".equals(style) ? Color.TRANSPARENT : surface);
                gd.setCornerRadius("pill".equals(style) ? 1000f : 32f);
                bar.setBackground(gd);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) bar.getLayoutParams();
                lp.topMargin = (int) (prefs.getInt("home_margin_top", 48) * getResources().getDisplayMetrics().density);
                bar.setLayoutParams(lp);
            }
        }

        applyWidgetSettings();
        
        String font = prefs.getString("app_font", "sans-serif");
        Typeface tf = Typeface.create(font, Typeface.NORMAL);
        TextClock clock = findViewById(R.id.textClock);
        if (clock != null) {
            clock.setTypeface(tf, Typeface.BOLD);
            clock.setFormat12Hour(prefs.getString("clock_format", "h:mm a"));
        }
        TextView date = findViewById(R.id.widget_date_text);
        if (date != null) {
            date.setTypeface(tf);
            date.setText(new SimpleDateFormat(prefs.getString("date_format", "EEEE, MMMM dd"), Locale.getDefault()).format(new Date()));
        }

        // Apply Dynamic System Corner Radii
        float radius = prefs.getInt("ui_corner_radius", 28) * getResources().getDisplayMetrics().density;
        applyCornerRadius(radius);
        applyDrawerSettings();

        if (adapter != null) {
            adapter.updateSettings(prefs.getInt("icon_size", 100), prefs.getBoolean("show_labels", true), 
                prefs.getInt("label_size", 12), font, prefs.getString("icon_pack", "default"), prefs.getBoolean("single_line_labels", true));
        }

        updateSystemHubWidgets();
        populateDockAndFrequentApps();
    }

    private void applyWidgetSettings() {
        findViewById(R.id.widget_system_hub).setVisibility(prefs.getBoolean("show_system_hub", true) ? View.VISIBLE : View.GONE);
        findViewById(R.id.widget_weather_text).setVisibility(prefs.getBoolean("show_weather", true) ? View.VISIBLE : View.GONE);
        findViewById(R.id.widget_media_glance).setVisibility(prefs.getBoolean("show_media_widget", true) ? View.VISIBLE : View.GONE);
    }

    private void applyCornerRadius(float radius) {
        MaterialCardView[] cards = {findViewById(R.id.widget_quick_glance), findViewById(R.id.widget_system_hub), 
            findViewById(R.id.widget_media_glance), findViewById(R.id.dock_card)};
        for (MaterialCardView card : cards) if (card != null) card.setRadius(radius);
    }

    private void applyDrawerSettings() {
        int cols = prefs.getInt("drawer_grid_columns", 4);
        recyclerView.setLayoutManager(new GridLayoutManager(this, cols));
        findViewById(R.id.search_box).setVisibility(prefs.getBoolean("show_search", true) ? View.VISIBLE : View.GONE);
        findViewById(R.id.frequent_apps_container).setVisibility(prefs.getBoolean("show_frequent_apps", true) ? View.VISIBLE : View.GONE);

        int opacity = prefs.getInt("drawer_opacity", 85);
        int blur = prefs.getInt("drawer_blur", 40);
        View drawer = findViewById(R.id.apps_drawer);
        if (drawer != null) {
            // ALWAYS SYNC DRAWER COLOR WITH SYSTEM SURFACE
            int surface = MaterialColors.getColor(drawer, com.google.android.material.R.attr.colorSurface);
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.argb((opacity * 255) / 100, Color.red(surface), Color.green(surface), Color.blue(surface)));
            float rad = getResources().getDimension(R.dimen.drawer_corner_radius);
            gd.setCornerRadii(new float[]{rad, rad, rad, rad, 0, 0, 0, 0});
            drawer.setBackground(gd);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) drawer.setRenderEffect(blur > 0 ? android.graphics.RenderEffect.createBlurEffect(blur/3f + 1, blur/3f + 1, android.graphics.Shader.TileMode.CLAMP) : null);
        }
    }

    private void updateSystemHubWidgets() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            int ramPct = (int) (((mi.totalMem - mi.availMem) / (float) mi.totalMem) * 100);
            ((TextView) findViewById(R.id.ram_label)).setText("RAM Usage: " + ramPct + "%");
            ((LinearProgressIndicator) findViewById(R.id.ram_progress)).setProgress(ramPct);

            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            long total = stat.getTotalBytes();
            int storagePct = (int) (((total - stat.getFreeBytes()) / (float) total) * 100);
            ((TextView) findViewById(R.id.storage_label)).setText("Internal Storage: " + storagePct + "%");
            ((LinearProgressIndicator) findViewById(R.id.storage_progress)).setProgress(storagePct);
        } catch (Exception ignored) {}
    }

    private void populateDockAndFrequentApps() {
        LinearLayout dock = findViewById(R.id.dock_apps_layout);
        MaterialCardView dockCard = findViewById(R.id.dock_card);
        if (dock == null || dockCard == null) return;
        boolean showDock = prefs.getBoolean("show_dock", true);
        dockCard.setVisibility(showDock ? View.VISIBLE : View.GONE);
        if (showDock) {
            dock.removeAllViews();
            int count = prefs.getInt("dock_count", 5);
            String style = prefs.getString("dock_style", "glass");
            int surface = MaterialColors.getColor(dockCard, com.google.android.material.R.attr.colorSurfaceContainer);
            dockCard.setCardBackgroundColor("solid".equals(style) ? surface : Color.parseColor("#15FFFFFF"));
            if ("transparent".equals(style)) dockCard.setCardBackgroundColor(Color.TRANSPARENT);
            dockCard.setRadius("pill".equals(style) ? 1000f : 48f);
            
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) dockCard.getLayoutParams();
            mlp.bottomMargin = (int) (prefs.getInt("dock_padding", 16) * getResources().getDisplayMetrics().density);
            dockCard.setLayoutParams(mlp);

            LayoutInflater inf = LayoutInflater.from(this);
            for (int i = 0; i < Math.min(count, appsList.size()); i++) {
                AppInfo app = appsList.get(i);
                View v = inf.inflate(R.layout.item_app, dock, false);
                v.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
                ((ImageView) v.findViewById(R.id.app_icon)).setImageDrawable(app.getIcon());
                v.findViewById(R.id.app_label).setVisibility(prefs.getBoolean("dock_labels", false) ? View.VISIBLE : View.GONE);
                v.setOnClickListener(view -> { performVibration(); startActivity(getPackageManager().getLaunchIntentForPackage(app.getPackageName())); });
                dock.addView(v);
            }
        }
        
        LinearLayout freq = findViewById(R.id.frequent_apps_layout);
        if (freq != null) {
            freq.removeAllViews();
            if (prefs.getBoolean("show_frequent_apps", true)) {
                LayoutInflater inf = LayoutInflater.from(this);
                for (int i = 0; i < Math.min(4, appsList.size()); i++) {
                    AppInfo app = appsList.get(i);
                    View v = inf.inflate(R.layout.item_app, freq, false);
                    v.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
                    ((ImageView) v.findViewById(R.id.app_icon)).setImageDrawable(app.getIcon());
                    ((TextView) v.findViewById(R.id.app_label)).setText(app.getLabel());
                    v.setOnClickListener(view -> { performVibration(); startActivity(getPackageManager().getLaunchIntentForPackage(app.getPackageName())); bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); });
                    freq.addView(v);
                }
            }
        }
    }

    @Override protected void onResume() { super.onResume(); registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); IntentFilter mf = new IntentFilter(MediaListenerService.ACTION_MEDIA_UPDATE); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(mediaReceiver, mf, Context.RECEIVER_EXPORTED); else registerReceiver(mediaReceiver, mf); applySettings(); }
    @Override protected void onPause() { super.onPause(); unregisterReceiver(batteryReceiver); unregisterReceiver(mediaReceiver); }

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.apps_drawer));
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(@NonNull View bs, int ns) { if (ns == BottomSheetBehavior.STATE_COLLAPSED) { searchBox.setText(""); searchBox.clearFocus(); } }
            @Override public void onSlide(@NonNull View bs, float so) { findViewById(R.id.widgets_container).setAlpha(1-so); findViewById(R.id.home_search_bar).setAlpha(1-so); findViewById(R.id.dock_card).setAlpha(1-so); }
        });
        findViewById(R.id.btn_settings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void loadAppsAsync() {
        bgExecutor.execute(() -> {
            List<AppInfo> temp = new ArrayList<>();
            PackageManager pm = getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER);
            for (ResolveInfo ri : pm.queryIntentActivities(intent, 0)) if (!ri.activityInfo.packageName.equals(getPackageName())) temp.add(new AppInfo(ri.loadLabel(pm).toString(), ri.activityInfo.packageName, ri.activityInfo.loadIcon(pm), determineCategory(ri.loadLabel(pm).toString(), ri.activityInfo.packageName)));
            temp.sort(Comparator.comparing(a -> a.getLabel().toLowerCase()));
            runOnUiThread(() -> { appsList = temp; adapter = new AppAdapter(this, appsList, () -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)); recyclerView.setAdapter(adapter); applySettings(); });
        });
    }

    private String determineCategory(String l, String p) {
        String lp = p.toLowerCase();
        if (lp.contains("game") || lp.contains("unity")) return "Games";
        if (lp.contains("facebook") || lp.contains("whatsapp") || lp.contains("insta") || lp.contains("twitter") || lp.contains("social")) return "Social";
        if (lp.startsWith("com.android") || lp.startsWith("com.google") || lp.contains("system")) return "System";
        return "All";
    }
}
