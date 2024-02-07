package com.live2d.hx030;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.live2d.hx030.component.ModelAdapter;
import com.live2d.wp.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipInputStream;

public class CustomModelsActivity extends AppCompatActivity {
    private static final int REQ_READ_MODEL = 88;
    public static List<String> imported_models;

    private RecyclerView model_list;
    private ModelAdapter model_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_models);

        Log.d("030-mod", "onCreate");

        Button btn_import = findViewById(R.id.btn_import);
        if (btn_import != null) {
            btn_import.setOnClickListener((view) -> {
                Log.d("030-mod", "select");
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                startActivityForResult(intent, REQ_READ_MODEL);
            });
        }

        Button btn_download = findViewById(R.id.btn_download);
        if (btn_download != null) {
            btn_download.setOnClickListener((view) -> {
                Toast.makeText(this, R.string.not_implemented, Toast.LENGTH_LONG).show();
            });
        }

        Log.d("030-mod-p", getFilesDir().getAbsolutePath());

        model_list = findViewById(R.id.model_list);
        model_adapter = new ModelAdapter();
        model_list.setLayoutManager(new LinearLayoutManager(this));
        model_list.setAdapter(model_adapter);
        model_list.addItemDecoration(new DividerItemDecoration(model_list.getContext(), DividerItemDecoration.VERTICAL));
        model_adapter.notifyDataSetChanged();
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
            Log.e("030-set", "error getting data");
            return;
        }
        Log.d("030-?", path.toString());
        String path_str = path.getPath();
        if (path_str != null) {
            Log.d("030-bg", path_str);
        }

        try {
            InputStream is = getContentResolver().openInputStream(path);
            if (requestCode == REQ_READ_MODEL) {
                String model_path = String.format("%s/custom_models/_tmp", getFilesDir().getAbsolutePath());
                File model_dir = new File(model_path);
                if (model_dir.exists() && !model_dir.delete()) throw new IOException("Failed to remove existing temp dir");;
                if (!model_dir.mkdirs()) throw new IOException("Failed to create dir for custom model");

                String model_name = IoHelper.unzip(new ZipInputStream(is), model_path);
                if (!model_dir.renameTo(new File(model_path.replace("_tmp", model_name)))) {
                    throw new IOException("Failed to rename dir for " + model_name);
                }

                imported_models.add(model_name);
                model_adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.file_select_err, Toast.LENGTH_LONG).show();
            Log.e("030-zip", String.format("%s - %s", e.getMessage(), Arrays.toString(e.getStackTrace())));
        }
    }
}