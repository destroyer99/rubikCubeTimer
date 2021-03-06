package com.destroyer.rubikcubetimer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter
{

  static final String TAG = "DBAdapter";
  static final String KEY_ID = "id";//0
  static final String KEY_DATE = "date";//1
  static final String KEY_SOLVE_TIME = "solveTime";//2
  static final String KEY_SESSION_ID = "sessionId";//3
  static final String KEY_SCRAMBLE = "scramble";//4
  static final String DATABASE_NAME = "rubik";
  static final String TABLE_NAME = "rubik_times";
  static final int DATABASE_VERSION = 1;
  static final String TABLE_RUBIK_CREATE = "CREATE TABLE rubik_times (" +
      "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
      "date INTEGER, " +
      "solveTime INTEGER, " +
      "sessionId INTEGER, " +
      "scramble TEXT" +
      ");";
  final Context context;
  DatabaseHelper DBHelper;
  SQLiteDatabase db;

  public DBAdapter(Context ctx)
  {
    this.context = ctx;
    DBHelper = new DatabaseHelper(context);
  }

  public DBAdapter open() throws SQLException
  {
    db = DBHelper.getWritableDatabase();
    return this;
  }

  public void close()
  {
    DBHelper.close();
  }

  // Input is a SQL statement that will be executed
  public void performExec(String query)
  {
    db.execSQL(query);
  }

  public boolean addTime(long date, long solveTime, long sessionId, String scramble)
  {
    ContentValues contentValues = new ContentValues();
    contentValues.put(KEY_DATE, date);
    contentValues.put(KEY_SOLVE_TIME, solveTime);
    contentValues.put(KEY_SESSION_ID, sessionId);
    contentValues.put(KEY_SCRAMBLE, scramble);

    return db.insert(TABLE_NAME, null, contentValues) > 0;
  }

  public Cursor getTime(int id)
  {
    return null;
  }

  /////////////////  Create  /////////////////////

  public Cursor getAllByMonth(String selection)
  {
    return db.query(true, TABLE_NAME, new String[]{KEY_ID, KEY_DATE, KEY_SOLVE_TIME, KEY_SESSION_ID, KEY_SCRAMBLE}, KEY_DATE + ">" + selection, null, null, null, null, null);
  }

  /////////////////  Read  /////////////////////

  public Cursor getAllTimes()
  {
    return db.query(true, TABLE_NAME, new String[]{KEY_ID, KEY_DATE, KEY_SOLVE_TIME, KEY_SESSION_ID, KEY_SCRAMBLE}, null, null, null, null, KEY_DATE + " DESC", null);
  }

  public Cursor getAllTimes(String sort)
  {
    return db.query(true, TABLE_NAME, new String[]{KEY_ID, KEY_DATE, KEY_SOLVE_TIME, KEY_SESSION_ID, KEY_SCRAMBLE}, null, null, null, null, KEY_DATE + " " + sort, null);
  }

  public Cursor getAllTimesBySession(long sessionId)
  {
    return db.query(true, TABLE_NAME, new String[]{KEY_ID, KEY_DATE, KEY_SOLVE_TIME, KEY_SESSION_ID, KEY_SCRAMBLE}, KEY_SESSION_ID + "=" + sessionId, null, null, null, null, null);
  }

  public boolean removeAllTimes()
  {
    return (db.delete(TABLE_NAME, null, null) > 0);
  }

  public DBAdapter removeAllScores()
  {
    return ((db.delete(TABLE_NAME, null, null) > 0) ? this : null);
  }


  /////////////////  Update  /////////////////////

  /////////////////  Delete  /////////////////////

  public boolean removeTime(int id)
  {
    return db.delete(TABLE_NAME, KEY_ID + "=" + id, null) > 0;
  }

  protected enum DB_KEYS
  {
    KEY_ID, KEY_DATE, KEY_SOLVE_TIME, KEY_SESSION_ID, KEY_SCRAMBLE
  }

  private static class DatabaseHelper extends SQLiteOpenHelper
  {

    DatabaseHelper(Context context)
    {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
      try
      {
        db.execSQL(TABLE_RUBIK_CREATE);
      } catch (SQLException e)
      {
        e.printStackTrace();
      }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
      Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy old data");
      db.execSQL("DROP TABLE IF EXISTS rubik_times");
      onCreate(db);
    }
  }
}


