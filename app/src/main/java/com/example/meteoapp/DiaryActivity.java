package com.example.meteoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DiaryActivity extends Activity {

    private LinearLayout entriesContainer;
    private TextView addDiaryButton;
    private TextView navHome;
    private TextView navProfile;

    private AppDatabaseHelper dbHelper;

    private final int COLOR_GOOD = Color.parseColor("#1B5E20");
    private final int COLOR_GOOD_BG = Color.parseColor("#E3F5E6");

    private final int COLOR_NORMAL = Color.parseColor("#795548");
    private final int COLOR_NORMAL_BG = Color.parseColor("#FFF3B8");

    private final int COLOR_BAD = Color.parseColor("#B71C1C");
    private final int COLOR_BAD_BG = Color.parseColor("#FFD9D9");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary);

        dbHelper = new AppDatabaseHelper(this);

        entriesContainer = findViewById(R.id.entriesContainer);
        addDiaryButton = findViewById(R.id.addDiaryButton);
        navHome = findViewById(R.id.navHome);
        navProfile = findViewById(R.id.navProfile);

        setupButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDiaryEntries();
    }

    private void setupButtons() {
        addDiaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DiaryActivity.this, AddDiaryActivity.class));
            }
        });

        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DiaryActivity.this, MainActivity.class));
                finish();
            }
        });

        navProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DiaryActivity.this, ProfileActivity.class));
                finish();
            }
        });
    }

    private void loadDiaryEntries() {
        entriesContainer.removeAllViews();

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

        if (cursor.getCount() == 0) {
            showEmptyDiary();
        }

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            String healthState = cursor.getString(cursor.getColumnIndexOrThrow("health_state"));
            String symptom = cursor.getString(cursor.getColumnIndexOrThrow("symptom"));
            String comment = cursor.getString(cursor.getColumnIndexOrThrow("comment"));
            int pressure = cursor.getInt(cursor.getColumnIndexOrThrow("pressure"));
            int humidity = cursor.getInt(cursor.getColumnIndexOrThrow("humidity"));
            double temperature = cursor.getDouble(cursor.getColumnIndexOrThrow("temperature"));
            long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));

            addEntryCard(id, healthState, symptom, comment, pressure, humidity, temperature, createdAt);
        }

        cursor.close();
    }

    private void showEmptyDiary() {
        LinearLayout emptyCard = new LinearLayout(this);
        emptyCard.setOrientation(LinearLayout.VERTICAL);
        emptyCard.setPadding(dp(20), dp(24), dp(20), dp(24));
        emptyCard.setBackground(createRoundedBackground(Color.WHITE, Color.parseColor("#E0E0E0")));
        emptyCard.setElevation(dp(2));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(12), 0, dp(16));
        emptyCard.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText("Записей пока нет");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#222222"));

        TextView subtitle = new TextView(this);
        subtitle.setText("Нажмите кнопку «+», чтобы добавить первое самочувствие.");
        subtitle.setTextSize(18);
        subtitle.setTextColor(Color.parseColor("#666666"));
        subtitle.setPadding(0, dp(10), 0, 0);

        emptyCard.addView(title);
        emptyCard.addView(subtitle);

        entriesContainer.addView(emptyCard);
    }

    private void addEntryCard(
            final int id,
            String healthState,
            String symptom,
            String comment,
            int pressure,
            int humidity,
            double temperature,
            long createdAt
    ) {
        LinearLayout outerCard = new LinearLayout(this);
        outerCard.setOrientation(LinearLayout.HORIZONTAL);
        outerCard.setBackground(createRoundedBackground(Color.WHITE, Color.parseColor("#E0E0E0")));
        outerCard.setElevation(dp(3));

        LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        outerParams.setMargins(0, 0, 0, dp(16));
        outerCard.setLayoutParams(outerParams);

        View colorLine = new View(this);
        LinearLayout.LayoutParams colorLineParams = new LinearLayout.LayoutParams(
                dp(6),
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        colorLine.setLayoutParams(colorLineParams);
        colorLine.setBackgroundColor(getStateColor(healthState));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        content.setLayoutParams(contentParams);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView dateText = new TextView(this);
        dateText.setText(formatDate(createdAt));
        dateText.setTextSize(18);
        dateText.setTextColor(Color.parseColor("#222222"));
        dateText.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        dateText.setLayoutParams(dateParams);

        TextView stateBadge = new TextView(this);
        stateBadge.setText(healthState);
        stateBadge.setTextSize(15);
        stateBadge.setTextColor(getStateColor(healthState));
        stateBadge.setTypeface(null, Typeface.BOLD);
        stateBadge.setGravity(Gravity.CENTER);
        stateBadge.setPadding(dp(12), dp(6), dp(12), dp(6));
        stateBadge.setBackground(createRoundedBackground(getStateBackgroundColor(healthState), Color.TRANSPARENT));

        topRow.addView(dateText);
        topRow.addView(stateBadge);

        View divider = new View(this);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        dividerParams.setMargins(0, dp(12), 0, dp(12));
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(Color.parseColor("#E6E6E6"));

        TextView symptomLabel = createSmallLabel("Симптом");
        TextView symptomText = createMainText(symptom);

        TextView commentLabel = createSmallLabel("Комментарий");
        TextView commentText = createMainText(comment == null || comment.trim().length() == 0
                ? "Без комментария"
                : comment);

        LinearLayout weatherBox = new LinearLayout(this);
        weatherBox.setOrientation(LinearLayout.VERTICAL);
        weatherBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        weatherBox.setBackground(createRoundedBackground(Color.parseColor("#F5F6FA"), Color.TRANSPARENT));

        LinearLayout.LayoutParams weatherBoxParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        weatherBoxParams.setMargins(0, dp(12), 0, dp(10));
        weatherBox.setLayoutParams(weatherBoxParams);

        TextView weatherTitle = new TextView(this);
        weatherTitle.setText("Погодные данные на момент записи");
        weatherTitle.setTextSize(15);
        weatherTitle.setTextColor(Color.parseColor("#666666"));
        weatherTitle.setTypeface(null, Typeface.BOLD);

        TextView weatherInfo = new TextView(this);
        weatherInfo.setText(
                "Температура: " + Math.round(temperature) + " °C\n" +
                        "Давление: " + pressure + " мм рт. ст.\n" +
                        "Влажность: " + humidity + "%"
        );
        weatherInfo.setTextSize(16);
        weatherInfo.setTextColor(Color.parseColor("#222222"));
        weatherInfo.setPadding(0, dp(6), 0, 0);

        weatherBox.addView(weatherTitle);
        weatherBox.addView(weatherInfo);

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView deleteButton = new TextView(this);
        deleteButton.setText("Удалить");
        deleteButton.setTextSize(16);
        deleteButton.setTextColor(COLOR_BAD);
        deleteButton.setTypeface(null, Typeface.BOLD);
        deleteButton.setPadding(dp(12), dp(8), dp(12), dp(8));
        deleteButton.setBackground(createRoundedBackground(Color.parseColor("#FDECEC"), Color.TRANSPARENT));

        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        deleteParams.gravity = Gravity.END;

        TextView spacer = new TextView(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        spacer.setLayoutParams(spacerParams);

        bottomRow.addView(spacer);
        bottomRow.addView(deleteButton, deleteParams);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmDeleteEntry(id);
            }
        });

        content.addView(topRow);
        content.addView(divider);
        content.addView(symptomLabel);
        content.addView(symptomText);
        content.addView(commentLabel);
        content.addView(commentText);
        content.addView(weatherBox);
        content.addView(bottomRow);

        outerCard.addView(colorLine);
        outerCard.addView(content);

        entriesContainer.addView(outerCard);
    }

    private TextView createSmallLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(14);
        label.setTextColor(Color.parseColor("#777777"));
        label.setTypeface(null, Typeface.BOLD);
        label.setPadding(0, dp(4), 0, dp(2));
        return label;
    }

    private TextView createMainText(String text) {
        TextView value = new TextView(this);
        value.setText(text);
        value.setTextSize(18);
        value.setTextColor(Color.parseColor("#222222"));
        value.setPadding(0, 0, 0, dp(8));
        return value;
    }

    private int getStateColor(String healthState) {
        if (healthState.equals("Хорошее")) {
            return COLOR_GOOD;
        }

        if (healthState.equals("Нормальное")) {
            return COLOR_NORMAL;
        }

        return COLOR_BAD;
    }

    private int getStateBackgroundColor(String healthState) {
        if (healthState.equals("Хорошее")) {
            return COLOR_GOOD_BG;
        }

        if (healthState.equals("Нормальное")) {
            return COLOR_NORMAL_BG;
        }

        return COLOR_BAD_BG;
    }

    private void confirmDeleteEntry(final int id) {
        new AlertDialog.Builder(this)
                .setTitle("Удаление записи")
                .setMessage("Удалить эту запись из дневника?")
                .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        deleteEntry(id);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteEntry(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.delete(
                "DiaryEntry",
                "id = ?",
                new String[]{String.valueOf(id)}
        );

        loadDiaryEntries();
    }

    private String formatDate(long time) {
        SimpleDateFormat format = new SimpleDateFormat("dd MMMM, HH:mm", new Locale("ru"));
        return format.format(new Date(time));
    }

    private GradientDrawable createRoundedBackground(int fillColor, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(12));

        if (strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(dp(1), strokeColor);
        }

        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}