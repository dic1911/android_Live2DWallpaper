package com.live2d.hx030.component;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.live2d.hx030.CustomModelsActivity;
import com.live2d.hx030.SettingsActivity;
import com.live2d.wp.R;

import java.util.ArrayList;

public class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.EntryListHolder> {
    private int count = 0;

    private int btnWidth = -1;

    public class EntryListHolder extends RecyclerView.ViewHolder {
        final public int id;
        final public TextView entry;

        public EntryListHolder(View v) {
            super(v);
            id = (count++);
            entry = v.findViewById(R.id.txt_entry);
//            v.setOnClickListener(view -> {
//                if (BlacklistHandler.getInstance().removeEntry(entry.getText().toString()))
//                    notifyDataSetChanged();
//            });
        }
    }

    public ModelAdapter() {}

    @NonNull
    @Override
    public EntryListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_row, parent, false);
        return new EntryListHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull final EntryListHolder holder, final int position) {
        if (CustomModelsActivity.imported_models == null) return;
        holder.entry.setText(CustomModelsActivity.imported_models.get(position).length() == 0 ?
                "Builtin" : CustomModelsActivity.imported_models.get(position));

        ImageButton btn_delete = holder.itemView.findViewById(R.id.btn_delete);
        if (position > 0) {
            btn_delete.setOnClickListener((v) -> {
                String name = String.valueOf(holder.entry.getText());
                Log.d("030-mod", "deleting model " + name);
                SettingsActivity.delExternalModel(name);
                notifyDataSetChanged();
            });
        } else {
            btn_delete.setEnabled(false);
        }
        ImageButton btn_select = holder.itemView.findViewById(R.id.btn_select);
        if (position > 0) {
            btn_select.setOnClickListener((v) -> {
                String name = String.valueOf(holder.entry.getText());
                Log.d("030-mod", "selecting model " + name);
                SettingsActivity.setExternalModel(String.valueOf(holder.entry.getText()));
            });
        } else {
            btn_select.setOnClickListener((v) -> SettingsActivity.setExternalModel(""));
        }
    }

    @Override
    public int getItemCount() {
        return CustomModelsActivity.imported_models == null ?
                0 : CustomModelsActivity.imported_models.size();
    }
}
