package com.marcin.imionatrends.ui.charts;

import android.app.Application;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.chip.Chip;
import com.marcin.imionatrends.R;
import com.marcin.imionatrends.databinding.FragmentChartsBinding;
import com.github.mikephil.charting.charts.LineChart;

import java.util.List;

public class ChartsFragment extends Fragment {

    private FragmentChartsBinding binding;
    private ChartsViewModel chartsViewModel;
    private LineChart lineChart;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChartsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Set up ViewModel
        chartsViewModel = new ViewModelProvider(this).get(ChartsViewModel.class);
        binding.setViewModel(chartsViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // Initialize LineChart
        lineChart = binding.lineChart;

        // Setup observers and listeners
        setupObservers();
        setupListeners();
        setupAutoCompleteTextView(); // Set up AutoCompleteTextView
        styleLineChart();
        return root;
    }

    private void setupObservers() {
        chartsViewModel.getUniqueNames().observe(getViewLifecycleOwner(), names -> {
            if (names != null) {
                setupAutoCompleteTextViewAdapter(names); // Set up AutoCompleteTextView adapter
            }
        });

        chartsViewModel.getChartData().observe(getViewLifecycleOwner(), lineData -> {
            if (lineData != null) {
                lineChart.setData(lineData);
                for (ILineDataSet set : lineChart.getData().getDataSets()) {
                    LineDataSet dataSet = (LineDataSet) set;
                    dataSet.setLineWidth(3f);   //
                    dataSet.setDrawCircles(false);  //
//                    dataSet.setCircleRadius(4f);   //
                    dataSet.setDrawValues(false);
                }



                // Customize the X-axis to display years without commas
                XAxis xAxis = lineChart.getXAxis();
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        // Format the x value to be a year (e.g., 2020 instead of 2020.0)
                        return String.format("%d", (int) value);
                    }
                });

                lineChart.invalidate(); // Refresh the chart
            }
        });
    }

    private void setupListeners() {
        binding.switchPercentage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            chartsViewModel.setPercentageValue(isChecked);
        });
    }

    private void setupAutoCompleteTextView() {
        binding.nameAutoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            chartsViewModel.addNameToChart(selectedName);
            addChipToGroup(selectedName);
        });
    }

    private void setupAutoCompleteTextViewAdapter(List<String> names) {
        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, names);
        binding.nameAutoCompleteTextView.setAdapter(autoCompleteAdapter);
    }

    private void addChipToGroup(String name) {
        if (binding.chipGroup.findViewWithTag(name) == null) {
            Chip chip = new Chip(getContext());
            chip.setText(name);
            chip.setTag(name);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                binding.chipGroup.removeView(chip);
                chartsViewModel.removeNameFromChart(name);
            });
            binding.chipGroup.addView(chip);
        }
    }

    private void styleLineChart() {
//        add line width
//        lineChart.set
        // Opis wyłączony
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setDrawBorders(false);

        // Animacja
        lineChart.animateX(1000);

        // OŚ X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // krok co 1 (np. rok)
        xAxis.setTextSize(12f);
        xAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.line_chart_axis_color));
        xAxis.setLabelRotationAngle(-45); // obrócenie etykiet, żeby się nie ściskały

        // OŚ Y lewa
        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getAxisLeft().setGridColor(ContextCompat.getColor(getContext(), R.color.line_chart_grid_color));
//        lineChart.getAxisLeft().setGridLineWidth(1f);
        lineChart.getAxisLeft().setTextColor(ContextCompat.getColor(getContext(), R.color.line_chart_axis_color));
        lineChart.getAxisLeft().setTextSize(12f);

        // Wyłącz prawa oś Y
        lineChart.getAxisRight().setEnabled(false);

        // LEGENDA
        lineChart.getLegend().setEnabled(true);
        lineChart.getLegend().setTextSize(14f);
        lineChart.getLegend().setTextColor(ContextCompat.getColor(getContext(), R.color.line_chart_legend_color));
        lineChart.getLegend().setWordWrapEnabled(true); // zawijanie, jeśli dużo serii
        lineChart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP);
        lineChart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        lineChart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        lineChart.getLegend().setDrawInside(false);

        // Padding wykresu (żeby nie ściskał się do krawędzi)
        lineChart.setExtraOffsets(8f, 8f, 8f, 8f);

//        // Interakcja
//        lineChart.setPinchZoom(true);   // pinch zoom
//        lineChart.setScaleEnabled(true);
//        lineChart.setDragEnabled(true);

        // Odśwież
        lineChart.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh chart data when fragment resumes
        chartsViewModel.updateChartData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        chartsViewModel.resetChartData(); // Reset chart data when fragment is destroyed
        binding = null; // Clean up binding to avoid memory leaks
    }
}
