package com.example.meteoapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Calendar;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "meteo_app.db";
    private static final int DATABASE_VERSION = 2;

    public static final String CREATE_USER_TABLE =
            "CREATE TABLE IF NOT EXISTS User (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT, " +
                    "city TEXT, " +
                    "sensitivity_type TEXT" +
                    ");";

    public static final String CREATE_WEATHER_TABLE =
            "CREATE TABLE IF NOT EXISTS WeatherData (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "city TEXT, " +
                    "temperature REAL, " +
                    "pressure INTEGER, " +
                    "humidity INTEGER, " +
                    "description TEXT, " +
                    "risk_level TEXT, " +
                    "created_at INTEGER" +
                    ");";

    public static final String CREATE_DIARY_TABLE =
            "CREATE TABLE IF NOT EXISTS DiaryEntry (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "health_state TEXT, " +
                    "symptom TEXT, " +
                    "comment TEXT, " +
                    "pressure INTEGER, " +
                    "humidity INTEGER, " +
                    "temperature REAL, " +
                    "created_at INTEGER" +
                    ");";

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_WEATHER_TABLE);
        db.execSQL(CREATE_DIARY_TABLE);

        insertTestDiaryData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS User");
        db.execSQL("DROP TABLE IF EXISTS WeatherData");
        db.execSQL("DROP TABLE IF EXISTS DiaryEntry");
        onCreate(db);
    }

    private void insertTestDiaryData(SQLiteDatabase db) {
        insertDiaryEntry(
                db,
                "Плохое",
                "Головная боль, Усталость",
                "Головная боль усилилась к вечеру.",
                735,
                82,
                31,
                createDate(2026, 5, 13, 19, 10)
        );

        insertDiaryEntry(
                db,
                "Нормальное",
                "Усталость",
                "Небольшая слабость утром.",
                752,
                65,
                18,
                createDate(2026, 5, 14, 8, 40)
        );

        insertDiaryEntry(
                db,
                "Хорошее",
                "Нет симптомов",
                "Самочувствие стабильное.",
                755,
                48,
                21,
                createDate(2026, 5, 15, 9, 15)
        );

        insertDiaryEntry(
                db,
                "Плохое",
                "Головокружение, Скачки давления",
                "Самочувствие ухудшилось после прогулки.",
                778,
                76,
                29,
                createDate(2026, 5, 16, 18, 30)
        );

        insertDiaryEntry(
                db,
                "Плохое",
                "Сонливость, Боль в суставах",
                "Была сильная сонливость и тяжесть в голове.",
                738,
                88,
                10,
                createDate(2026, 5, 17, 7, 50)
        );
    }

    private void insertDiaryEntry(
            SQLiteDatabase db,
            String healthState,
            String symptom,
            String comment,
            int pressure,
            int humidity,
            double temperature,
            long createdAt
    ) {
        ContentValues values = new ContentValues();
        values.put("health_state", healthState);
        values.put("symptom", symptom);
        values.put("comment", comment);
        values.put("pressure", pressure);
        values.put("humidity", humidity);
        values.put("temperature", temperature);
        values.put("created_at", createdAt);

        db.insert("DiaryEntry", null, values);
    }

    private long createDate(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }
}