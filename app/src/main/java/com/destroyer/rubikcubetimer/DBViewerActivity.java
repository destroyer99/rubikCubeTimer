package com.destroyer.rubikcubetimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.destroyer.rubikcubetimer.DBAdapter.DB_KEYS;

public class DBViewerActivity extends Activity {

  private ListView listView;
  private DBAdapter db;
  private String[] scramble;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_dbviewer);

    final Context context = this;
    db = new DBAdapter(this);

    listView = (ListView) findViewById(R.id.listView);

    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(context, scramble[position], Toast.LENGTH_LONG).show();
      }
    });

    listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
        new AlertDialog.Builder(context)
            .setTitle("Confirm")
            .setMessage("Are you sure you want to delete this score?")
            .setCancelable(false)
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
              public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                dialog.cancel();
              }
            })
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                db.open();
                if (!db.removeTime(Integer.valueOf(view.getTag().toString()))) {
                  Toast.makeText(getApplicationContext(), "Delete Failed", Toast.LENGTH_LONG).show();
                }
                db.close();
                populateList();
                dialog.dismiss();
              }
            })
            .show();
        return true;
      }
    });

    populateList();
  }

  private void populateList() {
    db.open();
    Cursor cursor = db.getAllTimes();

    if (cursor != null && cursor.getCount() > 0) {
      TimeItem[] timeItems = new TimeItem[cursor.getCount()];
      scramble = new String[cursor.getCount()];
      if (cursor.moveToFirst()) {
        do {
          timeItems[cursor.getPosition()] = new TimeItem(cursor.getInt(DB_KEYS.KEY_ID.ordinal()),
              cursor.getLong(DB_KEYS.KEY_DATE.ordinal()), cursor.getLong(DB_KEYS.KEY_SOLVE_TIME.ordinal()));
          scramble[cursor.getPosition()] = cursor.getString(DB_KEYS.KEY_SCRAMBLE.ordinal());
        } while (cursor.moveToNext());
      } else Log.wtf("TIME_LIST", "EMPTY");
      listView.setAdapter(new TimeListAdapter(this, R.layout.time_layout, timeItems));
    } else {
      listView.setAdapter(null);
    }
    db.close();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_dbviewer, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
