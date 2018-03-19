package edu.psu.jjb24.jokeproject_v2;


import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

public class JokeDB extends SQLiteOpenHelper {

    interface OnDBReadyListener {
        void onDBReady(SQLiteDatabase theDB);
    }

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "joke.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE jokes (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT, " +
                    "setup TEXT, " +
                    "punchline TEXT," +
                    "liked TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS jokes";

    private static JokeDB theDb;
    private Context appContext;

    private JokeDB(Context context) {
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
        appContext = context.getApplicationContext();
    }

    public static synchronized JokeDB getInstance(Context context) {
        if (theDb == null) {
            theDb = new JokeDB(context.getApplicationContext());
        }
        return theDb;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);

        // Note:  We start by populating the db with arrays
        String[] titles =
                appContext.getResources().getStringArray(R.array.JokeTitle);
        String[] setups =
                appContext.getResources().getStringArray(R.array.JokeSetup);
        String[] punchlines =
                appContext.getResources().getStringArray(R.array.JokePunchline);

        // Q:  Why are we wrapping these operations in a transaction?
        db.beginTransaction();
        ContentValues values = new ContentValues();
        values.put("liked","?");
        for (int i = 0; i < setups.length; i++) {
            values.put("title", titles[i]);
            values.put("setup", setups[i]);
            values.put("punchline", punchlines[i]);
            values.put("liked", "?");
            db.insert("jokes", null, values);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
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

    public void asyncWritableDatabase(OnDBReadyListener listener) {
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
