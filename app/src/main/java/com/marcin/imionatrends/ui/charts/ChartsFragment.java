package com.marcin.imionatrends.ui.charts;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.marcin.imionatrends.databinding.FragmentChartsBinding;

public class ChartsFragment extends Fragment {

    private FragmentChartsBinding binding;
    private ChartsViewModel chartsViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        chartsViewModel = new ViewModelProvider(this).get(ChartsViewModel.class);

        binding = FragmentChartsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        EditText searchEditText = binding.searchEditText;
        RecyclerView recyclerView = binding.recyclerView;
        LineChart lineChart = binding.lineChart;

        // Initialize adapter and set it to RecyclerView
        NameAdapter adapter = new NameAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // Set LayoutManager

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                chartsViewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed
            }
        });

        // Observe LiveData from ViewModel and update UI accordingly
        chartsViewModel.getSearchResults().observe(getViewLifecycleOwner(), searchResults -> {
            adapter.setData(searchResults);
        });

        chartsViewModel.getChartData().observe(getViewLifecycleOwner(), chartData -> {
            // Update LineChart with new data
            lineChart.setData(chartData); // Set data to the chart
            lineChart.invalidate(); // Refresh the chart
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}