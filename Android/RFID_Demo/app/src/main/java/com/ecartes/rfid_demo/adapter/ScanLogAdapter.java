package com.ecartes.rfid_demo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ecartes.rfid_demo.R;
import com.ecartes.rfid_demo.model.ScanLog;

import java.util.List;

import androidx.core.content.ContextCompat;

public class ScanLogAdapter extends RecyclerView.Adapter<ScanLogAdapter.ViewHolder> {

    private List<ScanLog> scanLogs;

    public ScanLogAdapter(List<ScanLog> scanLogs) {
        this.scanLogs = scanLogs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scan_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanLog scanLog = scanLogs.get(position);
        
        holder.textTagId.setText(scanLog.getTagId());
        holder.textLocation.setText("Location: " + scanLog.getLocation());
        
        String status = scanLog.getStatus();
        holder.textStatus.setText("Status: " + status);
        
        // Set text color based on status
        if (status != null && status.equalsIgnoreCase("active")) {
            holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success_green));
        } else if (status != null && status.equalsIgnoreCase("disable")) {
            holder.textStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.danger_red));
        } else {
            // Default color
            holder.textStatus.setTextColor(holder.itemView.getContext().getColor(R.color.colorOnSurface));
        }
    }

    @Override
    public int getItemCount() {
        return scanLogs.size();
    }

    public void updateScanLogs(List<ScanLog> newScanLogs) {
        this.scanLogs = newScanLogs;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTagId;
        TextView textLocation;
        TextView textStatus;

        ViewHolder(View itemView) {
            super(itemView);
            textTagId = itemView.findViewById(R.id.textTagId);
            textLocation = itemView.findViewById(R.id.textLocation);
            textStatus = itemView.findViewById(R.id.textStatus);
        }
    }
}