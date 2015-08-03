package com.destroyer.rubikcubetimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GyroCalibrationActivity extends Activity {

    private static final float[] gyroBuffer = {2f, 2f, 1.5f};

    private int val = 10;
    private float[] gyroVal = {0, 0, 0};
    private long sensorCount = 0;

    private CountDownTimer cdt;
    private SharedPreferences.Editor prefEditor;
    private TextView txtView;
    private Button btnStartSave;
    private SensorManager mSensorManager;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gyro_calibration);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        prefEditor = getSharedPreferences("appPreferences", MODE_PRIVATE).edit();

        txtView = (TextView) findViewById(R.id.txtView);
        Button btnCancel = (Button) findViewById(R.id.btnCancel);
        btnStartSave = (Button) findViewById(R.id.btnStartSave);

        txtView.setText("Leave still for " + String.valueOf(val) + " seconds");

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSensorManager.unregisterListener(mSensorListener);
                cdt.cancel();
                finish();
            }
        });

        btnStartSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((Button)v).getText().equals("Start")) {
                    mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
                    btnStartSave.setText("Save");
                    btnStartSave.setEnabled(false);
                    cdt.start();
                } else {
                    prefEditor.putString("gyroThreshold", gyroBuffer[0] * gyroVal[0] / sensorCount + ";" + gyroBuffer[1] * gyroVal[1] / sensorCount + ";" + gyroBuffer[2] * gyroVal[2] / sensorCount);
                    prefEditor.commit();
                    finish();
                }
            }
        });

        cdt = new CountDownTimer(val*1000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (millisUntilFinished < val*1000) {
                    txtView.setText("Leave still for " + String.valueOf(val--) + " seconds");
                }
            }

            @Override
            public void onFinish() {
                mSensorManager.unregisterListener(mSensorListener);
                txtView.setText("Complete!");
                btnStartSave.setEnabled(true);
                Log.wtf("CALIBRATION_RESULTS", "[0]:" + gyroVal[0] / sensorCount);
                Log.wtf("CALIBRATION_RESULTS", "[1]:" + gyroVal[1] / sensorCount);
                Log.wtf("CALIBRATION_RESULTS", "[2]:" + gyroVal[2] / sensorCount);
                Log.wtf("GYRO_THRESHOLD_AUTO", gyroBuffer[0] * gyroVal[0] / sensorCount + ";" + gyroBuffer[1] * gyroVal[1] / sensorCount + ";" + gyroBuffer[2] * gyroVal[2] / sensorCount);
            }
        };
    }

    private final SensorEventListener mSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE) {
                if (event.values[0] > 0.05 ||event.values[1] > 0.05 ||event.values[2] > 0.05) {
                    Log.wtf("GYRO_CALIBRATE", "Device not still enough to calibrate");
                    val = 10;
                    txtView.setText("KEEP DEVICE STILL!!\nLeave still for " + String.valueOf(val) + " seconds");
                    btnStartSave.setText("Start");
                    btnStartSave.setEnabled(true);
                    gyroVal[0] = 0;
                    gyroVal[1] = 0;
                    gyroVal[2] = 0;
                    cdt.cancel();
                    mSensorManager.unregisterListener(mSensorListener);
//                    btnCancel.performClick();
                }

                gyroVal[0] += Math.abs(event.values[0]);
                gyroVal[1] += Math.abs(event.values[1]);
                gyroVal[2] += Math.abs(event.values[2]);
                sensorCount++;
            }

        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gyro_calibration, menu);
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
