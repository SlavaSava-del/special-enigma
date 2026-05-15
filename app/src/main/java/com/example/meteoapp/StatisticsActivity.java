package com.example.meteoapp;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class StatisticsActivity extends Activity {

    private TextView backTitle;
    private TextView mainResultText;
    private TextView badDaysText;
    private SharedPreferences prefs;

    private TextView pressurePercentText;
    private TextView humidityPercentText;
    private TextView temperaturePercentText;

    private FrameLayout pressureBar;
    private FrameLayout humidityBar;
    private FrameLayout temperatureBar;

    private View pressureFill;
    private View humidityFill;
    private View temperatureFill;

    private TextView statRecommendationText;
    private TextView doneButton;

    private AppDatabaseHelper dbHelper;

    private static final int COLOR_PRESSURE = Color.parseColor("#43A047");
    private static final int COLOR_HUMIDITY = Color.parseColor("#1E88E5");
    private static final int COLOR_TEMPERATURE = Color.parseColor("#E53935");
    private static final int COLOR_BAR_BG = Color.parseColor("#E0E0E0");

    private static class DiaryEntry {
        private final String healthState;
        private final int pressure;
        private final int humidity;
        private final double temperature;

        public DiaryEntry(String healthState, int pressure, int humidity, double temperature) {
            this.healthState = healthState;
            this.pressure = pressure;
            this.humidity = humidity;
            this.temperature = temperature;
        }

        public String getHealthState() {
            return healthState;
        }

        public int getPressure() {
            return pressure;
        }

        public int getHumidity() {
            return humidity;
        }

        public double getTemperature() {
            return temperature;
        }
    }

    private static class StatisticResult {
        String title;
        String recommendation;

        StatisticResult(String title, String recommendation) {
            this.title = title;
            this.recommendation = recommendation;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        dbHelper = new AppDatabaseHelper(this);
        prefs = getSharedPreferences("meteo_profile", MODE_PRIVATE);

        initViews();
        setupButtons();
        setupBars();
        updateStatistics();
    }

    private void initViews() {
        backTitle = findViewById(R.id.backTitle);
        mainResultText = findViewById(R.id.mainResultText);
        badDaysText = findViewById(R.id.badDaysText);

        pressurePercentText = findViewById(R.id.pressurePercentText);
        humidityPercentText = findViewById(R.id.humidityPercentText);
        temperaturePercentText = findViewById(R.id.temperaturePercentText);

        pressureBar = findViewById(R.id.pressureBar);
        humidityBar = findViewById(R.id.humidityBar);
        temperatureBar = findViewById(R.id.temperatureBar);

        pressureFill = findViewById(R.id.pressureFill);
        humidityFill = findViewById(R.id.humidityFill);
        temperatureFill = findViewById(R.id.temperatureFill);

        statRecommendationText = findViewById(R.id.statRecommendationText);
        doneButton = findViewById(R.id.doneButton);
    }

    private void setupButtons() {
        backTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void setupBars() {
        pressureBar.setBackground(createRoundedBackground(COLOR_BAR_BG));
        humidityBar.setBackground(createRoundedBackground(COLOR_BAR_BG));
        temperatureBar.setBackground(createRoundedBackground(COLOR_BAR_BG));

        pressureFill.setBackground(createRoundedBackground(COLOR_PRESSURE));
        humidityFill.setBackground(createRoundedBackground(COLOR_HUMIDITY));
        temperatureFill.setBackground(createRoundedBackground(COLOR_TEMPERATURE));
    }

    private void updateStatistics() {
        List<DiaryEntry> entries = loadDiaryEntries();

        int totalBadDays = countBadDays(entries);

        int pressureMatches = countBadPressureMatches(entries);
        int humidityMatches = countHighHumidityMatches(entries);
        int temperatureMatches = countTemperatureMatches(entries);

        int pressurePercent = calculateMatchPercent(pressureMatches, totalBadDays);
        int humidityPercent = calculateMatchPercent(humidityMatches, totalBadDays);
        int temperaturePercent = calculateMatchPercent(temperatureMatches, totalBadDays);

        pressurePercentText.setText(pressurePercent + "%");
        humidityPercentText.setText(humidityPercent + "%");
        temperaturePercentText.setText(temperaturePercent + "%");

        setBarProgress(pressureBar, pressureFill, pressurePercent);
        setBarProgress(humidityBar, humidityFill, humidityPercent);
        setBarProgress(temperatureBar, temperatureFill, temperaturePercent);

        badDaysText.setText("Записей с плохим самочувствием: " + totalBadDays);

        if (totalBadDays == 0) {
            mainResultText.setText("Пока нет записей с плохим самочувствием. Добавьте несколько записей в дневник, чтобы приложение смогло выполнить анализ.");
            String name = prefs.getString("name", "Пользователь");
            statRecommendationText.setText(name + ", для более точной статистики отмечайте самочувствие в разные дни и при разных погодных условиях.");
            return;
        }

        StatisticResult result = getMainStatisticResult(
                pressurePercent,
                humidityPercent,
                temperaturePercent
        );

        mainResultText.setText(result.title);
        statRecommendationText.setText(result.recommendation);
    }

    private void setBarProgress(final FrameLayout bar, final View fill, final int percent) {
        bar.post(new Runnable() {
            @Override
            public void run() {
                int barWidth = bar.getWidth();

                int minWidth = dp(28);
                int fillWidth = (barWidth * percent) / 100;

                if (percent > 0 && fillWidth < minWidth) {
                    fillWidth = minWidth;
                }

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        fillWidth,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );

                fill.setLayoutParams(params);
            }
        });
    }

    private List<DiaryEntry> loadDiaryEntries() {
        List<DiaryEntry> entries = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                "DiaryEntry",
                null,
                null,
                null,
                null,
                null,
                "created_at DESC"
        );

        while (cursor.moveToNext()) {
            String healthState = cursor.getString(cursor.getColumnIndexOrThrow("health_state"));
            int pressure = cursor.getInt(cursor.getColumnIndexOrThrow("pressure"));
            int humidity = cursor.getInt(cursor.getColumnIndexOrThrow("humidity"));
            double temperature = cursor.getDouble(cursor.getColumnIndexOrThrow("temperature"));

            entries.add(new DiaryEntry(healthState, pressure, humidity, temperature));
        }

        cursor.close();

        return entries;
    }

    private int countBadDays(List<DiaryEntry> entries) {
        int count = 0;

        for (DiaryEntry entry : entries) {
            if (entry.getHealthState().equals("Плохое")) {
                count++;
            }
        }

        return count;
    }

    private int countBadPressureMatches(List<DiaryEntry> entries) {
        int matches = 0;

        for (DiaryEntry entry : entries) {
            boolean badHealth = entry.getHealthState().equals("Плохое");
            boolean badPressure = entry.getPressure() < 740 || entry.getPressure() > 770;

            if (badHealth && badPressure) {
                matches++;
            }
        }

        return matches;
    }

    private int countHighHumidityMatches(List<DiaryEntry> entries) {
        int matches = 0;

        for (DiaryEntry entry : entries) {
            boolean badHealth = entry.getHealthState().equals("Плохое");
            boolean highHumidity = entry.getHumidity() > 80;

            if (badHealth && highHumidity) {
                matches++;
            }
        }

        return matches;
    }

    private int countTemperatureMatches(List<DiaryEntry> entries) {
        int matches = 0;

        for (DiaryEntry entry : entries) {
            boolean badHealth = entry.getHealthState().equals("Плохое");
            boolean badTemperature = entry.getTemperature() < -10 || entry.getTemperature() > 30;

            if (badHealth && badTemperature) {
                matches++;
            }
        }

        return matches;
    }

    private int calculateMatchPercent(int matches, int totalBadDays) {
        if (totalBadDays == 0) {
            return 0;
        }

        return (matches * 100) / totalBadDays;
    }

    private StatisticResult getMainStatisticResult(
            int pressurePercent,
            int humidityPercent,
            int temperaturePercent
    ) {
        if (pressurePercent == 0 && humidityPercent == 0 && temperaturePercent == 0) {
            String name = prefs.getString("name", "Пользователь");

            return new StatisticResult(
                    "Пока не найдено устойчивой связи между погодой и плохим самочувствием.",
                    name + ", продолжайте вести дневник. Чем больше записей, тем точнее будет анализ."
            );
        }

        if (pressurePercent >= humidityPercent && pressurePercent >= temperaturePercent) {
            return buildPressureResult(pressurePercent);
        }

        if (humidityPercent >= pressurePercent && humidityPercent >= temperaturePercent) {
            return buildHumidityResult(humidityPercent);
        }

        return buildTemperatureResult(temperaturePercent);
    }

    private StatisticResult buildPressureResult(int percent) {
        String name = prefs.getString("name", "Пользователь");

        return new StatisticResult(
                "Основной возможный триггер — атмосферное давление. В " + percent + "% случаев плохое самочувствие совпадало с неблагоприятным давлением.",
                name + ", рекомендуется внимательнее следить за давлением в дни погодных изменений и снижать нагрузку при ухудшении самочувствия."
        );
    }

    private StatisticResult buildHumidityResult(int percent) {
        String name = prefs.getString("name", "Пользователь");

        return new StatisticResult(
                "Основной возможный триггер — высокая влажность. В " + percent + "% случаев плохое самочувствие совпадало с повышенной влажностью.",
                name + ", рекомендуется избегать духоты, чаще проветривать помещение и пить достаточное количество воды."
        );
    }

    private StatisticResult buildTemperatureResult(int percent) {
        String name = prefs.getString("name", "Пользователь");

        return new StatisticResult(
                "Основной возможный триггер — некомфортная температура. В " + percent + "% случаев плохое самочувствие совпадало с жарой или холодом.",
                name + ", рекомендуется одеваться по погоде, избегать перегрева или переохлаждения и снижать физическую активность при дискомфорте."
        );
    }

    private GradientDrawable createRoundedBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(8));
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}