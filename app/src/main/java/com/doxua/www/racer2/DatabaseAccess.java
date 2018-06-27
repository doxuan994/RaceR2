package com.doxua.www.racer2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseAccess {

    private SQLiteOpenHelper openHelper;
    private SQLiteDatabase db;

    private static DatabaseAccess instance;
    Cursor c = null;

    private DatabaseAccess(Context context) {
        this.openHelper = new DatabaseOpenHelper(context);
    }

    public static DatabaseAccess getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseAccess(context);
        }
        return instance;
    }

    public void open() {
        this.db = openHelper.getWritableDatabase();
    }

    public void close() {
        if (db != null) {
            this.db.close();
        }
    }

    public String getInformation(int id) {

        c = db.rawQuery("select Information from Celebrities where ID = '" + id + "'", new String[]{});
        StringBuffer buffer = new StringBuffer();

        while (c.moveToNext()) {
            String information = c.getString(0);
            buffer.append("" + information);
        }
        return buffer.toString();
    }

}
