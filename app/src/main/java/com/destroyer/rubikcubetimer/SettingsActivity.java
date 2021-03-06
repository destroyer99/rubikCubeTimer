package com.destroyer.rubikcubetimer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity
{

  private Context context;
  private SharedPreferences.Editor prefEditor;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onStart()
  {
    super.onStart();

    getPreferenceManager().setSharedPreferencesName("appPreferences");
    addPreferencesFromResource(R.xml.activity_settings);
    prefEditor = getSharedPreferences("appPreferences", MODE_PRIVATE).edit();

    context = this;

    findPreference("clearDB").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
    {
      @Override
      public boolean onPreferenceClick(Preference preference)
      {
        new AlertDialog.Builder(context)
            .setTitle("Confirm")
            .setMessage("Are you sure you want to clear the score database?")
            .setCancelable(false)
            .setNegativeButton("No", new DialogInterface.OnClickListener()
            {
              public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id)
              {
                dialog.cancel();
              }
            })
            .setPositiveButton("Yes", new DialogInterface.OnClickListener()
            {
              public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id)
              {
                new DBAdapter(context).open().removeAllScores().close();
                Toast.makeText(context, "Cleared Scores", Toast.LENGTH_LONG).show();
                dialog.dismiss();
              }
            })
            .show();
        return false;
      }
    });

    findPreference("timerFont").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
    {
      @Override
      public boolean onPreferenceClick(Preference preference)
      {
        final View convertView = LayoutInflater.from(context).inflate(R.layout.font_layout, null);
        ((TextView) convertView.findViewById(R.id.AtomicClockRadio)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/ATOMICCLOCKRADIO.otf"));
        ((TextView) convertView.findViewById(R.id.ChickenScratch)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/ChickenScratch.otf"));
        ((TextView) convertView.findViewById(R.id.ComicBook)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/ComicBook.otf"));
        ((TextView) convertView.findViewById(R.id.Delusion)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Delusion.otf"));
        ((TextView) convertView.findViewById(R.id.Eroded)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/RetroErosion.otf"));
        ((TextView) convertView.findViewById(R.id.Earthquake)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Erthqake.otf"));
        ((TextView) convertView.findViewById(R.id.GhettoMarquee)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/ghettomarquee.otf"));
        ((TextView) convertView.findViewById(R.id.HemiHead426)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/HemiHead426.otf"));
        ((TextView) convertView.findViewById(R.id.Jerrybuilt)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Jerrybuilt.otf"));
        ((TextView) convertView.findViewById(R.id.LetsGoDigital)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/LetsgoDigital.otf"));
        ((TextView) convertView.findViewById(R.id.RuggedRide)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/RuggedRide.otf"));
        ((TextView) convertView.findViewById(R.id.See)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/See.otf"));

        final RadioGroup fontGroup = (RadioGroup) convertView.findViewById(R.id.fontsGroup);
        fontGroup.check(getResources().getIdentifier(getSharedPreferences("appPreferences", MODE_PRIVATE).getString("fontID", "AtomicClockRadio"), "id", context.getPackageName()));

        new AlertDialog.Builder(context)
            .setTitle("Timer Fonts")
            .setView(convertView)
            .setCancelable(true)
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener()
            {
              public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id)
              {
                dialog.cancel();
              }
            })
            .setPositiveButton("Apply", new DialogInterface.OnClickListener()
            {
              public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id)
              {
                prefEditor.putString("timerFont", convertView.findViewById(fontGroup.getCheckedRadioButtonId()).getTag().toString());
                prefEditor.putString("fontID", String.valueOf(convertView.findViewById(fontGroup.getCheckedRadioButtonId()).getId()));
                prefEditor.commit();
                dialog.dismiss();
              }
            })
            .show();
        return false;
      }
    });

    findPreference("gyroThresholdAuto").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
    {

      @Override
      public boolean onPreferenceClick(Preference preference)
      {
        startActivity(new Intent(context, GyroCalibrationActivity.class));
        return false;
      }
    });

    findPreference("paidVersionSet").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
    {

      @Override
      public boolean onPreferenceClick(Preference preference)
      {
        prefEditor.putBoolean("paidVersion", true).commit();
        return false;
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_settings, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings)
    {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
