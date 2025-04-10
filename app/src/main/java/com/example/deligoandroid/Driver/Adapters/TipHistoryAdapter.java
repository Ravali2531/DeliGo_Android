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

public class TipHistoryAdapter extends RecyclerView.Adapter<TipHistoryAdapter.ViewHolder> {
    private List<Map<String, Object>> tipHistory;
    private SimpleDateFormat dateFormat;

    public TipHistoryAdapter(List<Map<String, Object>> tipHistory) {
        this.tipHistory = tipHistory;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_tip_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> delivery = tipHistory.get(position);
        
        holder.orderNumber.setText("Order #" + delivery.get("orderId"));
        
        // Format and set the delivery date
        Long timestamp = (Long) delivery.get("timestamp");
        if (timestamp != null) {
            holder.deliveryDate.setText(dateFormat.format(new Date(timestamp)));
        }


        // Format and set tip amount
        Double tipAmount = getDoubleValue(delivery.get("tipAmount"));
        holder.tipAmount.setText(String.format(Locale.getDefault(), "$%.2f", tipAmount));
    }

    private Double getDoubleValue(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        return 0.0;
    }

    @Override
    public int getItemCount() {
        return tipHistory.size();
    }

    public void updateData(List<Map<String, Object>> newTipHistory) {
        this.tipHistory = newTipHistory;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderNumber;
        TextView deliveryDate;

        TextView tipAmount;

        ViewHolder(View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.orderNumber);
            deliveryDate = itemView.findViewById(R.id.deliveryDate);
            tipAmount = itemView.findViewById(R.id.tipAmount);
        }
    }
} 