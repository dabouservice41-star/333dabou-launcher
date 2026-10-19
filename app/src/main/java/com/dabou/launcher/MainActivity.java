package com.dabou.launcher;

// Subject: a personal Android home screen for Dabou, not a web simulation.
// Design: garden-green paper, leaf accent, system sans for controls and a light
// oversized clock. Flowing, rounded clusters borrow the loose Savoy vase logic.
// Layout: greeting / clock / search / favorites / application grid.
// Signature: a quiet green clock panel with a single orange settings control.

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private final List<AppEntry> apps = new ArrayList<>();
    private Set<String> favorites;
    private GridLayout grid;
    private LinearLayout favoriteSection;
    private EditText search;
    private TextView appCount;
    private int foreground, muted, panel, background, accent, columns;

    private static class AppEntry {
        final String name;
        final ResolveInfo info;
        final ComponentName component;
        AppEntry(String name, ResolveInfo info) {
            this.name = name;
            this.info = info;
            component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
        }
        String key() { return component.flattenToString(); }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        buildHome();
    }

    @Override protected void onResume() {
        super.onResume();
        loadApps();
        renderApps();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (search != null) {
            search.setText("");
            search.clearFocus();
            android.view.inputmethod.InputMethodManager keyboard =
                (android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(search.getWindowToken(), 0);
        }
    }

    private int dp(float value) {
        return (int) (getResources().getDisplayMetrics().density * value + 0.5f);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private TextView button(String label, Runnable action) {
        TextView button = text(label, 16, foreground);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(16), dp(14), dp(16), dp(14));
        button.setMinHeight(dp(48));
        button.setBackground(shape(panel, 22));
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void buildHome() {
        boolean dark = prefs.getBoolean("dark", false);
        background = Color.parseColor(dark ? "#17251F" : "#F4F6EF");
        panel = Color.parseColor(dark ? "#263B30" : "#E2EADB");
        foreground = Color.parseColor(dark ? "#EDF2E5" : "#20352B");
        muted = Color.parseColor(dark ? "#B7C8AF" : "#56694F");
        accent = prefs.getInt("accent", Color.parseColor("#648C16"));
        float tint = dark ? 0.25f : 0.17f;
        panel = Color.rgb(
            Math.round(Color.red(background) * (1 - tint) + Color.red(accent) * tint),
            Math.round(Color.green(background) * (1 - tint) + Color.green(accent) * tint),
            Math.round(Color.blue(background) * (1 - tint) + Color.blue(accent) * tint));
        columns = prefs.getInt("columns", 4);
        favorites = new HashSet<>(prefs.getStringSet("favorites", new HashSet<>()));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(background);
        root.setPadding(dp(20), dp(12), dp(20), dp(8));
        root.setFitsSystemWindows(true);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(dp(20) + bars.left, dp(12) + bars.top,
                    dp(20) + bars.right, dp(8) + bars.bottom);
            } else {
                v.setPadding(dp(20) + insets.getSystemWindowInsetLeft(),
                    dp(12) + insets.getSystemWindowInsetTop(),
                    dp(20) + insets.getSystemWindowInsetRight(),
                    dp(8) + insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        setContentView(root);
        root.requestApplyInsets();

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(prefs.getString("title", "Chez Dabou"), 25, foreground);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView settings = button("Réglages", this::showSettings);
        settings.setTextColor(Color.parseColor("#20352B"));
        settings.setBackground(shape(Color.parseColor("#FFB274"), 22));
        heading.addView(settings);
        root.addView(heading);

        LinearLayout clockPanel = new LinearLayout(this);
        clockPanel.setOrientation(LinearLayout.VERTICAL);
        clockPanel.setPadding(dp(20), dp(10), dp(20), dp(18));
        clockPanel.setBackground(shape(panel, 32));
        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(-1, -2);
        clockParams.setMargins(0, dp(18), 0, dp(14));
        root.addView(clockPanel, clockParams);
        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm");
        clock.setFormat24Hour("HH:mm");
        clock.setTextSize(64);
        clock.setTextColor(foreground);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clockPanel.addView(clock);
        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE d MMMM");
        date.setFormat24Hour("EEEE d MMMM");
        date.setTextColor(muted);
        date.setTextSize(16);
        clockPanel.addView(date);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(16);
        search.setTextColor(foreground);
        search.setHintTextColor(muted);
        search.setHint("Rechercher une application");
        search.setContentDescription("Rechercher une application");
        search.setPadding(dp(18), dp(12), dp(18), dp(12));
        search.setBackground(shape(panel, 22));
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(52)));
        root.setFocusableInTouchMode(true);
        root.requestFocus();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollParams.topMargin = dp(12);
        root.addView(scroll, scrollParams);
        LinearLayout contents = new LinearLayout(this);
        contents.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(contents);
        favoriteSection = new LinearLayout(this);
        favoriteSection.setOrientation(LinearLayout.VERTICAL);
        contents.addView(favoriteSection);
        appCount = text("", 16, muted);
        appCount.setPadding(0, dp(14), 0, dp(12));
        contents.addView(appCount);
        grid = new GridLayout(this);
        grid.setColumnCount(columns);
        contents.addView(grid, new LinearLayout.LayoutParams(-1, -2));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderApps();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        loadApps();
        renderApps();
    }

    @SuppressWarnings("deprecation")
    private void loadApps() {
        apps.clear();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = getPackageManager().queryIntentActivities(query, 0);
        Set<String> seen = new HashSet<>();
        for (ResolveInfo info : resolved) {
            if (!info.activityInfo.exported) continue;
            if (info.activityInfo.packageName.equals(getPackageName())) continue;
            AppEntry app = new AppEntry(info.loadLabel(getPackageManager()).toString(), info);
            if (seen.add(app.key())) apps.add(app);
        }
        Collator collator = Collator.getInstance(Locale.getDefault());
        apps.sort((a, b) -> collator.compare(a.name, b.name));
    }

    private void renderApps() {
        if (grid == null) return;
        String query = search.getText().toString().trim().toLowerCase(Locale.getDefault());
        grid.removeAllViews();
        favoriteSection.removeAllViews();
        boolean hasFavorites = false;
        for (AppEntry app : apps) if (favorites.contains(app.key())) hasFavorites = true;
        favoriteSection.setVisibility(query.isEmpty() && hasFavorites ? View.VISIBLE : View.GONE);
        if (query.isEmpty() && hasFavorites) {
            TextView label = text("Favoris", 16, muted);
            label.setPadding(0, dp(12), 0, dp(8));
            favoriteSection.addView(label);
            GridLayout favGrid = new GridLayout(this);
            favGrid.setColumnCount(columns);
            favoriteSection.addView(favGrid, new LinearLayout.LayoutParams(-1, -2));
            for (AppEntry app : apps) {
                if (favorites.contains(app.key())) addTile(favGrid, app);
            }
        }
        int visible = 0;
        for (AppEntry app : apps) {
            if (app.name.toLowerCase(Locale.getDefault()).contains(query)) {
                addTile(grid, app);
                visible++;
            }
        }
        appCount.setText(visible == 0 ? "Aucune application trouvée"
            : visible + (visible == 1 ? " application" : " applications"));
    }

    private void addTile(GridLayout target, AppEntry app) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(3), dp(10), dp(3), dp(10));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(116);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        target.addView(tile, params);
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.info.loadIcon(getPackageManager()));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView label = text(app.name, 14, foreground);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setPadding(0, dp(7), 0, 0);
        tile.addView(label, new LinearLayout.LayoutParams(-1, -2));
        tile.setBackground(shape(favorites.contains(app.key()) ? panel : background, 20));
        tile.setContentDescription(app.name + ". Appui long pour les options.");
        tile.setFocusable(true);
        tile.setOnClickListener(v -> launch(app));
        tile.setOnLongClickListener(v -> {
            boolean saved = favorites.contains(app.key());
            new AlertDialog.Builder(this).setTitle(app.name)
                .setItems(new String[]{
                    saved ? "Retirer des favoris" : "Ajouter aux favoris",
                    "Informations de l'application"
                }, (dialog, choice) -> {
                    if (choice == 0) {
                        if (saved) favorites.remove(app.key()); else favorites.add(app.key());
                        prefs.edit().putStringSet("favorites", new HashSet<>(favorites)).apply();
                        renderApps();
                    } else {
                        safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + app.component.getPackageName())));
                    }
                }).show();
            return true;
        });
    }

    private void launch(AppEntry app) {
        Intent intent = new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        safeStart(intent);
    }

    private void safeStart(Intent intent) {
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException | SecurityException error) {
            Toast.makeText(this, "Cette action n'est pas disponible sur cet appareil.",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void showSettings() {
        String mode = prefs.getBoolean("dark", false) ? "Passer en mode clair" : "Passer en mode sombre";
        new AlertDialog.Builder(this).setTitle("Ton launcher")
            .setItems(new String[]{
                "Changer le nom de l'accueil", mode, "Couleur des panneaux",
                "Nombre de colonnes", "Choisir le fond d'écran", "Définir comme accueil"
            }, (dialog, choice) -> {
                switch (choice) {
                    case 0: renameHome(); break;
                    case 1:
                        prefs.edit().putBoolean("dark", !prefs.getBoolean("dark", false)).apply();
                        buildHome();
                        break;
                    case 2: chooseAccent(); break;
                    case 3:
                        new AlertDialog.Builder(this).setTitle("Colonnes")
                            .setSingleChoiceItems(new String[]{"3", "4", "5"}, columns - 3,
                                (d, which) -> {
                                    prefs.edit().putInt("columns", which + 3).apply();
                                    d.dismiss();
                                    buildHome();
                                }).show();
                        break;
                    case 4:
                        safeStart(new Intent(Intent.ACTION_SET_WALLPAPER)); break;
                    case 5: chooseHome(); break;
                    default: break;
                }
            }).show();
    }

    private void renameHome() {
        EditText input = new EditText(this);
        input.setText(prefs.getString("title", "Chez Dabou"));
        input.setSingleLine(true);
        input.setFilters(new android.text.InputFilter[]{
            new android.text.InputFilter.LengthFilter(28)
        });
        new AlertDialog.Builder(this).setTitle("Nom de l'accueil").setView(input)
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Enregistrer", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    prefs.edit().putString("title", name).apply();
                    buildHome();
                }
            }).show();
    }

    private void chooseAccent() {
        String[] names = {"Feuille", "Abricot", "Lavande", "Océan"};
        int[] colors = {
            Color.parseColor("#648C16"), Color.parseColor("#FF9C4C"),
            Color.parseColor("#AA8BC5"), Color.parseColor("#459AAB")
        };
        new AlertDialog.Builder(this).setTitle("Couleur des panneaux")
            .setItems(names, (dialog, index) -> {
                prefs.edit().putInt("accent", colors[index]).apply();
                buildHome();
            }).show();
    }

    private void chooseHome() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager manager = getSystemService(RoleManager.class);
            if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_HOME)
                    && !manager.isRoleHeld(RoleManager.ROLE_HOME)) {
                safeStart(manager.createRequestRoleIntent(RoleManager.ROLE_HOME));
                return;
            }
        }
        safeStart(new Intent(Settings.ACTION_HOME_SETTINGS));
    }

    @Override public void onBackPressed() {
        if (search != null && search.length() > 0) search.setText("");
        else if (search != null) search.clearFocus();
    }
}
