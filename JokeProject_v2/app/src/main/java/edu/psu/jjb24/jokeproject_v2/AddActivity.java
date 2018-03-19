package edu.psu.jjb24.jokeproject_v2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class AddActivity extends AppCompatActivity {
    public static int RESULT_DB_CHANGED = 1;
    public static int RESULT_DB_ADDED_RECORD = 2;
    public static int RESULT_DB_UNCHANGED = 3;

    private SQLiteDatabase db;

    private long rowid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        rowid = getIntent().getLongExtra("rowid", -1);

        JokeDB.getInstance(this).getWritableDatabase(new JokeDB.OnDBReadyListener() {
            @Override
            public void onDBReady(SQLiteDatabase theDB) {
                db = theDB;
                if (rowid != -1) {
                    loadJoke(rowid);
                }
            }
        });
        setSupportActionBar((Toolbar) findViewById(R.id.my_toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_add, menu);
        if (rowid == -1) {
            menu.getItem(0).setIcon(R.drawable.ic_cancel);
            menu.getItem(0).setTitle(R.string.menu_cancel);
            setTitle("Add joke");
        }
        else {
            setTitle("Edit joke");
        }
        return true;
    }

    private void loadJoke(long rowid) {
        String where = "_id = " + rowid;
        String[] projection = {"_id", "title", "setup","punchline","liked"};
        Cursor cursor = db.query("jokes", projection, where,
                null, null, null, null);
        if (cursor.moveToFirst()) {
            // Arguments are a way to set values for the dialog that will be passed
            // even if fragment gets destroyed and recreated
            ((EditText) findViewById(R.id.txtEditTitle)).setText(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            ((EditText) findViewById(R.id.txtEditSetup)).setText(cursor.getString(cursor.getColumnIndexOrThrow("setup")));
            ((EditText) findViewById(R.id.txtEditPunchline)).setText(
                    cursor.getString(cursor.getColumnIndexOrThrow("punchline")));
        }
        else {
            Toast.makeText(AddActivity.this, "Record could not be retrieved...", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (db == null) {
            Toast.makeText(this,"Try again in a few seconds.",Toast.LENGTH_LONG).show();
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_save:
                addRecord();
                return true;
            case R.id.menu_delete:
                if (rowid != -1) {
                    ConfirmDeleteDialog confirmDialog = new ConfirmDeleteDialog();
                    confirmDialog.show(getFragmentManager(), "deletionConfirmation");
                }
                else {
                    setResult(RESULT_DB_UNCHANGED);
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

   private void addRecord() {
       ContentValues values = new ContentValues();
       values.put("title",
                ((EditText) findViewById(R.id.txtEditTitle)).getText().toString());
       values.put("setup",
                ((EditText) findViewById(R.id.txtEditSetup)).getText().toString());
       values.put("punchline",
                ((EditText) findViewById(R.id.txtEditPunchline)).getText().toString());

        try {
            if (rowid == -1) {
                values.put("liked", "?");
                db.insert("jokes",null,values);
                setResult(RESULT_DB_ADDED_RECORD);
            } else {
                String where = "_id = " + rowid;
                db.update("jokes", values, where,null);
                setResult(RESULT_DB_CHANGED);
            }

            finish(); // Quit activity
        } catch (SQLException e) {
            Toast.makeText(this,"Error updating database.",Toast.LENGTH_LONG).show();
        }
    }


    public void deleteRecord() {
        String where = "_id = " + rowid;
        db.delete("jokes", where, null);
    }

    public static class ConfirmDeleteDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle("Delete the joke?")
                    .setMessage("You will not be able to undo the deletion!")
                    .setPositiveButton("Delete",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    try {
                                        ((AddActivity) getActivity()).deleteRecord();
                                        getActivity().setResult(RESULT_DB_CHANGED);
                                        getActivity().finish();
                                    } catch (SQLException e) {
                                        Toast.makeText(getActivity(),
                                                "Error deleting record.",
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            })
                    .setNegativeButton("Return to joke list",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    getActivity().setResult(RESULT_DB_UNCHANGED);
                                    getActivity().finish();
                                }
                            });
            return builder.create();
        }
    }
}
