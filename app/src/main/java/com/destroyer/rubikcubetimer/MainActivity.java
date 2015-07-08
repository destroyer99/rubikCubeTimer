package com.destroyer.rubikcubetimer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private final static float gryoThreshold = 0.018f;

    private SensorManager mSensorManager;
    private float[] gyroLast = {0, 0, 0};
    boolean runTimer = false;
    EditText timerTxt;
    long init, millis;
    boolean cubeInProx = false;
    boolean cubeOnDev = false;

    private final Runnable timer = new Runnable() {
        @Override
        public void run() {
            if(runTimer) {
                millis = System.currentTimeMillis()-init;
                timerTxt.setText(String.format("%d:%02d:%03d",
                        TimeUnit.MILLISECONDS.toMinutes(millis),
                        TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
                        TimeUnit.MILLISECONDS.toMillis(millis) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis))));
                new Handler().postDelayed(this, 99);
            }
        }
    };

    private final SensorEventListener mSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE) {
//                Log.wtf("GYRO_DELTA", "{x, y, z}: {" + (Math.abs(event.values[0]) - gyroLast[0]) + ", " + (Math.abs(event.values[1]) - gyroLast[1]) + ", " + (Math.abs(event.values[2]) - gyroLast[2]) + "}");
                if (Math.abs(event.values[0]) - gyroLast[0] > gryoThreshold || Math.abs(event.values[1]) - gyroLast[1] > gryoThreshold || Math.abs(event.values[2]) - gyroLast[2] > gryoThreshold) {
                    runTimer = false;
                    mSensorManager.unregisterListener(mSensorListener);
                    ((Button) findViewById(R.id.startTimerButton)).setBackgroundColor(Color.RED);
                    Log.wtf("SENSORLISTENER", "STOPPED");
                    DBAdapter db = new DBAdapter(getApplicationContext());
                    db.open();
                    db.addTime(System.currentTimeMillis(), millis);
                    db.close();
                } else {
                    gyroLast[0] = Math.abs(event.values[0]);
                    gyroLast[1] = Math.abs(event.values[1]);
                    gyroLast[2] = Math.abs(event.values[2]);
                }
            } else if(event.sensor.getType()==Sensor.TYPE_PROXIMITY) {
                if (!cubeOnDev && event.values[0] == 0) {
                    cubeInProx = true;
                    ((Button) findViewById(R.id.startTimerButton)).setBackgroundColor(Color.BLUE);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (cubeInProx) {
                                cubeOnDev = true;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((Button) findViewById(R.id.startTimerButton)).setBackgroundColor(Color.CYAN);
                                    }
                                });
                            }
                        }
                    }, 2000);
                } else if (cubeOnDev && event.values[0] > 1) {
                    cubeInProx = cubeOnDev = false;
                    mSensorManager.unregisterListener(mSensorListener);
                    mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
                    ((Button)findViewById(R.id.startTimerButton)).setBackgroundColor(Color.GREEN);
                    runTimer = true;
                    init = System.currentTimeMillis();
                    new Handler().post(timer);
                } else if (event.values[0] > 1) {
                    cubeInProx = cubeOnDev = false;
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

        timerTxt = (EditText) findViewById(R.id.editText);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        Log.wtf("SENSORS", mSensorManager.getSensorList(Sensor.TYPE_ALL).toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mSensorListener);
        super.onPause();
    }

    public void onButtonClick(View view) {
        switch (view.getId()) {
            case R.id.startTimerButton:
                ((Button)findViewById(R.id.startTimerButton)).setBackgroundColor(Color.YELLOW);
                mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_FASTEST);
                break;

            case R.id.stopTimerButton:
                ((Button)findViewById(R.id.startTimerButton)).setBackgroundColor(Color.TRANSPARENT);
                cubeInProx = cubeOnDev = runTimer = false;
                mSensorManager.unregisterListener(mSensorListener);
                DBAdapter db = new DBAdapter(this);
                db.open();
//                db.performExec("DROP TABLE IF EXISTS rubik_times");
                Log.wtf("DB", db.getAllTimes().toString());
                db.close();
                break;

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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
