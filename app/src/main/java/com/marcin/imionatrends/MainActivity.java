package com.marcin.imionatrends;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.marcin.imionatrends.data.CSVDownloader;
import com.marcin.imionatrends.databinding.ActivityMainBinding;
public class MainActivity extends AppCompatActivity {

private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
            R.id.navigation_people, R.id.navigation_top, R.id.navigation_chart)
            .build();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

//        binding the BottomNavigationView with the NavController
        NavigationUI.setupWithNavController(binding.navView, navController);
//      biding the Toolbar with the NavController
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
        updateDatabase();


    }

    private void updateDatabase() {
        CSVDownloader.downloadCsvData(this,
                () -> runOnUiThread(() -> Toast.makeText(MainActivity.this, "Checking And Updating Complete", Toast.LENGTH_SHORT).show()),
                () -> runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to download and complete data", Toast.LENGTH_SHORT).show()),
                () -> runOnUiThread(() -> Toast.makeText(MainActivity.this, "Data is missing or incomplete, starting download", Toast.LENGTH_SHORT).show()),
                () -> runOnUiThread(() -> Toast.makeText(MainActivity.this, "Data is already available", Toast.LENGTH_SHORT).show()));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_theme) {
            toggleTheme();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleTheme() {
        int nightMode = AppCompatDelegate.getDefaultNightMode();
        if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }
}