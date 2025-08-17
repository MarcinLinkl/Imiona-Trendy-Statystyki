package com.marcin.imionatrends.data;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CSVDownloader {
    private static final String TAG = "CSVDownloader";
    private static final String CSV_GIVEN_NAMES_URL_2000_2019 = "https://api.dane.gov.pl/resources/21458,imiona-nadane-dzieciom-w-polsce-w-latach-2000-2019-imie-pierwsze/csv";
    private static final Map<String, Integer> CSV_GIVEN_2020_2023_MAP = Map.of(
            "https://api.dane.gov.pl/resources/28020,imiona-meskie-nadane-dzieciom-w-polsce-w-2020-r-imie-pierwsze/csv", 2020,
            "https://api.dane.gov.pl/resources/28021,imiona-zenskie-nadane-dzieciom-w-polsce-w-2020-r-imie-pierwsze/csv", 2020,
            "https://api.dane.gov.pl/resources/36393,imiona-meskie-nadane-dzieciom-w-polsce-w-2021-r-imie-pierwsze/csv", 2021,
            "https://api.dane.gov.pl/resources/36394,imiona-zenskie-nadane-dzieciom-w-polsce-w-2021-r-imie-pierwsze/csv", 2021,
            "https://api.dane.gov.pl/resources/44825,imiona-meskie-nadane-dzieciom-w-polsce-w-2022-r-imie-pierwsze/csv", 2022,
            "https://api.dane.gov.pl/resources/44824,imiona-zenskie-nadane-dzieciom-w-polsce-w-2022-r-imie-pierwsze/csv", 2022,
            "https://api.dane.gov.pl/resources/54099,imiona-meskie-nadane-dzieciom-w-polsce-w-2023-r-imie-pierwsze/csv", 2023,
            "https://api.dane.gov.pl/resources/54100,imiona-zenskie-nadane-dzieciom-w-polsce-w-2023-r-imie-pierwsze/csv", 2023,
            "https://api.dane.gov.pl/resources/63900,imiona-meskie-nadane-dzieciom-w-polsce-w-2024-r-imie-pierwsze/csv",2024,
            "https://api.dane.gov.pl/resources/63899,imiona-zenskie-nadane-dzieciom-w-polsce-w-2024-r-imie-pierwsze/csv",2024



    );
    private static final String CSV_NAMES_LIVE_MALE_FROM_2024 = "https://api.dane.gov.pl/resources/54109,lista-imion-meskich-w-rejestrze-pesel-stan-na-19012023-imie-pierwsze/csv";
    private static final String CSV_NAMES_LIVE_FEMALE_FROM_2024 = "https://api.dane.gov.pl/resources/54110,lista-imion-zenskich-w-rejestrze-pesel-stan-na-19012024-imie-pierwsze/csv";
    private static final int TIMEOUT = 2;

    public static void downloadCsvData(Context context, Runnable onSuccessInsertingData, Runnable onFailureInsertingData, Runnable onDataMissing, Runnable onDataFull) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        // Check if first name data already exists
        if (!dbHelper.isFirstNameDataEmpty() && !dbHelper.isLiveFirstNameDataEmpty()) {
            onDataFull.run();
            return;
        }
        onDataMissing.run();

        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                // Check if first name data already exists
                if (dbHelper.isFirstNameDataEmpty() || dbHelper.isLiveFirstNameDataEmpty()) {
                    onDataMissing.run();

                    if (dbHelper.isFirstNameDataEmpty()) {
                        executor.submit(() -> {
                            try {
                                downloadAndInsertCSVGivenNames(dbHelper, CSV_GIVEN_NAMES_URL_2000_2019);
                            } catch (IOException | CsvException e) {
                                e.printStackTrace();
                                onFailureInsertingData.run();
                            }
                        });
                    }

                    for (Map.Entry<String, Integer> entry : CSV_GIVEN_2020_2023_MAP.entrySet()) {
                        executor.submit(() -> {
                            try {
                                downloadAndInsertCSVGivenNames(dbHelper, entry.getKey(), entry.getValue());
                            } catch (IOException | CsvException e) {
                                e.printStackTrace();
                                onFailureInsertingData.run();
                            }
                        });
                    }
                 if (dbHelper.isLiveFirstNameDataEmpty()) {
                        executor.submit(() -> {
                            try {
                                downloadAndInsertCSVLiveNames(dbHelper, CSV_NAMES_LIVE_FEMALE_FROM_2024, 0);
                            } catch (IOException | CsvException e) {
                                e.printStackTrace();
                                onFailureInsertingData.run();
                            }
                        });

                        executor.submit(() -> {
                            try {
                                downloadAndInsertCSVLiveNames(dbHelper, CSV_NAMES_LIVE_MALE_FROM_2024, 1);
                            } catch (IOException | CsvException e) {
                                e.printStackTrace();
                                onFailureInsertingData.run();
                            }
                        });
                    }
                }else {
                    onDataFull.run();
                }

                executor.shutdown();

                try {
                    if (!executor.awaitTermination(TIMEOUT, TimeUnit.MINUTES)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                }
                long endTime = System.currentTimeMillis();
                long elapsedTime = endTime - startTime;
                Log.d(TAG, "Time elapsed: " + elapsedTime / 1000 + "s");
                dbHelper.updatePercentagesGivenNamesByYears();
                dbHelper.updatePercentagesLiveNames();



                // Notify success
                onSuccessInsertingData.run();

            } catch (Exception e) {
                e.printStackTrace();
                onFailureInsertingData.run();
            }


        }).start();
    }

    private static void downloadAndInsertCSVGivenNames(DatabaseHelper dbHelper, String urlString) throws IOException, CsvException {
        dbHelper.downloadAndInsertCSVGivenNames(urlString);
    }

    private static void downloadAndInsertCSVGivenNames(DatabaseHelper dbHelper, String urlString, int year) throws IOException, CsvException {
        dbHelper.downloadAndInsertCSVGivenNames(urlString, year);
    }

    private static void downloadAndInsertCSVLiveNames(DatabaseHelper dbHelper, String urlString, int isMale) throws IOException, CsvException {
        dbHelper.downloadAndInsertCSVLiveNames(urlString, isMale);
    }
}
