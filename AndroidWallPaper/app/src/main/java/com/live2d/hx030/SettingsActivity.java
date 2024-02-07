package com.live2d.hx030;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.live2d.wp.JniBridgeJava;
import com.live2d.wp.LiveWallpaperService;
import com.live2d.wp.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipInputStream;

public class SettingsActivity extends AppCompatActivity {
    private static File customModelDir;
    private static Context mContext;

    protected static SharedPreferences prefs;
    protected static WallpaperParams params;

    private static int REQ_READ_IMAGE = 99;
    private static int REQ_READ_MODEL = 88;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        prefs = getSharedPreferences("com.live2d.wp_preferences", Context.MODE_PRIVATE);
        params = new WallpaperParams(getApplicationContext());
        mContext = getApplicationContext();

        customModelDir = new File(getFilesDir().getAbsolutePath() + "/custom_models");
        if (!customModelDir.exists()) {
            if (!customModelDir.mkdirs()) {
                Log.e("030-custom", "Failed to create folder");
            }
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (customModelDir != null && customModelDir.exists()) {
            if (CustomModelsActivity.imported_models == null)
                CustomModelsActivity.imported_models = new ArrayList<>();

            File[] children = customModelDir.listFiles();
            if (children != null) {
                CustomModelsActivity.imported_models.clear();
                CustomModelsActivity.imported_models.add("");
                for (File child : children) {
                    if (child.isFile()) continue;
                    Log.d("030-model", child.getName());
                    CustomModelsActivity.imported_models.add(child.getName());
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("030-res", String.format("%d %d %s", requestCode, resultCode, (data != null)));
//        if (requestCode != 0) return; // ????
        if (resultCode != RESULT_OK || data == null) {
            Log.e("030-set", "error getting file data");
            Toast.makeText(this, R.string.file_select_err, Toast.LENGTH_LONG).show();
            return;
        }

        Uri path = data.getData();
        if (path == null) {
            Log.e("030-set", "error getting bg data");
            return;
        }
        String path_str = path.getPath();
        if (path_str != null) {
            Log.d("030-bg", path_str);
        }

        try {
            InputStream is = getContentResolver().openInputStream(path);
            if (requestCode == REQ_READ_IMAGE) {
                Log.d("030-bg", String.format("save to %s/back.png", getFilesDir().getAbsolutePath()));
                File target = new File(String.format("%s/back.png", getFilesDir().getAbsolutePath()));
                OutputStream out = new FileOutputStream(target);

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.flush();
                out.close();

                prefs.edit().putBoolean("custom_background", true).apply();
            }
            if (is != null) is.close();
        } catch (Exception e) {
            Toast.makeText(this, R.string.file_select_err, Toast.LENGTH_LONG).show();
            Log.e("030-bg", String.format("%s - %s", e.getMessage(), Arrays.toString(e.getStackTrace())));
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private SwitchPreferenceCompat def_touch_interaction;
        private SwitchPreferenceCompat loop_idle_motion;

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            Log.d("030-fres", String.format("%d %d %s", requestCode, resultCode, (data != null)));
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.general_preferences, rootKey);

            // custom background
            Preference bg_picker = findPreference("custom_background");
            if (bg_picker != null) {
                bg_picker.setOnPreferenceClickListener(preference -> {
                    SwitchPreferenceCompat toggle = (SwitchPreferenceCompat) preference;
                    if (toggle.isChecked()) {
                        readImage();
                    } else {
                        prefs.edit().putBoolean("custom_background", false).apply();
                    }
                    return true;
                });
            }
            // custom model
            Preference external_model = findPreference("external_model");
            if (external_model != null) {
                external_model.setOnPreferenceClickListener(preference -> {
                    Intent openCMA = new Intent(getActivity(), CustomModelsActivity.class);
                    startActivity(openCMA);
                    return true;
                });
            }

            Preference restart = findPreference("restart");
            if (restart != null) {
                restart.setOnPreferenceClickListener(preference -> {
                    Context context = getActivity();
                    Intent i = new Intent();
                    i.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);

                    String p = LiveWallpaperService.class.getPackage().getName();
                    String c = LiveWallpaperService.class.getCanonicalName();
                    i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(p, c));
                    context.startActivity(i);
                    return true;
                });
            }

            String[] pref_keys = {"default_touch_interaction", "loop_idle_motion", "use_background", "custom_background", "sensor", "model_scale", "x_offset", "y_offset", "model"};
            Preference.OnPreferenceChangeListener prefChanged = (pref, v) -> {
                params.SetParams();
                return true;
            };

            for (String pk : pref_keys) {
                Preference p = findPreference(pk);
                if (p == null) {
                    Log.w("030-set", "unable to find " + pk);
                    continue;
                }

                if (def_touch_interaction == null && pk.equals("default_touch_interaction")) {
                    def_touch_interaction = (SwitchPreferenceCompat) p;
                }

                if (loop_idle_motion == null && pk.equals("loop_idle_motion")) {
                    loop_idle_motion = (SwitchPreferenceCompat) p;
                    loop_idle_motion.setOnPreferenceChangeListener((_p, v) -> {
                        SwitchPreferenceCompat pref = (SwitchPreferenceCompat) _p;
                        if (!pref.isChecked()) {
                            if (def_touch_interaction == null)
                                def_touch_interaction = (SwitchPreferenceCompat) findPreference("default_touch_interaction");

                            def_touch_interaction.setChecked(false);
                        }
                        prefChanged.onPreferenceChange(p, v);
                        return true;
                    });
                } else {
                    p.setOnPreferenceChangeListener(prefChanged);
                }

                try {
                    if (pk.equals("model")) {
                        ListPreference lp = (ListPreference) p;
                        CharSequence[] model_names = lp.getEntries();
                        CharSequence[] model_paths = lp.getEntryValues();
                        List<CharSequence> new_names = new ArrayList<>();
                        List<CharSequence> new_paths = new ArrayList<>();

                        AssetManager am = getContext().getAssets();
                        for (int i = 0; i < model_names.length; i++) {
                            try {
                                InputStream is = am.open(String.format("%s/%s.model3.json", model_paths[i], model_paths[i]));
                                if (is.available() > 0) {
                                    new_names.add(model_names[i]);
                                    new_paths.add(model_paths[i]);
                                }
                            } catch (Exception ignored) {}
                        }

                        CharSequence[] names = new CharSequence[new_names.size()];
                        CharSequence[] paths = new CharSequence[new_paths.size()];
                        new_names.toArray(names);
                        new_paths.toArray(paths);
                        if (new_paths.size() == 0) {
                            lp.setVisible(false);
                            return;
                        } else if (!new_paths.contains(lp.getValue())) {
                            Log.d("030-model", String.format("%s doesn't exist, resetting to %s", lp.getValue(), new_paths.get(0)));
                            lp.setValue(String.valueOf(new_paths.get(0)));
                        }
                        lp.setEntries(names);
                        lp.setEntryValues(paths);
                    }
                } catch (Exception e) {
                    Log.e("030-trim", "error trimming options", e);
                }
            }
        }

        private void readImage() {
            Log.d("030-v", "readfile");
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            String[] mimetypes = {"image/jpeg", "image/png", "image/bmp"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            getActivity().startActivityForResult(intent, REQ_READ_IMAGE);
        }

        private void readZip() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            getActivity().startActivityForResult(intent, REQ_READ_MODEL);
        }

    }

