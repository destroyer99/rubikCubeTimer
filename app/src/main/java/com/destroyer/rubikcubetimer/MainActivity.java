package com.destroyer.rubikcubetimer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private enum STATE {START, WAIT, HOLD, TIMING, STOP, RESET}

    private float gyroThreshold;
    private int timerPrecision;

    private SharedPreferences appPrefs;
    private SensorManager mSensorManager;
    DBAdapter db = new DBAdapter(this);
    Vibrator vibrator;
    private float[] gyroLast = {0, 0, 0};
    boolean runTimer = false;
    TextView timerTxt, statsTxt;
    long init, millis;
    boolean cubeInProx = false;
    boolean cubeInProx1 = false;
    boolean cubeInProx2 = false;
    boolean cubeOnDev = false;
    boolean gyroSettled = false;

    private final Runnable timer = new Runnable() {
        @Override
        public void run() {
            if(runTimer) {
                millis = System.currentTimeMillis()-init;
                timerTxt.setText(formatString(millis));
                new Handler().postDelayed(this, timerPrecision);
            }
        }
    };

    private final SensorEventListener mSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE) {
                if (gyroSettled && (Math.abs(event.values[0]) - gyroLast[0] > gyroThreshold || Math.abs(event.values[1]) - gyroLast[1] > gyroThreshold || Math.abs(event.values[2]) - gyroLast[2] > gyroThreshold)) {
                    stateResolver(STATE.STOP);
                    db.open();
                    Log.wtf("DB_ADDED_SCORE", String.valueOf(db.addTime(System.currentTimeMillis(), millis)));
                    db.close();
                } else {
                    gyroLast[0] = Math.abs(event.values[0]);
                    gyroLast[1] = Math.abs(event.values[1]);
                    gyroLast[2] = Math.abs(event.values[2]);
                }
            } else if(event.sensor.getType()==Sensor.TYPE_PROXIMITY) {
                if (!cubeOnDev && event.values[0] == 0) {
                    stateResolver(STATE.HOLD);
                } else if (cubeOnDev && event.values[0] > 1) {
                    stateResolver(STATE.TIMING);
                    runTimer = true;
                    init = System.currentTimeMillis();
                    new Handler().post(timer);
                } else if (event.values[0] > 1) {
                    stateResolver(STATE.WAIT);
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

        appPrefs = getSharedPreferences("appPreferences", MODE_PRIVATE);

        timerTxt = (TextView) findViewById(R.id.timerTxt);
        statsTxt = (TextView) findViewById(R.id.statsTxt);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        stateResolver(STATE.START);
    }

    @Override
    protected void onResume() {
        super.onResume();
        timerPrecision = Integer.valueOf(appPrefs.getString("timerPrecision", "21"));
        gyroThreshold = Float.valueOf(appPrefs.getString("gyroThreshold", "18")) / 1000;
        updateStats();
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mSensorListener);
        super.onPause();
    }

    public void onButtonClick(View view) {
        switch (view.getId()) {
            case R.id.startResetBtn:
                if(((Button)view).getText().equals("Start")) {
                    stateResolver(STATE.WAIT);
                } else if(((Button)view).getText().equals("Reset")) {
                    stateResolver(STATE.RESET);
                }
                break;

            default:
                break;
        }
    }

    private void updateStats() {
        int avg = 0, low = Integer.MAX_VALUE, high = Integer.MIN_VALUE, val;
        db.open();
        Cursor cursor;
        if ((cursor = db.getAllTimes()) != null && cursor.moveToFirst()) {
            do {
                val = cursor.getInt(1);
                avg += val;
                if (val > high) high = val;
                if (val < low) low = val;
            } while (cursor.moveToNext());

            avg = avg / cursor.getCount();

            statsTxt.setText("Average Score: " + formatString(avg) +
                                "\nFastest Score: " + formatString(low) +
                                "\nLast Score: " + (cursor.moveToFirst() ? formatString(cursor.getInt(1)) : formatString(0)));
        } else statsTxt.setText("");
    }

    private void setColors(int color) {
//        findViewById(R.id.startResetBtn).setBackgroundColor(color);
        findViewById(R.id.background).setBackgroundColor(color);
    }

    private void stateResolver(STATE state) {
        switch (state) {
            case START:
                timerTxt.setTextSize(22);
                timerTxt.setText("Press Start to begin...");
                runTimer = cubeInProx = cubeInProx1 = cubeInProx2  = cubeOnDev = gyroSettled = false;
                setColors(Color.WHITE);
                updateStats();
                ((Button)findViewById(R.id.startResetBtn)).setText("Start");
                break;
            case WAIT:
                timerTxt.setTextSize(22);
                mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_FASTEST);
                runTimer = cubeInProx = cubeInProx1 = cubeInProx2  = cubeOnDev = gyroSettled = false;
                setColors(Color.YELLOW);
                timerTxt.setText("Waiting for rubik's cube...");
                ((Button)findViewById(R.id.startResetBtn)).setText("Reset");
                break;
            case HOLD:
                cubeInProx = true;
                setColors(Color.BLUE);
                vibrator.vibrate(250);
                timerTxt.setText("Holding cube 3...");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (cubeInProx) {
                            vibrator.vibrate(250);
                            timerTxt.setText("Holding cube 2...");
                            cubeInProx1 = true;
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (cubeInProx1) {
                                        vibrator.vibrate(250);
                                        timerTxt.setText("Holding cube 1...");
                                        cubeInProx2 = true;
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (cubeInProx2) {
                                                    cubeOnDev = true;
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            setColors(Color.CYAN);
                                                            timerTxt.setText("Lift cube to begin timer!!!");
                                                        }
                                                    });
                                                }
                                            }
                                        }, 1000);
                                    }
                                }
                            }, 1000);
                        }
                    }
                }, 1000);
                break;
            case TIMING:
                timerTxt.setTextSize(50);
                gyroSettled = cubeInProx = cubeInProx1 = cubeInProx2 = cubeOnDev = false;
                mSensorManager.unregisterListener(mSensorListener);
                mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        gyroSettled = true;
                    }
                }, 250);
                setColors(Color.GREEN);
                ((Button)findViewById(R.id.startResetBtn)).setText("Stop");
                findViewById(R.id.startResetBtn).setClickable(false);
                timerTxt.setText("");
                break;
            case STOP:
                mSensorManager.unregisterListener(mSensorListener);
                runTimer = cubeInProx = cubeInProx1 = cubeInProx2  = cubeOnDev = gyroSettled = false;
                ((Button)findViewById(R.id.startResetBtn)).setText("Start");
                setColors(Color.RED);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (((Button)findViewById(R.id.startResetBtn)).getText().equals("Start")) {
                            setColors(Color.WHITE);
                        }
                    }
                }, 2000);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.startResetBtn).setClickable(true);
                            }
                        });
                    }
                }, 250);
                break;
            case RESET:
                mSensorManager.unregisterListener(mSensorListener);
                runTimer = cubeInProx = cubeInProx1 = cubeInProx2  = cubeOnDev = gyroSettled = false;
                ((Button)findViewById(R.id.startResetBtn)).setText("Start");
                timerTxt.setText("Press Start to begin...");
                setColors(Color.WHITE);
                break;
        }
    }

    private String formatString(long millis) {
        return String.format("%d:%02d:%03d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
                TimeUnit.MILLISECONDS.toMillis(millis) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis)));
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
