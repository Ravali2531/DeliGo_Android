package com.example.deligoandroid.Restaurant;

import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.R;
import java.util.List;
import java.util.Locale;

public class StoreHoursAdapter extends RecyclerView.Adapter<StoreHoursAdapter.ViewHolder> {
    private final List<StoreHoursActivity.DaySchedule> schedules;
    private final OnScheduleUpdateListener listener;

    public interface OnScheduleUpdateListener {
        void onScheduleUpdated(StoreHoursActivity.DaySchedule schedule);
    }

    public StoreHoursAdapter(List<StoreHoursActivity.DaySchedule> schedules, OnScheduleUpdateListener listener) {
        this.schedules = schedules;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_store_hours, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StoreHoursActivity.DaySchedule schedule = schedules.get(position);
        holder.bind(schedule);
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView dayText;
        private final TextView openTimeText;
        private final TextView closeTimeText;
        private final Switch isOpenSwitch;
        private final View timeContainer;

        ViewHolder(View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.dayText);
            openTimeText = itemView.findViewById(R.id.openTimeText);
            closeTimeText = itemView.findViewById(R.id.closeTimeText);
            isOpenSwitch = itemView.findViewById(R.id.isOpenSwitch);
            timeContainer = itemView.findViewById(R.id.timeContainer);
        }

        void bind(StoreHoursActivity.DaySchedule schedule) {
            dayText.setText(schedule.day.toUpperCase());
            openTimeText.setText(schedule.openTime);
            closeTimeText.setText(schedule.closeTime);
            isOpenSwitch.setChecked(schedule.isOpen);
            timeContainer.setAlpha(schedule.isOpen ? 1.0f : 0.5f);

            isOpenSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                schedule.isOpen = isChecked;
                timeContainer.setAlpha(isChecked ? 1.0f : 0.5f);
                listener.onScheduleUpdated(schedule);
            });

            openTimeText.setOnClickListener(v -> showTimePickerDialog(v.getContext(), true, schedule));
            closeTimeText.setOnClickListener(v -> showTimePickerDialog(v.getContext(), false, schedule));
        }

        private void showTimePickerDialog(Context context, boolean isOpenTime, StoreHoursActivity.DaySchedule schedule) {
            String currentTime = isOpenTime ? schedule.openTime : schedule.closeTime;
            String[] timeParts = currentTime.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            TimePickerDialog dialog = new TimePickerDialog(context, (view, hourOfDay, minute1) -> {
                String newTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                if (isOpenTime) {
                    schedule.openTime = newTime;
                    openTimeText.setText(newTime);
                } else {
                    schedule.closeTime = newTime;
                    closeTimeText.setText(newTime);
                }
                listener.onScheduleUpdated(schedule);
            }, hour, minute, true);

            dialog.show();
        }
    }
} 