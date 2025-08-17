package com.marcin.imionatrends.ui.top;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marcin.imionatrends.data.DatabaseHelper;
import com.marcin.imionatrends.data.GivenFirstNameData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TopViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Integer>> years = new MutableLiveData<>();
    private final MutableLiveData<List<GivenFirstNameData>> givenFirstNameData = new MutableLiveData<>();
    private final MutableLiveData<List<GivenFirstNameData>> originalGivenFirstNameData = new MutableLiveData<>();
    private final DatabaseHelper databaseHelper;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String selectedYear = "2024"; // Domyślny rok
    private String selectedGender = "Wszyscy"; // Domyślna płeć

    public TopViewModel(@NonNull Application application) {
        super(application);
        databaseHelper = new DatabaseHelper(application);
        loadYears(); // Załaduj lata domyślne
    }

    public LiveData<List<Integer>> getYears() {
        return years;
    }

    public LiveData<List<GivenFirstNameData>> getGivenFirstNameData() {
        return givenFirstNameData;
    }

    public LiveData<List<GivenFirstNameData>> getOriginalGivenFirstNameData() {
        return originalGivenFirstNameData;
    }

    private void loadYears() {
        executorService.execute(() -> {
            try {
                List<Integer> yearList = databaseHelper.getYears();
                mainHandler.post(() -> {
                    if (yearList != null && !yearList.isEmpty()) {
                        years.setValue(yearList);
                        selectedYear = yearList.get(0).toString(); // Ustaw domyślny rok na pierwszy rok z listy
                        loadData(); // Załaduj dane dla domyślnego roku
                    } else {
                        years.setValue(new ArrayList<>()); // Ustaw pustą listę, jeśli brak lat
                    }
                });
            } catch (Exception e) {
                Log.e("TopViewModel", "Error loading years", e);
            }
        });
    }

    private void loadData() {
        executorService.execute(() -> {
            try {
                List<GivenFirstNameData> data = databaseHelper.getRankingByYearAndGender(selectedYear, selectedGender);
                Log.d("TopViewModel", "Data loaded for year " + selectedYear + " and gender " + selectedGender + ": " + data);
                mainHandler.post(() -> {
                    givenFirstNameData.setValue(data);
                    originalGivenFirstNameData.setValue(data);
                });
            } catch (Exception e) {
                Log.e("TopViewModel", "Error loading data", e);
            }
        });
    }

    public void filterData(String query) {
        executorService.execute(() -> {
            try {
                List<GivenFirstNameData> originalData = originalGivenFirstNameData.getValue();
                Log.d("TopViewModel", "Original data for filtering: " + originalData);
                if (originalData != null) {
                    List<GivenFirstNameData> filteredList = new ArrayList<>();
                    String queryLowerCase = query.toLowerCase();

                    for (GivenFirstNameData item : originalData) {
                        if (item.getName().toLowerCase().contains(queryLowerCase)) {
                            filteredList.add(item);
                        }
                    }
                    Log.d("TopViewModel", "Filtered data: " + filteredList);
                    mainHandler.post(() -> givenFirstNameData.setValue(filteredList));
                }
            } catch (Exception e) {
                Log.e("TopViewModel", "Error filtering data", e);
            }
        });
    }

    public void updateYear(String year) {
        this.selectedYear = year; // Zaktualizuj wybrany rok
        loadData(); // Załaduj dane z nowym rokiem
    }

    public void updateGender(String gender) {
        this.selectedGender = gender; // Zaktualizuj wybraną płeć
        loadData(); // Załaduj dane z nową płcią
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown(); // Shutdown the executor service when the ViewModel is cleared
    }
}
