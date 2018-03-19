package edu.psu.jjb24.jokeproject_v2;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private SQLiteDatabase db;
    private SimpleCursorAdapter adapter;
    private boolean filtered = false;  // Are results filtered by likes
    private final static int ADD_ACTIVITY_RESULT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        filtered = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_filter),false);

        if (savedInstanceState != null)
            filtered = savedInstanceState.getBoolean("filtered");
        onCreateSetupJokeListAdapter();

        onCreateSetupJokeListView();

        setSupportActionBar((Toolbar) findViewById(R.id.my_toolbar));

        JokeDB.getInstance(this).asyncWritableDatabase(new JokeDB.OnDBReadyListener() {
            @Override
            public void onDBReady(SQLiteDatabase theDB) {
                db = theDB;
                dbAsyncLoadCursor(false);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);

        if (filtered) {
            menu.getItem(1).setIcon(R.drawable.ic_thumbs_up_down);
        }
        else {
            menu.getItem(1).setIcon(R.drawable.ic_thumb_up);
        }

        return true;
    }

    /**
     * Initialize the adapter that is bound to the listview
     */
    private void onCreateSetupJokeListAdapter() {
        // Initially set cursor to null BC IT IS NOT YET READY!
        adapter = new SimpleCursorAdapter(this, R.layout.list_item, null,
                new String[]{"title", "liked"},
                new int[]{R.id.txtTitle, R.id.imgLiked}, 0);

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == 2) {
                    switch (cursor.getString(cursor.getColumnIndex("liked"))) {
                        case "Y":
                            ((ImageView) view).setImageResource(
                                    R.drawable.ic_thumb_up);
                            view.setTag("Y");
                            break;
                        case "N":
                            ((ImageView) view).setImageResource(
                                    R.drawable.ic_thumb_down);
                            view.setTag("N");
                            break;
                        default:
                            ((ImageView) view).setImageResource(
                                    R.drawable.ic_thumbs_up_down);
                            view.setTag("?");
                    }

                    final long rowid = cursor.getLong(cursor.getColumnIndex("_id"));
                    view.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View v) {
                            toggleImage(rowid, (ImageView) v);
                        }
                    });
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Toggle the image that is displayed in the Liked image view, when
     * the user clicks on this, update the database, and reset the cursor
     *
     * @param rowid     the row id from the database
     * @param imageView the view holding the liked image
     */
    private void toggleImage(long rowid, ImageView imageView) {
        String newVal;
        if (imageView.getTag().equals("Y")) {
            newVal = "N";
        } else if (imageView.getTag().equals("N")) {
            newVal = "?";
        } else {
            newVal = "Y";
        }
        imageView.setTag(newVal);

        update(rowid, newVal);
    }

    /**
     * Bind adapter to view and set the onclick listeners
     */
    private void onCreateSetupJokeListView() {
        ListView listView = findViewById(R.id.lstJokes);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                displayJoke(id);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                           long id) {
                Intent intent = new Intent(MainActivity.this, AddActivity.class);
                intent.putExtra("rowid", id);
                startActivityForResult(intent,ADD_ACTIVITY_RESULT);
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_ACTIVITY_RESULT  && resultCode != AddActivity.RESULT_DB_UNCHANGED) {
            dbAsyncLoadCursor(resultCode == AddActivity.RESULT_DB_ADDED_RECORD);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                startActivityForResult(new Intent(this, AddActivity.class),ADD_ACTIVITY_RESULT);
                return true;
            case R.id.menu_filter:
                filtered = !filtered;
                if (filtered) {
                    item.setIcon(R.drawable.ic_thumbs_up_down);
                }
                else {
                    item.setIcon(R.drawable.ic_thumb_up);
                }
                dbAsyncLoadCursor(false);
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("filtered", filtered);
    }

    @SuppressLint("StaticFieldLeak")
    private void dbAsyncLoadCursor(boolean scrollToEnd) {

        new AsyncTask<Boolean, Void, Cursor>() {
            boolean scrollToEnd;
            @Override
            protected Cursor doInBackground(Boolean... params) {
                scrollToEnd = params[0];
                String where = null;
                if (filtered) {
                    where = "liked = 'Y'";
                }
                String[] projection = {"_id", "title", "liked"};
                return db.query("jokes", projection, where,
                        null, null, null, null);
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                adapter.swapCursor(cursor);
                if (scrollToEnd)
                    ((ListView) findViewById(R.id.lstJokes)).setSelection(adapter.getCount() - 1);
            }
        }.execute(scrollToEnd);
    }

    private void displayJoke(long rowid) {
        String where = "_id = " + rowid;

        String[] projection = {"_id", "title", "setup", "punchline", "liked"};
        Cursor cursor = db.query("jokes", projection, where,
                null, null, null, null);
        if (cursor.moveToFirst()) {
            // Arguments are a way to set values for the dialog that will be passed
            // even if fragment gets destroyed and recreated
            Bundle args = new Bundle();
            args.putLong("rowid", rowid);
            args.putString("title",cursor.getString(cursor.getColumnIndexOrThrow("title")));
            args.putString("setup",cursor.getString(cursor.getColumnIndexOrThrow("setup")));
            args.putString("punchline",cursor.getString(cursor.getColumnIndexOrThrow("punchline")));

            DisplaySetupDialog setupDialog = new DisplaySetupDialog();
            setupDialog.setArguments(args);
            setupDialog.show(getFragmentManager(), "setupDialog");
        }
        else {
            Toast.makeText(MainActivity.this, "Record could not be retrieved...", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
    }

    private void update(long rowid, String liked) {
        ContentValues values = new ContentValues();
        values.put("liked", liked);
        String where = "_id = " + rowid;

        int count = 0;
        try {
            count = db.update("jokes", values, where, null);
        } catch (SQLException e) {
            Log.e("JokeDB", e.getMessage());
        }
        if (count == 0) {
            Toast.makeText(MainActivity.this, "Error updating record.", Toast.LENGTH_LONG).show();
        } else {
            dbAsyncLoadCursor(false);
        }
    }

    public static class DisplaySetupDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final long rowid = getArguments().getLong("rowid");
            final String title = getArguments().getString("title");
            final String setup = getArguments().getString("setup");
            final String punchline = getArguments().getString("punchline");

            builder.setTitle(title)
                    .setMessage(setup)
                    .setPositiveButton("Punchline",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    DisplayPunchlineDialog punchlineDialog =
                                            new DisplayPunchlineDialog();

                                    Bundle args = new Bundle();
                                    args.putLong("rowid", rowid);
                                    args.putString("title", title);
                                    args.putString("punchline", punchline);
                                    punchlineDialog.setArguments(args);

                                    punchlineDialog.show(getFragmentManager(),
                                            "punchlineDialog");
                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });

            return builder.create();
        }
    }

    public static class DisplayPunchlineDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final long rowid = getArguments().getLong("rowid");
            String title = getArguments().getString("title");
            String punchline = getArguments().getString("punchline");

            builder.setTitle(title)
                    .setMessage(punchline)
                    .setPositiveButton("Like", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ((MainActivity) getActivity()).update(rowid, "Y");
                        }
                    })
                    .setNegativeButton("Dislike",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    ((MainActivity) getActivity()).update(rowid, "N");
                                }
                            })
                    .setNeutralButton("Decide later",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });

            return builder.create();
        }
    }
}
