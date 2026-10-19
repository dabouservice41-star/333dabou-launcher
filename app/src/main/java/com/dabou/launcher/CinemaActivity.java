package com.dabou.launcher;

/*
 * Native Android launcher for Dabou. Film-poster design: condensed headlines,
 * monospaced clock, charcoal #141519, ivory #F9F4ED, signal red #C30F27.
 * Signature: a curved red field, like a sweeping poster composition.
 * Layout: day / time / portrait + tools / search / favorites / applications.
 * The user chooses their own portrait; no extracted film artwork is included.
 */

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.provider.CalendarContract;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.InputStream;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.*;

public class CinemaActivity extends Activity {
    private static final int PHOTO = 41, AUDIO = 42;
    private static final int INK = Color.rgb(20, 21, 25);
    private static final int WHITE = Color.rgb(249, 244, 237);
    private static final int MUTED = Color.rgb(222, 212, 207);
    private final List<App> apps = new ArrayList<>();
    private final Calendar month = Calendar.getInstance();
    private SharedPreferences prefs;
    private Set<String> favorites;
    private LinearLayout tools, favoritesBox;
    private GridLayout appGrid;
    private TextView appCount, playButton;
    private EditText search;
    private MediaPlayer player;
    private String activeTool = "info";
    private int accent, columns;
    private boolean preparingAudio;
    private ScrollView scroll;

