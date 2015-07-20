package com.destroyer.rubikcubetimer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

/*TODO:
    stats viewer/editor (database)
 */

public class MainActivity extends Activity {

    private UIStateMachine stateMachine;
    private SensorManager mSensorManager;

    private float[] gyroLast = {0, 0, 0};
    private float gyroThreshold;
    private boolean gyroSettled = false;

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

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        stateMachine = new UIStateMachine(this, displayMetrics.widthPixels, displayMetrics.ydpi, findViewById(R.id.bkgGlow), findViewById(R.id.startResetBtn),
                findViewById(R.id.dottedLine), findViewById(R.id.cube), findViewById(R.id.statsTxt), findViewById(R.id.timerTxt), findViewById(R.id.bkgMain));
        stateMachine.addState(UIStateMachine.STATES.START, UIStateMachine.STATES.WAITING, R.drawable.glow_start_rainbow, R.drawable.btn_start);
        stateMachine.addState(UIStateMachine.STATES.WAITING, UIStateMachine.STATES.HOLDING, R.drawable.btn_waiting, R.drawable.btn_waiting);
        stateMachine.addState(UIStateMachine.STATES.HOLDING, UIStateMachine.STATES.READY, R.drawable.glow_holding, R.drawable.btn_holding);
        stateMachine.addState(UIStateMachine.STATES.READY, UIStateMachine.STATES.RUNNING, R.drawable.glow_ready, R.drawable.btn_ready);
        stateMachine.addState(UIStateMachine.STATES.RUNNING, UIStateMachine.STATES.STOPPING, R.drawable.glow_running, R.drawable.btn_running);
        stateMachine.addState(UIStateMachine.STATES.STOPPING, UIStateMachine.STATES.START, R.drawable.glow_finished, R.drawable.btn_finished);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gyroThreshold = Float.valueOf(getSharedPreferences("appPreferences", MODE_PRIVATE).getString("gyroThreshold", "18")) / 1000;
        stateMachine.resetState();
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
