package com.ecartes.rfid_demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ecartes.rfid_demo.model.RfidTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RfidTagAdapter extends RecyclerView.Adapter<RfidTagAdapter.ViewHolder> {

    private List<RfidTag> rfidTags;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RfidTag tag);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public RfidTagAdapter(List<RfidTag> rfidTags) {
        this.rfidTags = rfidTags;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rfid_tag, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RfidTag tag = rfidTags.get(position);
        holder.textTagId.setText("TAG ID: " + tag.getTagId() + " (Count: " + tag.getCount() + ")");
        holder.textTimestamp.setText(tag.getTimestamp());
        holder.textStatus.setText(tag.getStatus());
        
        // Set status color based on status
        if ("Found".equals(tag.getStatus())) {
            holder.textStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.success_green, null));
        } else if ("Not Found".equals(tag.getStatus())) {
            holder.textStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.danger_red, null));
        } else if ("Saved".equals(tag.getStatus())) {
            holder.textStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.success_green, null));
        } else if ("Discarded".equals(tag.getStatus())) {
            holder.textStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.danger_red, null));
            holder.textStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.warning_orange, null));
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(tag);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rfidTags.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTagId;
        TextView textTimestamp;
        TextView textStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTagId = itemView.findViewById(R.id.textTagId);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            textStatus = itemView.findViewById(R.id.textStatus);
        }
    }

    public void updateTags(List<RfidTag> newTags) {
        this.rfidTags = newTags;
        notifyDataSetChanged();
    }
}