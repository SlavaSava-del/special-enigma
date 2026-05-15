package com.example.meteoapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class ProfileActivity extends Activity {

    private EditText nameEditText;
    private EditText cityEditText;
    private Spinner sensitivitySpinner;
    private Switch warningsSwitch;
    private Switch autoUpdateSwitch;
    private TextView openStatisticsButton;
    private TextView saveProfileButton;
    private TextView navHome;
    private TextView navDiary;

    private SharedPreferences prefs;

    private final String[] sensitivityTypes = {
            "Чувствительность к давлению",
            "Чувствительность к влажности",
            "Чувствительность к температуре",
            "Общая метеочувствительность"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        prefs = getSharedPreferences("meteo_profile", MODE_PRIVATE);

        initViews();
        setupSpinner();
        loadProfile();
        setupButtons();
    }

    private void initViews() {
        nameEditText = findViewById(R.id.nameEditText);
        cityEditText = findViewById(R.id.cityEditText);
        sensitivitySpinner = findViewById(R.id.sensitivitySpinner);
        warningsSwitch = findViewById(R.id.warningsSwitch);
        autoUpdateSwitch = findViewById(R.id.autoUpdateSwitch);
        openStatisticsButton = findViewById(R.id.openStatisticsButton);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        navHome = findViewById(R.id.navHome);
        navDiary = findViewById(R.id.navDiary);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                sensitivityTypes
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sensitivitySpinner.setAdapter(adapter);
    }

    private void loadProfile() {
        String name = prefs.getString("name", "Вячеслав");
        String city = prefs.getString("city", "Moscow");
        String sensitivity = prefs.getString("sensitivity", "Чувствительность к давлению");
        boolean warnings = prefs.getBoolean("warnings", true);
        boolean autoUpdate = prefs.getBoolean("auto_update", true);

        nameEditText.setText(name);
        cityEditText.setText(city);
        warningsSwitch.setChecked(warnings);
        autoUpdateSwitch.setChecked(autoUpdate);

        for (int i = 0; i < sensitivityTypes.length; i++) {
            if (sensitivityTypes[i].equals(sensitivity)) {
                sensitivitySpinner.setSelection(i);
                break;
            }
        }
    }

    private void setupButtons() {
        openStatisticsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ProfileActivity.this, StatisticsActivity.class));
            }
        });

        saveProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveProfile();
            }
        });

        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                finish();
            }
        });

        navDiary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ProfileActivity.this, DiaryActivity.class));
                finish();
            }
        });
    }

    private void saveProfile() {
        String name = nameEditText.getText().toString().trim();
        String city = cityEditText.getText().toString().trim();
        String sensitivity = sensitivitySpinner.getSelectedItem().toString();

        if (name.length() == 0) {
            name = "Пользователь";
        }

        if (city.length() == 0) {
            city = "Moscow";
        }

        prefs.edit()
                .putString("name", name)
                .putString("city", city)
                .putString("sensitivity", sensitivity)
                .putBoolean("warnings", warningsSwitch.isChecked())
                .putBoolean("auto_update", autoUpdateSwitch.isChecked())
                .apply();

        Toast.makeText(this, "Профиль сохранён", Toast.LENGTH_SHORT).show();
    }
}