package com.marcin.imionatrends.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "imiona_trendy.db";
    private static final int DATABASE_VERSION = 1;
    private static final int LAST_YEAR_AVAILABLE_DATA = 2024;

    // Table names
    private static final String TABLE_GIVEN_FIRST_NAME_DATA = "given_firstname_data";
    private static final String TABLE_LIVE_FIRST_NAME_DATA = "live_firstname_data";

    // Common column names
    private static final String KEY_ID = "id";

    // GIVEN_firstname_data table - column names
    private static final String KEY_YEAR = "year";
    private static final String KEY_NAME = "name";
    private static final String KEY_COUNT = "count";
    private static final String KEY_IS_MALE = "is_male";
    private static final String KEY_PERCENTAGE = "percentage";

    // live_firstname_data table - column names
    private static final String KEY_LIVE_NAME = "live_name";
    private static final String KEY_LIVE_COUNT = "live_count";
    private static final String KEY_IS_MALE_LIVE = "is_male";
    private static final String KEY_LIVE_PERCENTAGE = "live_percentage";


    // Batch size for bulk insertion
    private static final int BATCH_SIZE = 10000;


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create firstname_data table
        String CREATE_TABLE_FIRST_NAME_DATA = "CREATE TABLE " + TABLE_GIVEN_FIRST_NAME_DATA + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_YEAR + " INTEGER,"
                + KEY_NAME + " TEXT,"
                + KEY_COUNT + " INTEGER,"
                + KEY_IS_MALE + " INTEGER,"
                + KEY_PERCENTAGE + " REAL"
                + ")";
        db.execSQL(CREATE_TABLE_FIRST_NAME_DATA);

        // Create live_firstname_data table
        String CREATE_TABLE_LIVE_FIRST_NAME_DATA = "CREATE TABLE " + TABLE_LIVE_FIRST_NAME_DATA + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_LIVE_NAME + " TEXT,"
                + KEY_LIVE_COUNT + " INTEGER,"
                + KEY_IS_MALE_LIVE + " INTEGER, "
                + KEY_LIVE_PERCENTAGE + " REAL"
                + ")";
        db.execSQL(CREATE_TABLE_LIVE_FIRST_NAME_DATA);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GIVEN_FIRST_NAME_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LIVE_FIRST_NAME_DATA);
        // Create tables again
        onCreate(db);
    }

    // Insert firstname_data records in batch
    public void insertFirstNameData(List<GivenFirstNameData> givenFirstNameDataList) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransactionNonExclusive();
        try {
            String sql = "INSERT INTO " + TABLE_GIVEN_FIRST_NAME_DATA + " (" +
                    KEY_YEAR + ", " + KEY_NAME + ", " + KEY_COUNT + ", " + KEY_IS_MALE + ") " +
                    "VALUES (?, ?, ?, ?)";
            SQLiteStatement statement = db.compileStatement(sql);

            for (GivenFirstNameData data : givenFirstNameDataList) {
                statement.clearBindings();
                statement.bindLong(1, data.getYear());
                statement.bindString(2, data.getName());
                statement.bindLong(3, data.getCount());
                statement.bindLong(4, data.isMale());
                statement.execute();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Insert live_firstname_data records in batch
    public void insertLiveFirstNameData(List<LiveFirstNameData> liveFirstNameDataList) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransactionNonExclusive();
        try {
            String sql = "INSERT INTO " + TABLE_LIVE_FIRST_NAME_DATA + " (" +
                    KEY_LIVE_NAME + ", " + KEY_LIVE_COUNT + ", " + KEY_IS_MALE_LIVE + ") " +
                    "VALUES (?, ?, ?)";
            SQLiteStatement statement = db.compileStatement(sql);

            for (LiveFirstNameData data : liveFirstNameDataList) {
                statement.clearBindings();
                statement.bindString(1, data.getName());
                statement.bindLong(2, data.getCount());
                statement.bindLong(3, data.getIsMale());
                statement.execute();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Check if firstname_data table is empty
    public boolean isFirstNameDataEmpty() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_GIVEN_FIRST_NAME_DATA, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count == 0;
    }

    // Check if live_firstname_data table is empty
    public boolean isLiveFirstNameDataEmpty() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_LIVE_FIRST_NAME_DATA, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count == 0;
    }

    //    get all names from TABLE_UNIQUE_NAMES

    public List<String> getAllUniqueNames() {
        List<String> names = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + KEY_LIVE_NAME + " FROM " + TABLE_LIVE_FIRST_NAME_DATA + " ORDER BY " + KEY_LIVE_COUNT + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                names.add(cursor.getString(cursor.getColumnIndexOrThrow(KEY_LIVE_NAME)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        Log.d(TAG, "getAllUniqueNames: " + names);
        return names;
    }



    // Download and insert data for given names from CSV URL
    public void downloadAndInsertCSVGivenNames(String urlString, int year) throws IOException, CsvException {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
             CSVReader csvReader = new CSVReader(reader)) {
            List<GivenFirstNameData> batchData = new ArrayList<>();
            String[] nextLine;
            csvReader.readNext(); // Skip header
            while ((nextLine = csvReader.readNext()) != null) {
                String name = nextLine[0];
                int count = Integer.parseInt(nextLine[2]);
                int isMale = nextLine[1].toUpperCase().startsWith("M") ? 1 : 0;
                GivenFirstNameData givenFirstNameData = new GivenFirstNameData(year, name, count, isMale);
                batchData.add(givenFirstNameData);
                if (batchData.size() >= BATCH_SIZE) {
                    insertFirstNameData(batchData); // Insert batch of data
                    batchData.clear();
                }
            }
            // Insert remaining data
            if (!batchData.isEmpty()) {
                insertFirstNameData(batchData); // Insert remaining data
            }
            Log.d(TAG, "Downloaded and inserted CSV GivenNames: " + urlString);
        }
    }

    public void downloadAndInsertCSVGivenNames(String urlString) throws IOException, CsvException {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
             CSVReader csvReader = new CSVReader(reader)) {
            List<GivenFirstNameData> batchData = new ArrayList<>();
            String[] nextLine;
            csvReader.readNext(); // Skip header
            while ((nextLine = csvReader.readNext()) != null) {
                int year = Integer.parseInt(nextLine[0]);

                String name = nextLine[1];
                int count = Integer.parseInt(nextLine[2]);
                int isMale = nextLine[3].toUpperCase().startsWith("M") ? 1 : 0;
                GivenFirstNameData givenFirstNameData = new GivenFirstNameData(year, name, count, isMale);
                batchData.add(givenFirstNameData);
                if (batchData.size() >= BATCH_SIZE) {
                    insertFirstNameData(batchData); // Insert batch of data
                    batchData.clear();
                }
            }
            // Insert remaining data
            if (!batchData.isEmpty()) {
                insertFirstNameData(batchData); // Insert remaining data
                batchData.clear();
            }
            Log.d(TAG, "Downloaded and inserted CSV GivenNames: " + urlString);
        }
    }

    // Download and insert live data from CSV URL
    public void downloadAndInsertCSVLiveNames(String urlString, int isMale) throws IOException, CsvException {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            CSVReader csvReader = new CSVReader(reader)) {
            List<LiveFirstNameData> batchData = new ArrayList<>();
            String[] nextLine;
            csvReader.readNext();
            while ((nextLine = csvReader.readNext()) != null) {
                String name = nextLine[0];
                int count = Integer.parseInt(nextLine[2]);
                LiveFirstNameData liveFirstNameData = new LiveFirstNameData(name, count, isMale);
                batchData.add(liveFirstNameData);
                if (batchData.size() >= BATCH_SIZE) {
                    insertLiveFirstNameData(batchData); // Insert batch of data
                    batchData.clear();
                }
            }
            // Insert remaining data
            if (!batchData.isEmpty()) {
                insertLiveFirstNameData(batchData); // Insert remaining data
            }
            Log.d(TAG, "Downloaded and inserted CSV LiveNames: " + (isMale == 1 ? "Male" : "Female"));
        }
    }


    //    get data from live_firstname_data table
    public List<LiveFirstNameData> getAllLiveFirstNameData() {
        List<LiveFirstNameData> liveFirstNameDatas = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM "+ TABLE_LIVE_FIRST_NAME_DATA + " order by 3 desc", null);
        if (cursor.moveToFirst()) {
            do {
                LiveFirstNameData liveFirstNameData = new LiveFirstNameData(
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_LIVE_NAME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_LIVE_COUNT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_MALE_LIVE)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_LIVE_PERCENTAGE))
                );
                liveFirstNameDatas.add(liveFirstNameData);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return liveFirstNameDatas;
    }


    public List<Integer> getYears() {
        List<Integer> years = new ArrayList<>();
        for (int year = LAST_YEAR_AVAILABLE_DATA; year >= 2000; year--) {
            years.add(year);
        }
        return years;
    }


    public List<GivenFirstNameData> getRankingByYearAndGender(String year, String gender) {
        List<GivenFirstNameData> data = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        // Validate inputs
        if (year == null || year.trim().isEmpty()) {
            Log.e("DatabaseHelper", "Year is null or empty");
            return data; // Return empty list if year is invalid
        }

        if (gender == null) {
            gender = "Wszyscy"; // Default to 'Wszyscy' if gender is null
        }

        // Define gender filter
        String genderFilter = null;
        switch (gender) {
            case "Wszyscy":
                // No filter for gender
                Log.d("DatabaseHelper", "Gender filter set to 'Wszyscy'");
                break;
            case "Mężczyźni":
                genderFilter = "1";
                Log.d("DatabaseHelper", "Gender filter set to 'Mężczyźni'");
                break;
            case "Kobiety":
                genderFilter = "0";
                Log.d("DatabaseHelper", "Gender filter set to 'Kobiety'");
                break;
            default:
                Log.e("DatabaseHelper", "Unrecognized gender filter: " + gender);
                return data; // Return empty list for unrecognized gender
        }

        try {
            String query;
            String[] selectionArgs;
            if (genderFilter == null) {
                query = "SELECT name, count, percentage FROM " + TABLE_GIVEN_FIRST_NAME_DATA + " WHERE year = ? ORDER BY 3 DESC";
                selectionArgs = new String[]{year};
            } else {
                query = "SELECT name, count, percentage FROM " + TABLE_GIVEN_FIRST_NAME_DATA + " WHERE year = ? AND is_male = ?";
                selectionArgs = new String[]{year, genderFilter};
            }

            cursor = db.rawQuery(query, selectionArgs);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndex("name"));
                    int count = cursor.getInt(cursor.getColumnIndex("count"));
                    float percentage = cursor.getFloat(cursor.getColumnIndex("percentage"));
                    GivenFirstNameData givenFirstNameData = new GivenFirstNameData(name, count, percentage);
                    data.add(givenFirstNameData);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error while trying to get ranking by year and gender", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }

        return data;
    }





    public void updatePercentagesGivenNamesByYears() {
        List<Integer> years = getYears();
        for (int year : years) {
            updatePercentagesForYear(String.valueOf(year));
        }
    }


    private void updatePercentagesForYear(String year) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT SUM("+KEY_COUNT+") FROM " + TABLE_GIVEN_FIRST_NAME_DATA + " WHERE "+ KEY_YEAR + " = ?";
        Cursor totalCursor = db.rawQuery(query, new String[]{year});
        if (totalCursor.moveToFirst()) {
            int total = totalCursor.getInt(0);
            totalCursor.close();


            String updateQuery = "UPDATE "+ TABLE_GIVEN_FIRST_NAME_DATA + " SET " +  KEY_PERCENTAGE + " = ROUND((count * 100.0 / ?), 4) WHERE year = ?";
            db.execSQL(updateQuery, new String[]{String.valueOf(total), year});

        }
        db.close();
        Log.d(TAG, "Updated percentages for given names in year " + year);
    }


    public void updatePercentagesLiveNames() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT SUM("+KEY_LIVE_COUNT+") FROM " + TABLE_LIVE_FIRST_NAME_DATA;
        Cursor totalCursor = db.rawQuery(query, null);
        if (totalCursor.moveToFirst()) {
            int total = totalCursor.getInt(0);
            totalCursor.close();
            String updateQuery = "UPDATE " + TABLE_LIVE_FIRST_NAME_DATA + " SET " + KEY_LIVE_PERCENTAGE + " = ROUND(("+KEY_LIVE_COUNT+" * 100.0 / ?), 4)";
            db.execSQL(updateQuery, new String[]{String.valueOf(total)});
        }
        db.close();
        Log.d(TAG, "Updated percentages for live names");
    }


    public LineData getChartDataForName(String name, boolean isPercentage) {
        // Lista do przechowywania wartości wykresu
        List<Entry> entries = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        // Tworzenie zapytania SQL w zależności od wyboru (procentowe/liczbowe)
        String query = "SELECT " + KEY_YEAR + ", " + (isPercentage ? KEY_PERCENTAGE : KEY_COUNT) +
                " FROM " + TABLE_GIVEN_FIRST_NAME_DATA +
                " WHERE " + KEY_NAME + " = ?" +
                " ORDER BY " + KEY_YEAR + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{name});

        // Sprawdzanie, czy kursor zawiera wyniki
        if (cursor.moveToFirst()) {
            do {
                // Pobieranie wartości z kursora
                int year = cursor.getInt(cursor.getColumnIndex(KEY_YEAR));
                float value = cursor.getFloat(cursor.getColumnIndex(isPercentage ? KEY_PERCENTAGE : KEY_COUNT));

                // Dodawanie wartości do listy jako Entry (rok, wartość)
                entries.add(new Entry(year, value));
            } while (cursor.moveToNext());
        }

        cursor.close(); // Zamknięcie kursora
        db.close(); // Zamknięcie bazy danych
        // Tworzenie zestawu danych wykresu
        Log.d(TAG, entries.size() + " entries");
        Log.d(TAG, entries.toString());
        LineDataSet dataSet = new LineDataSet(entries, "Data");
        dataSet.setColor(Color.BLUE); // Kolor linii wykresu
        dataSet.setValueTextColor(Color.BLACK); // Kolor tekstu wartości na wykresie

        // Tworzenie obiektu LineData z zestawem danych
        return new LineData(dataSet);
    }

    public List<LiveFirstNameData> getAllLiveFirstNameDataByGender(String gender) {

        String condition = gender.equals("Wszyscy") ? "" : " where is_male = " + (gender.equals("Mężczyźni") ? "1" : "0");
        String query = "SELECT * FROM " + TABLE_LIVE_FIRST_NAME_DATA + condition + " order by 3 desc";

        List<LiveFirstNameData> liveFirstNameDatas = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                LiveFirstNameData liveFirstNameData = new LiveFirstNameData(
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_LIVE_NAME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_LIVE_COUNT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_MALE_LIVE)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_LIVE_PERCENTAGE))
                );
                liveFirstNameDatas.add(liveFirstNameData);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return liveFirstNameDatas;
    }
}

