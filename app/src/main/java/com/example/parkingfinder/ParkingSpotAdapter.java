package com.example.parkingfinder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ParkingSpotAdapter extends RecyclerView.Adapter<ParkingSpotAdapter.VH> {

    /**
     * ממשק להעברת פעולות מה-Adapter ל-Activity
     */
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
                .inflate(R.layout.item_parking_spot, parent, false); // XML מותאם
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ParkingSpot spot = data.get(pos);

        h.txtStatus.setText(spot.isEmpty() ? "פנוי" : "לא פנוי");
        h.txtCoordinates.setText(String.format("x: %.2f, y: %.2f", spot.getX(), spot.getY()));

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

        VH(@NonNull View itemView) {
            super(itemView);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtCoordinates = itemView.findViewById(R.id.txtCoordinates);
        }
    }
}
