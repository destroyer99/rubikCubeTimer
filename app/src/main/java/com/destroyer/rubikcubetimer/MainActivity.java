package com.destroyer.rubikcubetimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

/* TODO:
 * audio jack pwm sender/reader
 * ads
 * fix gyro auto calibration (buggy on S4)
 */

public class MainActivity extends Activity {

    protected static long WEEK_IN_MILLISECONDS = 648000000L;

    private SharedPreferences prefs;
    private UIStateMachine stateMachine;
    private SensorManager mSensorManager;
    private ExternalPadAdapter epa;

    private float[] gyroLast = {0, 0, 0};
    private float[] gyroThreshold = {0, 0, 0};
    private boolean gyroSettled = false;
    private boolean doubleBackToExitPressedOnce = false;

    private final SensorEventListener mSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE) {
                if (gyroSettled && (Math.abs(event.values[0]) - gyroLast[0] > gyroThreshold[0] || Math.abs(event.values[1]) - gyroLast[1] > gyroThreshold[1] || Math.abs(event.values[2]) - gyroLast[2] > gyroThreshold[2])) {
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

        prefs = getSharedPreferences("appPreferences", Context.MODE_PRIVATE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        epa = new ExternalPadAdapter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefs.getBoolean("firstRun", true)) {
            prefs.edit().putBoolean("trialVersion", false).apply();
            TextView howTo = new TextView(this);
            howTo.setTextSize(16);
            howTo.setTypeface(null, Typeface.NORMAL);
            howTo.setPadding(10,10,10,10);
            howTo.setText("Be sure device is on a hard surface for good detection! (ex. counter top or desk)\n\n\n" +
                            "1.\tAfter you've scrambled your cube, press the 'Start' button\n\n" +
                            "2.\tAfter your inspection time is up, place the cube above the dotted line\n\n" +
                            "3.\tWait 3 seconds for timer to initialize\n\n" +
                            "4.\tLift the cube to start the timer\n\n" +
                            "5.\tWhen solve is complete, 'slap' the table with your hands to end the timer!\n\n" +
                            "**Hint** - You might have to adjust the 'Slap Sensitivity' in settings if your phone isn't detecting the vibration of your hands.\n" +
                            "The lower the number, the more sensitive it is"

            );
            new AlertDialog.Builder(this)
                    .setTitle("How to use this app:")
                    .setView(howTo)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            prefs.edit().putBoolean("firstRun", false).apply();
                            dialog.dismiss();
                        }
                    }).show();
        } else {
            if (prefs.getBoolean("paidVersion", false)) {
                // TODO: do paid version stuff
            } else {
                long appCreatedDate = prefs.getLong("trialVersionX", -1L);

                if (appCreatedDate == -1) {
                    // TODO: cannot get pref
                } else if ((System.currentTimeMillis() - appCreatedDate) < WEEK_IN_MILLISECONDS) {
                    long millisLeft = (WEEK_IN_MILLISECONDS - (System.currentTimeMillis() - appCreatedDate));
                    new AlertDialog.Builder(this)
                            .setTitle("Trial Version")
                            .setMessage("You have " + (millisLeft / (1000*60*60*24)) + " days left on your trial version.")
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                    dialog.dismiss();
                                }
                            }).show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Trial Version")
                            .setMessage("Trial Version Expired, give us your money")
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                    dialog.dismiss();
                                }
                            }).show();
                }
            }

        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

//        gyroThreshold = Float.valueOf(getSharedPreferences("appPreferences", MODE_PRIVATE).getString("gyroThreshold", "25")) / 1000;
        String[] gyroVals = getSharedPreferences("appPreferences", MODE_PRIVATE).getString("gyroThreshold", "0.025;0.025;0.025").split(";");
        gyroThreshold[0] = Float.valueOf(gyroVals[0]);
        gyroThreshold[1] = Float.valueOf(gyroVals[1]);
        gyroThreshold[2] = Float.valueOf(gyroVals[2]);

        stateMachine = new UIStateMachine(this, displayMetrics.ydpi, findViewById(R.id.bkgGlow),
                findViewById(R.id.startResetBtn), findViewById(R.id.cube), findViewById(R.id.dottedLine), findViewById(R.id.statsTxt),
                findViewById(R.id.timerTxt), findViewById(R.id.bestTimeTxt), findViewById(R.id.worstTimeText), findViewById(R.id.last5Txt),
                findViewById(R.id.last12Txt),findViewById(R.id.last25Txt), findViewById(R.id.last50Txt), findViewById(R.id.monthTxt),
                findViewById(R.id.lastTimeTxt), findViewById(R.id.scrambleTxt));
        stateMachine.resetState();

        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.isWiredHeadsetOn()) {
            Log.wtf("AUDIO", "Wired headset detected");
        }
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mSensorListener);
        stateMachine.haltProcess();
        super.onPause();
    }

    public void onButtonClick(View view) {
        switch (stateMachine.getState()) {
            case START:
                mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_FASTEST);
//                epa.stop();
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
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
        } else if (stateMachine.getState() != UIStateMachine.STATES.START) {
            mSensorManager.unregisterListener(mSensorListener);
            stateMachine.resetState();
        } else {
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(getApplicationContext(), "Press BACK again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
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
//                epa.logReadBuffer();
//                epa.playTone();
                break;

            case R.id.action_help:
                Toast.makeText(this, "Help", Toast.LENGTH_LONG).show();
//                epa.start();
                break;

            case R.id.action_db:
                startActivity(new Intent(this, DBViewerActivity.class));
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
