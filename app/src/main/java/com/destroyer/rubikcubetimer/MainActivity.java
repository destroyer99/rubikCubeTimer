package com.destroyer.rubikcubetimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ImageView;
import android.widget.Toast;

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
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getBoolean("firstRun", true)) {
            ImageView img = new ImageView(this);
            img.setImageResource(R.drawable.cube);
            new AlertDialog.Builder(this)
                    .setTitle("First Run")
                    .setView(img)
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
        stateMachine = new UIStateMachine(this, displayMetrics.widthPixels, displayMetrics.ydpi, findViewById(R.id.bkgGlow),
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
