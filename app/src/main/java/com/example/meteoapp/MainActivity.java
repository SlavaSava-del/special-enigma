package com.example.meteoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    private TextView cityText;
    private TextView weatherIconText;
    private TextView temperatureText;
    private TextView descriptionText;
    private TextView pressureText;
    private TextView humidityText;
    private TextView riskShieldText;
    private TextView riskLevelText;
    private TextView recommendationText;
    private TextView openDiaryButton;
    private TextView profileButton;
    private TextView navDiary;
    private TextView navProfile;
    private LinearLayout riskContainer;

    private AppDatabaseHelper dbHelper;
    private SharedPreferences prefs;

    private static final String API_KEY = "afe8398de20d68db1ca149bb3f589bfe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new AppDatabaseHelper(this);
        prefs = getSharedPreferences("meteo_profile", MODE_PRIVATE);

        initViews();
        setupButtons();

        String city = prefs.getString("city", "Moscow");
        updateWeather(city);
    }

    private void initViews() {
        cityText = findViewById(R.id.cityText);
        weatherIconText = findViewById(R.id.weatherIconText);
        temperatureText = findViewById(R.id.temperatureText);
        descriptionText = findViewById(R.id.descriptionText);
        pressureText = findViewById(R.id.pressureText);
        humidityText = findViewById(R.id.humidityText);
        riskShieldText = findViewById(R.id.riskShieldText);
        riskLevelText = findViewById(R.id.riskLevelText);
        recommendationText = findViewById(R.id.recommendationText);
        openDiaryButton = findViewById(R.id.openDiaryButton);
        profileButton = findViewById(R.id.profileButton);
        navDiary = findViewById(R.id.navDiary);
        navProfile = findViewById(R.id.navProfile);
        riskContainer = findViewById(R.id.riskContainer);
    }

    private void setupButtons() {
        View.OnClickListener openDiaryListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DiaryActivity.class));
            }
        };

        View.OnClickListener openProfileListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            }
        };

        openDiaryButton.setOnClickListener(openDiaryListener);
        navDiary.setOnClickListener(openDiaryListener);

        profileButton.setOnClickListener(openProfileListener);
        navProfile.setOnClickListener(openProfileListener);

        riskContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRiskInfoDialog();
            }
        });
    }

    private String buildWeatherUrl(String city) {
        try {
            String encodedCity = URLEncoder.encode(city, "UTF-8");

            return "https://api.openweathermap.org/data/2.5/weather?q="
                    + encodedCity
                    + "&appid="
                    + API_KEY
                    + "&units=metric&lang=ru";

        } catch (Exception e) {
            return "";
        }
    }

    private void updateWeather(final String city) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = buildWeatherUrl(city);
                final String response = loadWeatherData(url);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        parseWeatherResponse(response);
                    }
                });
            }
        }).start();
    }

    private String loadWeatherData(String urlString) {
        StringBuilder result = new StringBuilder();

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            BufferedReader reader;

            int responseCode = connection.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );
            } else {
                reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream())
                );
            }

            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            reader.close();
            connection.disconnect();

            System.out.println("OpenWeather response code: " + responseCode);
            System.out.println("OpenWeather response: " + result.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    private void parseWeatherResponse(String response) {
        try {
            if (response == null || response.length() == 0) {
                showDemoWeather("Пустой ответ от API");
                return;
            }

            JSONObject jsonObject = new JSONObject(response);

            if (!jsonObject.has("main")) {
                String message = jsonObject.has("message")
                        ? jsonObject.getString("message")
                        : "Неизвестная ошибка API";

                showDemoWeather("Ошибка API: " + message);
                return;
            }

            JSONObject main = jsonObject.getJSONObject("main");

            double temperature = main.getDouble("temp");

            int pressureHpa = main.getInt("pressure");
            int pressure = (int) Math.round(pressureHpa * 0.75006);

            int humidity = main.getInt("humidity");

            JSONArray weatherArray = jsonObject.getJSONArray("weather");
            String description = weatherArray.getJSONObject(0).getString("description");

            String cityName = jsonObject.getString("name");

            int riskScore = calculateRiskScore(pressure, humidity, temperature);

            String sensitivity = prefs.getString(
                    "sensitivity",
                    "Общая метеочувствительность"
            );

            riskScore = applyUserSensitivity(
                    riskScore,
                    sensitivity,
                    pressure,
                    humidity,
                    temperature
            );

            String riskLevel = getRiskLevel(riskScore);
            String recommendation = getRecommendation(riskLevel);

            saveLastWeather(temperature, pressure, humidity);

            showWeatherOnScreen(
                    cityName,
                    temperature,
                    pressure,
                    humidity,
                    description,
                    riskLevel,
                    recommendation
            );

            saveWeatherData(
                    cityName,
                    temperature,
                    pressure,
                    humidity,
                    description,
                    riskLevel
            );

            checkRiskAndShowWarning(riskLevel);

        } catch (Exception e) {
            e.printStackTrace();
            showDemoWeather("Ошибка обработки данных");
        }
    }

    private int calculateRiskScore(int pressure, int humidity, double temperature) {
        int riskScore = 0;

        if (pressure < 740 || pressure > 770) {
            riskScore += 2;
        }

        if (humidity >= 80) {
            riskScore += 1;
        }

        if (temperature < -10 || temperature > 30) {
            riskScore += 1;
        }

        return riskScore;
    }

    private int applyUserSensitivity(
            int riskScore,
            String sensitivityType,
            int pressure,
            int humidity,
            double temperature
    ) {
        if (sensitivityType.contains("давлению") && (pressure < 740 || pressure > 770)) {
            riskScore += 1;
        }

        if (sensitivityType.contains("влажности") && humidity >= 80) {
            riskScore += 1;
        }

        if (sensitivityType.contains("температуре") && (temperature < -10 || temperature > 30)) {
            riskScore += 1;
        }

        return riskScore;
    }

    private String getRiskLevel(int riskScore) {
        if (riskScore <= 1) {
            return "Низкий";
        } else if (riskScore <= 3) {
            return "Средний";
        } else {
            return "Высокий";
        }
    }

    private String getRecommendation(String riskLevel) {
        String name = prefs.getString("name", "Пользователь");

        if (riskLevel.equals("Низкий")) {
            return name + ", погодные условия стабильные. Серьёзных ограничений нет.";
        } else if (riskLevel.equals("Средний")) {
            return name + ", рекомендуется следить за самочувствием и избегать перегрузок.";
        } else {
            return name + ", рекомендуется снизить физическую активность и больше отдыхать.";
        }
    }

    private void showWeatherOnScreen(
            String city,
            double temperature,
            int pressure,
            int humidity,
            String description,
            String riskLevel,
            String recommendation
    ) {
        cityText.setText(city);
        temperatureText.setText(Math.round(temperature) + " °C");
        descriptionText.setText(makeFirstUpper(description));

        pressureText.setText("Давление: " + pressure + " мм рт. ст.");
        humidityText.setText("Влажность: " + humidity + "%");

        String descriptionLower = description.toLowerCase();

        if (descriptionLower.contains("ясно")) {
            weatherIconText.setText("☀");
        } else if (descriptionLower.contains("дожд")) {
            weatherIconText.setText("☔");
        } else if (descriptionLower.contains("снег")) {
            weatherIconText.setText("❄");
        } else {
            weatherIconText.setText("☁");
        }

        riskLevelText.setText(riskLevel + " риск");
        recommendationText.setText(recommendation);

        updateRiskView(riskLevel);
    }

    private String makeFirstUpper(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }

        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private void updateRiskView(String riskLevel) {
        if (riskLevel.equals("Низкий")) {
            riskContainer.setBackgroundResource(R.drawable.bg_risk_low);
            riskLevelText.setTextColor(Color.parseColor("#1B5E20"));
            riskShieldText.setTextColor(Color.parseColor("#1B5E20"));

        } else if (riskLevel.equals("Средний")) {
            riskContainer.setBackgroundResource(R.drawable.bg_risk_medium);
            riskLevelText.setTextColor(Color.parseColor("#795548"));
            riskShieldText.setTextColor(Color.parseColor("#795548"));

        } else {
            riskContainer.setBackgroundResource(R.drawable.bg_risk_high);
            riskLevelText.setTextColor(Color.parseColor("#B71C1C"));
            riskShieldText.setTextColor(Color.parseColor("#B71C1C"));
        }

        riskShieldText.setText("🛡");
    }

    private void saveLastWeather(double temperature, int pressure, int humidity) {
        prefs.edit()
                .putFloat("last_temperature", (float) temperature)
                .putInt("last_pressure", pressure)
                .putInt("last_humidity", humidity)
                .apply();
    }

    private void saveWeatherData(
            String city,
            double temperature,
            int pressure,
            int humidity,
            String description,
            String riskLevel
    ) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("city", city);
        values.put("temperature", temperature);
        values.put("pressure", pressure);
        values.put("humidity", humidity);
        values.put("description", description);
        values.put("risk_level", riskLevel);
        values.put("created_at", System.currentTimeMillis());

        db.insert("WeatherData", null, values);
    }

    private void checkRiskAndShowWarning(String riskLevel) {
        boolean warningsEnabled = prefs.getBoolean("warnings", true);

        if (warningsEnabled && riskLevel.equals("Высокий")) {
            showWarningMessage();
        }
    }

    private void showWarningMessage() {
        new AlertDialog.Builder(this)
                .setTitle("Предупреждение")
                .setMessage("Сегодня возможна повышенная метеочувствительность. Рекомендуется снизить нагрузку и следить за самочувствием.")
                .setPositiveButton("Понятно", null)
                .show();
    }

    private void showRiskInfoDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_risk_info, null);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Понятно", null)
                .show();
    }

    private void showDemoWeather(String errorText) {
        String cityName = prefs.getString("city", "Moscow");

        double temperature = 21;
        int pressure = 755;
        int humidity = 48;
        String description = "ясно";

        int riskScore = calculateRiskScore(pressure, humidity, temperature);

        String sensitivity = prefs.getString(
                "sensitivity",
                "Общая метеочувствительность"
        );

        riskScore = applyUserSensitivity(
                riskScore,
                sensitivity,
                pressure,
                humidity,
                temperature
        );

        String riskLevel = getRiskLevel(riskScore);
        String recommendation = getRecommendation(riskLevel);

        saveLastWeather(temperature, pressure, humidity);

        showWeatherOnScreen(
                cityName,
                temperature,
                pressure,
                humidity,
                description,
                riskLevel,
                recommendation
        );

        Toast.makeText(this, errorText + ". Показаны тестовые данные.", Toast.LENGTH_SHORT).show();
    }
}