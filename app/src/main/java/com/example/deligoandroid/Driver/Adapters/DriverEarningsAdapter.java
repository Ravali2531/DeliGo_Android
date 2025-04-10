package com.example.deligoandroid.Driver.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DriverEarningsAdapter extends RecyclerView.Adapter<DriverEarningsAdapter.ViewHolder> {
    private List<Map<String, Object>> earningsList;
    private final SimpleDateFormat dateFormat;

    public DriverEarningsAdapter(List<Map<String, Object>> earningsList) {
        this.earningsList = earningsList;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_earnings_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> earning = earningsList.get(position);
        
        // Set order number
        holder.orderNumber.setText(String.format("Order #%s", earning.get("orderId")));
        
        // Set delivery date
        Long timestamp = (Long) earning.get("timestamp");
        if (timestamp != null) {
            holder.deliveryDate.setText(dateFormat.format(new Date(timestamp)));
        }
        
        // Set earnings details
        double deliveryFee = (double) earning.get("deliveryFee");
        double tipAmount = (double) earning.get("tipAmount");
        double total = (double) earning.get("total");
        
        holder.deliveryFee.setText(String.format(Locale.getDefault(), "$%.2f", deliveryFee));
        holder.tipAmount.setText(String.format(Locale.getDefault(), "$%.2f", tipAmount));
        holder.totalAmount.setText(String.format(Locale.getDefault(), "$%.2f", total));
    }

    @Override
    public int getItemCount() {
        return earningsList.size();
    }

    public void updateData(List<Map<String, Object>> newEarningsList) {
        this.earningsList = newEarningsList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderNumber;
        TextView deliveryDate;
        TextView deliveryFee;
        TextView tipAmount;
        TextView totalAmount;

        ViewHolder(View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.orderNumber);
            deliveryDate = itemView.findViewById(R.id.deliveryDate);
            deliveryFee = itemView.findViewById(R.id.deliveryFee);
            tipAmount = itemView.findViewById(R.id.tipAmount);
            totalAmount = itemView.findViewById(R.id.totalAmount);
        }
    }
} 