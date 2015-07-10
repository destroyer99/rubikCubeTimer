package com.destroyer.rubikcubetimer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private final static float gyroThreshold = 0.018f;

    private UIStateMachine stateMachine;

    private SensorManager mSensorManager;
    DBAdapter db = new DBAdapter(this);
    Vibrator vibrator;
    private float[] gyroLast = {0, 0, 0};
    boolean runTimer = false;
    TextView timerTxt, statsTxt;
    long init, millis;
    boolean gyroSettled = false;
    private int timerPrecision;
    float height;

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
                    runTimer = false;
                    mSensorManager.unregisterListener(mSensorListener);
                    stateMachine.nextState();
                    db.open();
                    Log.wtf("DB_ADDED_SCORE", String.valueOf(db.addTime(System.currentTimeMillis(), millis)));
                    db.close();
                } else {
                    gyroLast[0] = Math.abs(event.values[0]);
                    gyroLast[1] = Math.abs(event.values[1]);
                    gyroLast[2] = Math.abs(event.values[2]);
                }
            } else if(event.sensor.getType()==Sensor.TYPE_PROXIMITY) {
                if (stateMachine.getState() == UIStateMachine.STATES.WAITING && event.values[0] == 0) {
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
                    runTimer = true;
                    init = System.currentTimeMillis();
                    timerTxt.setText("");
                    timerTxt.setTextSize(50);
                    new Handler().post(timer);
                } else if (stateMachine.getState() != UIStateMachine.STATES.WAITING && event.values[0] > 1) {
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

        timerTxt = (TextView) findViewById(R.id.timerTxt);
        statsTxt = (TextView) findViewById(R.id.statsTxt);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Point dsp = new Point();
        getWindowManager().getDefaultDisplay().getSize(dsp);

        stateMachine = new UIStateMachine(this, dsp.x, dsp.y, findViewById(R.id.background), findViewById(R.id.startResetBtn), findViewById(R.id.dottedLine));
        stateMachine.addState(UIStateMachine.STATES.START, UIStateMachine.STATES.WAITING, R.drawable.startbtn, Color.WHITE); //R.drawable.rubikmainbackground));
        stateMachine.addState(UIStateMachine.STATES.WAITING, UIStateMachine.STATES.HOLDING, R.drawable.proxwaitbtn, Color.YELLOW); //R.drawable.rubiksetprox));
        stateMachine.addState(UIStateMachine.STATES.HOLDING, UIStateMachine.STATES.READY, R.drawable.proxholdbtn, Color.BLUE); //R.drawable.rubikproxready));
        stateMachine.addState(UIStateMachine.STATES.READY, UIStateMachine.STATES.RUNNING, R.drawable.proxreadybtn, Color.CYAN); //R.drawable.rubiktimerready));
        stateMachine.addState(UIStateMachine.STATES.RUNNING, UIStateMachine.STATES.STOPPING, R.drawable.startbtn/*TODO: change to STOP button*/, Color.GREEN); //R.drawable.rubiktimerstart));
        stateMachine.addState(UIStateMachine.STATES.STOPPING, UIStateMachine.STATES.START, R.drawable.finishedbtn, Color.RED); //R.drawable.rubiktimerstop));
    }

    @Override
    protected void onResume() {
        super.onResume();
        timerPrecision = Integer.valueOf(getSharedPreferences("appPreferences", MODE_PRIVATE).getString("timerPrecision", "21"));
        updateStats();
        stateMachine.updateViews();
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mSensorListener);
        super.onPause();
    }

    public void onButtonClick(View view) {
        switch (stateMachine.getState()) {
            case START:
                    //stateMachine.nextState();
                    mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_FASTEST);
                    updateStats();
                break;

            case WAITING: case HOLDING: case READY:
                runTimer = gyroSettled = false;
                mSensorManager.unregisterListener(mSensorListener);
                stateMachine.resetState();

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