    public static void setExternalModel(String name) {
        prefs.edit().putString("external_model_name", name).putBoolean("external_model", name.length() > 0).apply();
        params.updateValues();
    }

    public static void delExternalModel(String name) {
        File target = new File(String.format("%s/custom_models/%s", mContext.getFilesDir().getAbsolutePath(), name));
        try {
            if (!IoHelper.delete(target)) {
                Toast.makeText(mContext, R.string.err_delete_model, Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception ex) {
            Toast.makeText(mContext, R.string.err_delete_model, Toast.LENGTH_LONG).show();
            Log.e("030-mod-del", "failed to delete model", ex);
            return;
        }
        prefs.edit().putString("external_model_name", "").putBoolean("external_model", false).apply();
        CustomModelsActivity.imported_models.remove(name);
        params.updateValues();
    }

    private static class WallpaperParams {
        private static final String def_model = "kanade_normal_0101";
        private String model = def_model;
        private boolean external_model = false;
        private String external_model_name = "";
        private boolean sensor = false;
        private boolean touch_interaction = true;
        private boolean loop_idle_motion = false;
        private boolean use_background = false;
        private boolean custom_background = false;
        private boolean no_reset = false;
        private int x_offset = 0;
        private int y_offset = 0;
        private int model_scale = 100;

        private File dir;

        public WallpaperParams(Context ctx) {
            dir = ctx.getFilesDir();
            updateValues();
        }

        private int safeParsePrefInt(String key, int def) {
            String defStr = String.format("%d", def);
            try {
                return Integer.parseInt(Objects.requireNonNull(prefs.getString(key, defStr)));
            } catch (Exception ignored) {
                prefs.edit().putString(key, defStr).apply();
                return def;
            }
        }

        public void updateValues() {
            String model_new = prefs.getString("model", def_model);
            File m = new File(String.format("%s/model", dir.getAbsolutePath()));
            sensor = prefs.getBoolean("sensor", sensor);
            touch_interaction = prefs.getBoolean("default_touch_interaction", touch_interaction);
            loop_idle_motion = prefs.getBoolean("loop_idle_motion", loop_idle_motion);
            no_reset = prefs.getBoolean("no_reset", no_reset);
            use_background = prefs.getBoolean("use_background", use_background);
            custom_background = prefs.getBoolean("custom_background", custom_background);
            external_model = prefs.getBoolean("external_model", external_model);
            external_model_name = prefs.getString("external_model_name", "");
            x_offset = safeParsePrefInt("x_offset", 0);
            y_offset = safeParsePrefInt("y_offset", 0);
            model_scale = safeParsePrefInt("model_scale", 100);

            if (LiveWallpaperService.forceLoopIdle.contains(model)) {
                loop_idle_motion = true;
            }

            if ((loop_idle_motion || no_reset) && touch_interaction) {
                touch_interaction = false;
                prefs.edit().putBoolean("touch_interaction", touch_interaction).apply();
            }

            if (!custom_background) {
                File bg = new File(String.format("%s/back.png", dir.getAbsolutePath()));
                bg.delete();
            }

            if (external_model) {
                model_new = "custom_models/" + external_model_name;
            }
            Log.d("030-p", model_new);

            if (!model.equals(model_new) || !m.exists()) {
                model = model_new;
                try {
                    FileOutputStream os = new FileOutputStream(m, false);
                    os.write(model.getBytes());
                    os.write(0);
                    os.close();
                    Log.d("030?", String.format("%s - %s", m.getAbsolutePath(), model));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            Log.d("030-set", String.format("loop_idle: %s, touch: %s", loop_idle_motion, touch_interaction));
        }

        public void SetParams_real() {
            updateValues();
            LiveWallpaperService svc = LiveWallpaperService.getInstance();
            if (svc == null) return;

            Log.d("030-set", String.format("sensor: %s, touch: %s", sensor, touch_interaction));
            if (svc.getEngine() != null && svc.getEngine().renderer != null) {
                svc.getEngine().renderer.setUseSensor(sensor);
            }
            JniBridgeJava.SetParam(model, loop_idle_motion, use_background, custom_background, touch_interaction, no_reset, model_scale, x_offset, y_offset);
        }

        public void SetParams() {
            new SetParamThread().start();
        }

        private class SetParamThread extends Thread {
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.d("030-sett", String.format("%s: %s", e.getMessage(), Arrays.toString(e.getStackTrace())));
                }
                SetParams_real();
            }
        }
    }
}