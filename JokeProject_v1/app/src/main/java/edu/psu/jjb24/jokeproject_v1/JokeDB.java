package edu.psu.jjb24.jokeproject_v1;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

public class JokeDB extends SQLiteOpenHelper {
    interface OnDBReadyListener {
        void onDBReady(SQLiteDatabase db);
    }

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "joke.db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE jokes (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "setup TEXT, " +
                    "punchline TEXT);";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS jokes";


    private static JokeDB theDb;

    // Q:  How do we implement the singleton pattern?
    private JokeDB(Context context) {
        super(context.getApplicationContext(),DATABASE_NAME,null,DATABASE_VERSION);
    }

    public static synchronized JokeDB getInstance(Context context) {
        if (theDb == null) {
            theDb = new JokeDB(context);
        }
        return theDb;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }


    // Q: How should we call theDb.getWritableDatabase() ?  On the UI thread?
    // Use an AsyncTask
    // Q: How do we tell the caller that it is ready? Via a callback
    public void getWritableDatabase(OnDBReadyListener listener) {
        new OpenDbAsyncTask().execute(listener);
    }

    private static class OpenDbAsyncTask extends AsyncTask<OnDBReadyListener,Void,SQLiteDatabase> {
        OnDBReadyListener listener;

        @Override
        protected SQLiteDatabase doInBackground(OnDBReadyListener... params){
            listener = params[0];
            return JokeDB.theDb.getWritableDatabase();
        }

        @Override
        protected void onPostExecute(SQLiteDatabase db) {
            //Make that callback
            listener.onDBReady(db);
        }
    }


}

