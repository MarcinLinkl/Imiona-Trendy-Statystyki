package com.marcin.imionatrends.ui.top;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.marcin.imionatrends.R;
import com.marcin.imionatrends.data.GivenFirstNameData;

import java.util.ArrayList;
import java.util.List;

public class GivenFirstNameDataAdapter extends RecyclerView.Adapter<GivenFirstNameDataAdapter.NameViewHolder> {

    private List<GivenFirstNameData> dataList = new ArrayList<>();

    @NonNull
    @Override
    public NameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_name, parent, false);
        return new NameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NameViewHolder holder, int position) {
        GivenFirstNameData data = dataList.get(position);
        holder.rankTextView.setText(String.valueOf(position + 1));
        holder.nameTextView.setText(data.getName());
        holder.countTextView.setText(String.valueOf(data.getCount()));
        holder.percentageTextView.setText(String.format("%.2f%%", data.getPercentage()));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public void setData(List<GivenFirstNameData> data) {
        this.dataList.clear();
        if (data != null) {
            this.dataList.addAll(data);
        }
        notifyDataSetChanged();
    }

    static class NameViewHolder extends RecyclerView.ViewHolder {

        TextView rankTextView;
        TextView nameTextView;
        TextView countTextView;
        TextView percentageTextView;

        NameViewHolder(@NonNull View itemView) {
            super(itemView);
            rankTextView = itemView.findViewById(R.id.order_number);
            nameTextView = itemView.findViewById(R.id.first_name);
            countTextView = itemView.findViewById(R.id.count);
            percentageTextView = itemView.findViewById(R.id.percentage);
        }
    }
}