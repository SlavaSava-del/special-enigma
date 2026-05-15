package com.example.meteoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AddDiaryActivity extends Activity {

    private TextView backTitle;
    private Spinner healthSpinner;
    private TextView symptomsText;
    private EditText commentEditText;
    private TextView saveDiaryButton;

    private AppDatabaseHelper dbHelper;
    private SharedPreferences prefs;

    private boolean[] selectedSymptoms;

    private final String[] healthStates = {
            "Хорошее",
            "Нормальное",
            "Плохое"
    };

    private final String[] symptoms = {
            "Головная боль",
            "Усталость",
            "Сонливость",
            "Боль в суставах",
            "Головокружение",
            "Скачки давления"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_diary);

        dbHelper = new AppDatabaseHelper(this);
        prefs = getSharedPreferences("meteo_profile", MODE_PRIVATE);

        backTitle = findViewById(R.id.backTitle);
        healthSpinner = findViewById(R.id.healthSpinner);
        symptomsText = findViewById(R.id.symptomsText);
        commentEditText = findViewById(R.id.commentEditText);
        saveDiaryButton = findViewById(R.id.saveDiaryButton);

        selectedSymptoms = new boolean[symptoms.length];

        setupSpinners();
        setupButtons();
    }

    private void setupButtons() {
        backTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        symptomsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSymptomsDialog();
            }
        });

        saveDiaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveEntryFromForm();
            }
        });
    }

    private void setupSpinners() {
        ArrayAdapter<String> healthAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                healthStates
        );

        healthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        healthSpinner.setAdapter(healthAdapter);
        healthSpinner.setSelection(1);
    }

    private void showSymptomsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Выберите симптомы")
                .setMultiChoiceItems(symptoms, selectedSymptoms, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int index, boolean isChecked) {
                        selectedSymptoms[index] = isChecked;
                    }
                })
                .setPositiveButton("Готово", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        updateSymptomsText();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void updateSymptomsText() {
        String selected = getSelectedSymptoms();

        if (selected.equals("Нет симптомов")) {
            symptomsText.setText("Выберите симптомы");
        } else {
            symptomsText.setText(selected);
        }
    }

    private String getSelectedSymptoms() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < symptoms.length; i++) {
            if (selectedSymptoms[i]) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }

                builder.append(symptoms[i]);
            }
        }

        if (builder.length() == 0) {
            return "Нет симптомов";
        }

        return builder.toString();
    }

    private void saveEntryFromForm() {
        String healthState = healthSpinner.getSelectedItem().toString();
        String symptom = getSelectedSymptoms();
        String comment = commentEditText.getText().toString().trim();

        if (comment.length() == 0) {
            comment = "Без комментария";
        }

        double temperature = prefs.getFloat("last_temperature", 0);
        int pressure = prefs.getInt("last_pressure", 0);
        int humidity = prefs.getInt("last_humidity", 0);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("health_state", healthState);
        values.put("symptom", symptom);
        values.put("comment", comment);
        values.put("temperature", temperature);
        values.put("pressure", pressure);
        values.put("humidity", humidity);
        values.put("created_at", System.currentTimeMillis());

        db.insert("DiaryEntry", null, values);

        Toast.makeText(this, "Запись сохранена", Toast.LENGTH_SHORT).show();
        finish();
    }
}