    private static class App {
        final String label;
        final ResolveInfo info;
        final ComponentName component;
        App(ResolveInfo r, String name) {
            info = r; label = name;
            component = new ComponentName(r.activityInfo.packageName, r.activityInfo.name);
        }
        String key() { return component.flattenToString(); }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("cinema", MODE_PRIVATE);
        buildHome();
    }
    @Override protected void onResume() {
        super.onResume();
        loadApps(); renderApps();
        if (tools != null && activeTool.equals("info")) renderTool();
    }
    @Override protected void onStop() { releasePlayer(); super.onStop(); }
    @Override protected void onDestroy() { releasePlayer(); super.onDestroy(); }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if (search != null) {
            search.setText(""); search.clearFocus();
            android.view.inputmethod.InputMethodManager keyboard =
                (android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(search.getWindowToken(), 0);
        }
        if (scroll != null) scroll.smoothScrollTo(0, 0);
    }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private TextView headline(String s, int size) {
        TextView t = text(s, size, WHITE);
        t.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD)); return t;
    }
    private TextView button(String s, Runnable action) {
        TextView t = headline(s, 16);
        t.setTextColor(INK); t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(10), dp(8), dp(10));
        t.setMinHeight(dp(48)); t.setBackground(shape(WHITE, 3));
        t.setOnClickListener(v -> action.run()); t.setFocusable(true); return t;
    }
    private void buildHome() {
        releasePlayer();
        accent = prefs.getInt("accent", Color.rgb(195, 15, 39));
        columns = prefs.getInt("columns", 4);
        favorites = new HashSet<>(prefs.getStringSet("favorites", new HashSet<>()));
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(INK);
        root.addView(new Backdrop(), new FrameLayout.LayoutParams(-1, -1));
        scroll = new ScrollView(this); scroll.setFillViewport(true);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), dp(24));
        content.setFocusableInTouchMode(true);
        scroll.addView(content);
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        root.requestApplyInsets();
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = headline(prefs.getString("title", "DABOU / HOME"), 17);
        title.setLetterSpacing(.10f);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView settings = text("RÉGLAGES", 14, WHITE);
        settings.setPadding(dp(8), dp(14), 0, dp(14));
        settings.setMinHeight(dp(48)); settings.setOnClickListener(v -> settings());
        top.addView(settings); content.addView(top);
        TextClock day = new TextClock(this);
        day.setFormat12Hour("EEEE"); day.setFormat24Hour("EEEE"); day.setAllCaps(true);
        day.setTextColor(WHITE); day.setTextSize(52);
        day.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        day.setSingleLine(true);
        day.setAutoSizeTextTypeUniformWithConfiguration(26, 52, 1,
            android.util.TypedValue.COMPLEX_UNIT_SP);
        content.addView(day, new LinearLayout.LayoutParams(-1, dp(70)));
        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextClock date = new TextClock(this);
        date.setFormat12Hour("dd MMM · yyyy"); date.setFormat24Hour("dd MMM · yyyy");
        date.setTextColor(MUTED); date.setTextSize(16);
        timeRow.addView(date, new LinearLayout.LayoutParams(0, -2, 1));
        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm a"); clock.setFormat24Hour("HH:mm");
        clock.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        clock.setTextColor(INK); clock.setTextSize(23);
        clock.setPadding(dp(8), dp(3), dp(8), dp(3)); clock.setBackground(shape(WHITE, 2));
        timeRow.addView(clock); content.addView(timeRow);

        LinearLayout stage = new LinearLayout(this);
        LinearLayout.LayoutParams stageParams = new LinearLayout.LayoutParams(-1, -2);
        stageParams.setMargins(0, dp(26), 0, dp(18)); content.addView(stage, stageParams);
        FrameLayout portrait = new FrameLayout(this);
        LinearLayout.LayoutParams portraitParams = new LinearLayout.LayoutParams(0, dp(350), .46f);
        portraitParams.rightMargin = dp(14); stage.addView(portrait, portraitParams);
        Bitmap bitmap = loadPortrait();
        if (bitmap != null) {
            ImageView image = new ImageView(this); image.setImageBitmap(bitmap);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setContentDescription("Ton image personnalisée");
            portrait.addView(image, new FrameLayout.LayoutParams(-1, -1));
        } else {
            TextView placeholder = headline("TON\nUNIVERS.", 37);
            placeholder.setGravity(Gravity.BOTTOM);
            placeholder.setPadding(0, 0, 0, dp(52));
            portrait.addView(placeholder, new FrameLayout.LayoutParams(-1, -1));
            TextView tip = text("+ TON IMAGE", 14, WHITE);
            tip.setGravity(Gravity.BOTTOM); tip.setPadding(0, 0, 0, dp(12));
            portrait.addView(tip, new FrameLayout.LayoutParams(-1, -1));
        }
        portrait.setOnClickListener(v -> pick("image/*", PHOTO));
        portrait.setContentDescription("Choisir une image ou un personnage");
        portrait.setFocusable(true);
        LinearLayout right = new LinearLayout(this); right.setOrientation(LinearLayout.VERTICAL);
        stage.addView(right, new LinearLayout.LayoutParams(0, -2, .54f));
        tools = new LinearLayout(this); tools.setOrientation(LinearLayout.VERTICAL);
        tools.setPadding(dp(8), dp(10), dp(8), dp(10));
        tools.setBackground(shape(Color.argb(135, 20, 21, 25), 4));
        right.addView(tools, new LinearLayout.LayoutParams(-1, -2));
        String[] labels = {"INFOS", "CALENDRIER", "MUSIQUE"};
        String[] ids = {"info", "calendar", "music"};
        for (int n = 0; n < ids.length; n++) {
            final String id = ids[n];
            TextView action = button(labels[n], () -> { activeTool = id; renderTool(); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.topMargin = dp(8); right.addView(action, p);
        }
        renderTool();
        search = new EditText(this); search.setSingleLine(true);
        search.setTextColor(WHITE); search.setHintTextColor(MUTED); search.setTextSize(16);
        search.setHint("Rechercher une application");
        search.setContentDescription("Rechercher une application");
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setPadding(dp(16), dp(12), dp(16), dp(12));
        search.setBackground(shape(Color.argb(210, 20, 21, 25), 6));
        content.addView(search, new LinearLayout.LayoutParams(-1, dp(52)));
        favoritesBox = new LinearLayout(this); favoritesBox.setOrientation(LinearLayout.VERTICAL);
        content.addView(favoritesBox);
        appCount = text("", 14, WHITE); appCount.setPadding(0, dp(18), 0, dp(12));
        content.addView(appCount);
        appGrid = new GridLayout(this); appGrid.setColumnCount(columns);
        content.addView(appGrid, new LinearLayout.LayoutParams(-1, -2));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int f) {}
            @Override public void afterTextChanged(Editable e) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { renderApps(); }
        });
        content.requestFocus(); loadApps(); renderApps();
    }
    private class Backdrop extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Backdrop() { super(CinemaActivity.this); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight(); canvas.drawColor(INK);
            Path curve = new Path();
            curve.moveTo(0, h * .54f);
            curve.cubicTo(w * .34f, h * .28f, w * .55f, h * .32f, w, h * .25f);
            curve.lineTo(w, h); curve.lineTo(0, h); curve.close();
            paint.setColor(accent); canvas.drawPath(curve, paint);
        }
    }
    private void renderTool() {
        tools.removeAllViews();
        if (activeTool.equals("calendar")) calendar();
        else if (activeTool.equals("music")) music();
        else info();
    }
    private void toolTitle(String label) {
        TextView t = headline(label, 18); t.setPadding(0, 0, 0, dp(10)); tools.addView(t);
    }
    private void infoLine(String label, String value) {
        TextView t = text(label + "\n" + value, 14, WHITE);
        t.setPadding(0, 0, 0, dp(10)); tools.addView(t);
    }
    private String gb(long value) {
        return String.format(Locale.getDefault(), "%.1f Go", value / 1073741824.0);
    }
    private void info() {
        toolTitle("DEVICE INFO"); infoLine("APPAREIL", Build.MODEL);
        infoLine("ANDROID", Build.VERSION.RELEASE);
        ActivityManager manager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        if (manager != null) {
            ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            manager.getMemoryInfo(memory);
            infoLine("RAM LIBRE", gb(memory.availMem) + " / " + gb(memory.totalMem));
        }
        StatFs storage = new StatFs(Environment.getDataDirectory().getPath());
        infoLine("STOCKAGE LIBRE", gb(storage.getAvailableBytes()));
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = battery == null ? -1 : battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery == null ? -1 : battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        infoLine("BATTERIE", level >= 0 && scale > 0 ? (100 * level / scale) + " %" : "Indisponible");
    }
    private void calendar() {
        toolTitle(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.getTime()));
        LinearLayout nav = new LinearLayout(this);
        nav.addView(button("‹", () -> { month.add(Calendar.MONTH, -1); renderTool(); }),
            new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams next = new LinearLayout.LayoutParams(0, dp(48), 1);
        next.leftMargin = dp(8);
        nav.addView(button("›", () -> { month.add(Calendar.MONTH, 1); renderTool(); }), next);
        tools.addView(nav);
        // Single custom-painted grid keeps seven date columns aligned in a narrow panel.
        tools.addView(new MonthGrid(), new LinearLayout.LayoutParams(-1, dp(210)));
        TextView open = text("Ouvrir l'agenda ↗", 14, WHITE);
        open.setGravity(Gravity.CENTER_VERTICAL); open.setMinHeight(dp(48));
        open.setOnClickListener(v -> safeStart(new Intent(Intent.ACTION_VIEW,
            Uri.withAppendedPath(CalendarContract.CONTENT_URI, "time/" + month.getTimeInMillis()))));
        tools.addView(open);
    }
    private class MonthGrid extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        MonthGrid() {
            super(CinemaActivity.this);
            setContentDescription("Calendrier de " + new SimpleDateFormat("MMMM yyyy",
                Locale.getDefault()).format(month.getTime())
                + ". Utiliser Ouvrir l'agenda pour consulter les dates et événements.");
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            Calendar first = (Calendar)month.clone(); first.set(Calendar.DAY_OF_MONTH, 1);
            int start = first.getFirstDayOfWeek();
            int offset = (first.get(Calendar.DAY_OF_WEEK) - start + 7) % 7;
            int total = first.getActualMaximum(Calendar.DAY_OF_MONTH);
            float cell = getWidth() / 7f, row = getHeight() / 7f;
            p.setTextAlign(Paint.Align.CENTER);
            // Condensed 14sp numerals fit seven columns on typical phone widths.
            p.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
            p.setTextSize(14 * getResources().getDisplayMetrics().scaledDensity);
            String[] weekdays = new java.text.DateFormatSymbols().getShortWeekdays();
            p.setColor(MUTED);
            for (int n = 0; n < 7; n++) {
                String label = weekdays[(start - 1 + n) % 7 + 1].substring(0, 1);
                c.drawText(label, cell * (n + .5f), row * .8f, p);
            }
            Calendar today = Calendar.getInstance();
            for (int day = 1; day <= total; day++) {
                int slot = offset + day - 1, col = slot % 7, line = slot / 7 + 1;
                float x = cell * (col + .5f), y = row * (line + .8f);
                boolean current = month.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && month.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                    && day == today.get(Calendar.DAY_OF_MONTH);
                if (current) {
                    p.setColor(WHITE);
                    c.drawCircle(x, y - dp(5), Math.min(cell * .48f, dp(13)), p);
                }
                p.setColor(current ? INK : WHITE);
                c.drawText(String.valueOf(day), x, y, p);
            }
        }
    }
    private void music() {
        toolTitle("MUSIQUE");
        TextView title = text(prefs.getString("audioName", "Choisis un fichier audio local."), 14, WHITE);
        title.setMaxLines(3); title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(0, 0, 0, dp(12)); tools.addView(title);
        playButton = button(preparingAudio ? "CHARGEMENT…" : isPlaying() ? "PAUSE" : "LIRE",
            this::toggleAudio);
        tools.addView(playButton, new LinearLayout.LayoutParams(-1, -2));
        TextView choose = text("Choisir un morceau ↗", 14, WHITE);
        choose.setMinHeight(dp(48)); choose.setGravity(Gravity.CENTER_VERTICAL);
        choose.setOnClickListener(v -> pick("audio/*", AUDIO)); tools.addView(choose);
        tools.addView(text("Arrêt en quittant l'accueil.", 14, MUTED));
    }
    private boolean isPlaying() {
        if (player == null || preparingAudio) return false;
        try { return player.isPlaying(); } catch (IllegalStateException e) { return false; }
    }
    private void toggleAudio() {
        String saved = prefs.getString("audio", "");
        if (saved.isEmpty()) { pick("audio/*", AUDIO); return; }
        if (preparingAudio) return;
        if (player != null) {
            try {
                if (player.isPlaying()) player.pause(); else player.start();
                renderTool();
            } catch (IllegalStateException e) { audioError(); }
            return;
        }
        try {
            MediaPlayer mp = new MediaPlayer(); player = mp;
            mp.setAudioAttributes(new android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build());
            mp.setDataSource(this, Uri.parse(saved)); preparingAudio = true;
            mp.setOnPreparedListener(ready -> {
                if (player != ready) return;
                preparingAudio = false; ready.start();
                if (activeTool.equals("music")) renderTool();
            });
            mp.setOnCompletionListener(done -> {
                if (activeTool.equals("music")) renderTool();
            });
            mp.setOnErrorListener((broken, what, extra) -> { audioError(); return true; });
            mp.prepareAsync(); renderTool();
        } catch (Exception e) { audioError(); }
    }
    private void audioError() {
        releasePlayer();
        Toast.makeText(this, "Audio inaccessible ou format non pris en charge.", Toast.LENGTH_LONG).show();
        if (tools != null && activeTool.equals("music")) renderTool();
    }
    private void releasePlayer() {
        if (player != null) { player.release(); player = null; }
        preparingAudio = false;
        if (playButton != null) playButton.setText("LIRE");
    }
    @SuppressWarnings("deprecation")
    private void pick(String type, int request) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(intent, request); }
        catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Aucun sélecteur de fichiers disponible.", Toast.LENGTH_LONG).show();
        }
    }
    @Override @SuppressWarnings("deprecation")
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (request != PHOTO && request != AUDIO) return;
        Uri uri = data.getData(); String key = request == PHOTO ? "photo" : "audio";
        String old = prefs.getString(key, "");
        try {
            getContentResolver().takePersistableUriPermission(uri,
                data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            Toast.makeText(this, "Ce fichier pourrait devoir être choisi à nouveau au redémarrage.",
                Toast.LENGTH_LONG).show();
        }
        prefs.edit().putString(key, uri.toString()).apply();
        String other = request == PHOTO ? "audio" : "photo";
        if (!old.isEmpty() && !old.equals(uri.toString()) && !old.equals(prefs.getString(other, ""))) {
            try {
                getContentResolver().releasePersistableUriPermission(Uri.parse(old),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {}
        }
        if (request == AUDIO) {
            String name = "Fichier audio";
            try (Cursor cursor = getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
            } catch (Exception ignored) {}
            prefs.edit().putString("audioName", name).apply();
            releasePlayer(); activeTool = "music"; renderTool();
        } else buildHome();
    }
    private Bitmap loadPortrait() {
        String value = prefs.getString("photo", ""); if (value.isEmpty()) return null;
        try {
            Uri uri = Uri.parse(value);
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(stream, null, options);
            }
            options.inSampleSize = 1;
            while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 1600)
                options.inSampleSize *= 2;
            options.inJustDecodeBounds = false;
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(stream, null, options);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Image inaccessible. Choisis une autre image.", Toast.LENGTH_LONG).show();
            return null;
        }
    }
    @SuppressWarnings("deprecation")
    private void loadApps() {
        apps.clear(); Set<String> seen = new HashSet<>();
        List<ResolveInfo> resolved = getPackageManager().queryIntentActivities(
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        for (ResolveInfo r : resolved) {
            if (!r.activityInfo.exported || r.activityInfo.packageName.equals(getPackageName())) continue;
            App app = new App(r, r.loadLabel(getPackageManager()).toString());
            if (seen.add(app.key())) apps.add(app);
        }
        Collator collator = Collator.getInstance();
        apps.sort((a, b) -> collator.compare(a.label, b.label));
    }
    private void renderApps() {
        if (appGrid == null || search == null) return;
        String query = search.getText().toString().trim().toLowerCase(Locale.getDefault());
        appGrid.removeAllViews(); favoritesBox.removeAllViews();
        boolean has = false; for (App a : apps) if (favorites.contains(a.key())) has = true;
        favoritesBox.setVisibility(has && query.isEmpty() ? View.VISIBLE : View.GONE);
        if (has && query.isEmpty()) {
            TextView label = headline("FAVORIS", 16); label.setPadding(0, dp(18), 0, dp(10));
            favoritesBox.addView(label);
            GridLayout grid = new GridLayout(this); grid.setColumnCount(columns);
            favoritesBox.addView(grid, new LinearLayout.LayoutParams(-1, -2));
            for (App a : apps) if (favorites.contains(a.key())) tile(grid, a);
        }
        int count = 0;
        for (App a : apps) if (a.label.toLowerCase(Locale.getDefault()).contains(query)) {
            tile(appGrid, a); count++;
        }
        appCount.setText(count == 0 ? "Aucune application trouvée" : "APPLICATIONS · " + count);
    }
    private void tile(GridLayout grid, App app) {
        LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(3), dp(8), dp(3), dp(8));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0; params.height = dp(112);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        grid.addView(tile, params);
        ImageView icon = new ImageView(this); icon.setImageDrawable(app.info.loadIcon(getPackageManager()));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView label = text(app.label, 14, WHITE); label.setGravity(Gravity.CENTER);
        label.setMaxLines(2); label.setEllipsize(TextUtils.TruncateAt.END);
        label.setPadding(0, dp(6), 0, 0); tile.addView(label, new LinearLayout.LayoutParams(-1, -2));
        tile.setContentDescription(app.label + ". Appui long pour les options."); tile.setFocusable(true);
        tile.setOnClickListener(v -> safeStart(new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)));
        tile.setOnLongClickListener(v -> {
            boolean favorite = favorites.contains(app.key());
            new AlertDialog.Builder(this).setTitle(app.label)
                .setItems(new String[]{favorite ? "Retirer des favoris" : "Ajouter aux favoris",
                    "Informations de l'application"}, (dialog, index) -> {
                    if (index == 0) {
                        if (favorite) favorites.remove(app.key()); else favorites.add(app.key());
                        prefs.edit().putStringSet("favorites", new HashSet<>(favorites)).apply(); renderApps();
                    } else safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + app.component.getPackageName())));
                }).show();
            return true;
        });
    }
    private void settings() {
        new AlertDialog.Builder(this).setTitle("Personnalisation")
            .setItems(new String[]{"Choisir une image", "Nom de l'accueil", "Couleur",
                "Colonnes d'applications", "Définir comme accueil"}, (d, n) -> {
                switch (n) {
                    case 0: pick("image/*", PHOTO); break;
                    case 1: rename(); break;
                    case 2:
                        int[] colors = {Color.rgb(195, 15, 39), Color.rgb(137, 67, 9), Color.rgb(91, 43, 145)};
                        new AlertDialog.Builder(this).setTitle("Couleur")
                            .setItems(new String[]{"Rouge cinéma", "Ambre", "Violet"}, (dialog, i) -> {
                                prefs.edit().putInt("accent", colors[i]).apply(); buildHome();
                            }).show();
                        break;
                    case 3:
                        new AlertDialog.Builder(this).setTitle("Colonnes")
                            .setSingleChoiceItems(new String[]{"3", "4", "5"}, columns - 3, (dialog, i) -> {
                                prefs.edit().putInt("columns", i + 3).apply(); dialog.dismiss(); buildHome();
                            }).show();
                        break;
                    case 4: chooseHome(); break;
                    default: break;
                }
            }).show();
    }
    private void rename() {
        EditText input = new EditText(this); input.setSingleLine(true);
        input.setText(prefs.getString("title", "DABOU / HOME"));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(24)});
        new AlertDialog.Builder(this).setTitle("Nom de l'accueil").setView(input)
            .setNegativeButton("Annuler", null).setPositiveButton("Enregistrer", (d, n) -> {
                String value = input.getText().toString().trim();
                if (!value.isEmpty()) { prefs.edit().putString("title", value).apply(); buildHome(); }
            }).show();
    }
    private void chooseHome() {
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager manager = getSystemService(RoleManager.class);
            if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_HOME)
                    && !manager.isRoleHeld(RoleManager.ROLE_HOME)) {
                safeStart(manager.createRequestRoleIntent(RoleManager.ROLE_HOME)); return;
            }
        }
        safeStart(new Intent(Settings.ACTION_HOME_SETTINGS));
    }
    private void safeStart(Intent intent) {
        try { startActivity(intent); }
        catch (android.content.ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Aucune application compatible pour cette action.", Toast.LENGTH_LONG).show();
        }
    }
    @Override @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (search != null && search.length() > 0) search.setText("");
        else if (scroll != null) scroll.smoothScrollTo(0, 0);
    }
}
