package com.destroyer.rubikcubetimer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

public class SettingsActivity extends PreferenceActivity {

    Context context;
    String selectedFont;
    SharedPreferences.Editor prefEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName("appPreferences");
        addPreferencesFromResource(R.xml.activity_settings);
        prefEditor = getSharedPreferences("appPreferences", MODE_PRIVATE).edit();

        context = this;

        findPreference("clearDB").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(context)
                        .setTitle("Confirm")
                        .setMessage("Are you sure you want to clear the score database?")
                        .setCancelable(false)
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                new DBAdapter(context).open().removeAllScores().close();
                                Toast.makeText(context, "Cleared Scores", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            }
                        })
                        .show();
                return false;
            }
        });

        findPreference("timerFont").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final View convertView = LayoutInflater.from(context).inflate(R.layout.font_layout, null);
                ((TextView)convertView.findViewById(R.id.AtomicClockRadio)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/ATOMICCLOCKRADIO.otf"));
                ((TextView)convertView.findViewById(R.id.BaarPhilos)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/baarpbi_.otf"));
                ((TextView)convertView.findViewById(R.id.Delusion)).setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/DELUSION.otf"));

                final RadioGroup fontGroup = (RadioGroup) convertView.findViewById(R.id.fontsGroup);
                fontGroup.check(getResources().getIdentifier(getSharedPreferences("appPreferences", MODE_PRIVATE).getString("fontID", "AtomicClockRadio"), "id", context.getPackageName()));

                new AlertDialog.Builder(context)
                        .setTitle("Timer Fonts")
                        .setView(convertView)
                        .setCancelable(true)
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                            public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
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
    }

    public void onRadioClick(View view) {
        selectedFont = view.getTag().toString();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
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
