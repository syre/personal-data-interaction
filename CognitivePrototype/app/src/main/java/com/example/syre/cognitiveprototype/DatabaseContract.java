package com.example.syre.cognitiveprototype;

import android.provider.BaseColumns;

/**
 * Created by syre on 2/14/15.
 */
public final class DatabaseContract {
    private static final String TEXT_TYPE          = " TEXT";
    private static final String INTEGER_TYPE       = " INTEGER";
    private static final String COMMA_SEP          = ",";
    private static final String DATETIME = " DATETIME DEFAULT CURRENT_TIMESTAMP";

    public DatabaseContract() {}
    public static abstract class ScoresTable implements BaseColumns {
        public static final String TABLE_NAME = "scores";
        public static final String COLUMN_NAME_COL1 = "score";
        public static final String COLUMN_NAME_COL2 = "failed_number";
        public static final String COLUMN_NAME_COL3 = "created_at";
        public static final String COLUMN_NAME_COL4 = "score_integer_list";

        public static final String SQL_CREATE_ENTRIES = "CREATE TABLE " +
                TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME_COL1 + INTEGER_TYPE + COMMA_SEP +
                COLUMN_NAME_COL2 + INTEGER_TYPE + COMMA_SEP +
                COLUMN_NAME_COL3 + DATETIME + COMMA_SEP +
                COLUMN_NAME_COL4 + TEXT_TYPE + " )";

        public static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    }
