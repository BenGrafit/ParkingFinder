package com.example.parkingfinder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ParkingSpotAdapter extends RecyclerView.Adapter<ParkingSpotAdapter.VH> {

    public interface OnParkingSpotAction {
        void onClick(ParkingSpot spot);
        void onLongClick(ParkingSpot spot);
    }

    private List<ParkingSpot> data = new ArrayList<>();
    private final OnParkingSpotAction action;

    public ParkingSpotAdapter(OnParkingSpotAction action) {
        this.action = action;
    }

    public void submitList(List<ParkingSpot> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parking_spot, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ParkingSpot spot = data.get(pos);

        h.txtStatus.setText(spot.isEmpty() ? "פנוי" : "תפוס");
        h.txtCoordinates.setText(String.format("Lat: %.5f, Lon: %.5f", spot.getX(), spot.getY()));
        h.chkEmpty.setChecked(spot.isEmpty());
        
        // Disable checkbox interaction if it's just for display, or handle it
        h.chkEmpty.setEnabled(false);

        h.itemView.setOnClickListener(v -> action.onClick(spot));
        h.itemView.setOnLongClickListener(v -> {
            action.onLongClick(spot);
            return true;
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtStatus, txtCoordinates;
        CheckBox chkEmpty;

        VH(@NonNull View itemView) {
            super(itemView);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtCoordinates = itemView.findViewById(R.id.txtCoordinates);
            chkEmpty = itemView.findViewById(R.id.chkEmpty);
        }
    }
}
