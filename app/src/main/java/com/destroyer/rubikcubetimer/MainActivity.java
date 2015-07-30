package com.destroyer.rubikcubetimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*TODO:
    stats viewer/editor (database)
 */

public class MainActivity extends Activity {

    protected static long WEEK_IN_MILLISECONDS = 648000000L;

    private UIStateMachine stateMachine;
    private SensorManager mSensorManager;

    private float[] gyroLast = {0, 0, 0};
    private float gyroThreshold;
    private boolean gyroSettled = false, isTrialVer = false;

    private Long appCreatedDate;

    private final SensorEventListener mSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE) {
                if (gyroSettled && (Math.abs(event.values[0]) - gyroLast[0] > gyroThreshold || Math.abs(event.values[1]) - gyroLast[1] > gyroThreshold || Math.abs(event.values[2]) - gyroLast[2] > gyroThreshold)) {
                    stateMachine.nextState();
                    mSensorManager.unregisterListener(mSensorListener);
                } else {
                    gyroLast[0] = Math.abs(event.values[0]);
                    gyroLast[1] = Math.abs(event.values[1]);
                    gyroLast[2] = Math.abs(event.values[2]);
                }
            } else if(event.sensor.getType()==Sensor.TYPE_PROXIMITY) {
                if (stateMachine.getState() == UIStateMachine.STATES.WAITING && event.values[0] < 1) {
                    stateMachine.nextState();
                } else if (stateMachine.getState() == UIStateMachine.STATES.READY && event.values[0] > 1) {
                    mSensorManager.unregisterListener(mSensorListener);
                    gyroSettled = false;
                    mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            gyroSettled = true;
                        }
                    }, 250);
                    stateMachine.nextState();
                } else if (stateMachine.getState() != UIStateMachine.STATES.WAITING && event.values[0] >= 1) {
                    stateMachine.haltProcess();
                    stateMachine.setState(UIStateMachine.STATES.WAITING);
                }
            } else Log.wtf("Sensor_Type", String.valueOf(event.sensor.getType()));
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (getActionBar() != null) getActionBar().hide();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        isTrialVersion();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getBoolean("firstRun", true)) {
            TextView howTo = new TextView(this);
            howTo.setTextSize(16);
            howTo.setTypeface(null, Typeface.NORMAL);
            howTo.setPadding(10,10,10,10);
            howTo.setText("Be sure device is on a hard surface for good detection!\n\n\n" +
                            "1.\tPress 'Start' Button\n\n" +
                            "2.\tPlace cube above dotted line when ready\n\n" +
                            "3.\tWait for count down timer to ready\n\n" +
                            "4.\tLift cube to begin timer\n\n" +
                            "5.\tWhen solve is complete, 'slap' table with hands the end timer!"
            );
            new AlertDialog.Builder(this)
                    .setTitle("First Run")
                    .setView(howTo)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            getSharedPreferences("appPreferences", Context.MODE_PRIVATE).edit().putBoolean("firstRun", false).apply();
                            dialog.dismiss();
                        }
                    }).show();
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        gyroThreshold = Float.valueOf(getSharedPreferences("appPreferences", MODE_PRIVATE).getString("gyroThreshold", "18")) / 1000;
        stateMachine = new UIStateMachine(this, displayMetrics.ydpi, findViewById(R.id.bkgGlow),
                findViewById(R.id.startResetBtn), findViewById(R.id.cube), findViewById(R.id.dottedLine), findViewById(R.id.statsTxt),
                findViewById(R.id.timerTxt), findViewById(R.id.bestTimeTxt), findViewById(R.id.worstTimeText), findViewById(R.id.last5Txt),
                findViewById(R.id.last12Txt),findViewById(R.id.last25Txt), findViewById(R.id.last50Txt), findViewById(R.id.monthTxt),
                findViewById(R.id.lastTimeTxt), findViewById(R.id.scrambleTxt));
        stateMachine.resetState();
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mSensorListener);
        stateMachine.haltProcess();
        super.onPause();
    }

    private void isTrialVersion() {
        Log.wtf("DEVICE_ID", ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId());
        final Context ctx = this;

        Parse.initialize(this, "UMpeJeqOQFoiNMxTX0SozYgs2hobmX2YY4Mh7tuv", "7bWn5dI3JiWnKvrdE4Xd5sCOlLYY5UO9eO1WZA7x");

        ParseQuery<ParseObject> query = ParseQuery.getQuery("trialVersion");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                    if (e == null) {
                        if (list.isEmpty()) { // device ID not in DB (DB empty)
                            // add device to DB
                            ParseObject trialVersion = new ParseObject("trialVersion");
                            trialVersion.put("deviceId", ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId());
                            trialVersion.put("millis", System.currentTimeMillis());
                            trialVersion.saveInBackground();
                            Log.wtf("PARSE", "empty DB, added device to cloud DB");
                        } else{ // found device ID
                            // find matching device ID
                            List<Integer> foundID = new ArrayList<>();
                            for (ParseObject obj : list) {
                                if (obj.getString("deviceId").equals(((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId())) {
                                    foundID.add(list.indexOf(obj));
                                }
                            }

                            if (foundID.isEmpty()) { // device ID not in DB
                                // add device to DB
                                ParseObject trialVersion = new ParseObject("trialVersion");
                                trialVersion.put("deviceId", ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId());
                                trialVersion.put("millis", System.currentTimeMillis());
                                trialVersion.saveInBackground();
                                Log.wtf("PARSE", "added device to cloud DB");
                                return;
                            }
                            if (foundID.size() > 1) { // found multiple matching IDs (should not happen)
                                Log.wtf("PARSE", "found " + foundID + " devices with mathcing ID");
                                return;
                            }
                            appCreatedDate = list.get(foundID.get(0)).getLong("millis");
                            Log.wtf("PARSE", "found device id: " + list.get(foundID.get(0)).getString("deviceId"));

                            Log.wtf("temp", String.valueOf(appCreatedDate));
                            isTrialVer = System.currentTimeMillis() - appCreatedDate < WEEK_IN_MILLISECONDS;
                            Log.wtf("TRIAL", String.valueOf(isTrialVer));

                            if (isTrialVer) {
                                Log.w("time1", String.valueOf(WEEK_IN_MILLISECONDS));
                                Log.w("time2", String.valueOf((System.currentTimeMillis() - appCreatedDate)));
                                long millis = (WEEK_IN_MILLISECONDS - (System.currentTimeMillis() - appCreatedDate));
                                Log.w("timeMillis", String.valueOf(millis));
                                new AlertDialog.Builder(ctx)
                                        .setTitle("Trial Version")
                                        .setMessage("You have " + (millis / (1000*60*60*24)) + " days left on your trial version.")
                                        .setCancelable(false)
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                                dialog.dismiss();
                                            }
                                        }).show();
                            }
                        }
                    } else {
                        Log.wtf("PARSE_EXCEPTION", e.getMessage());
                    }
            }
        });
    }

    public void onButtonClick(View view) {
        switch (stateMachine.getState()) {
            case START:
                mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_FASTEST);
                break;

            case WAITING: case HOLDING: case READY:
                gyroSettled = false;
                mSensorManager.unregisterListener(mSensorListener);
                stateMachine.resetState();

            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_about:
                Toast.makeText(this, "About", Toast.LENGTH_LONG).show();
                break;

            case R.id.action_help:
                Toast.makeText(this, "Help", Toast.LENGTH_LONG).show();
                break;

            case R.id.action_db:
                startActivity(new Intent(this, DBViewerActivity.class));
//                Toast.makeText(this, "DB Viewer/Editor", Toast.LENGTH_LONG).show();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
