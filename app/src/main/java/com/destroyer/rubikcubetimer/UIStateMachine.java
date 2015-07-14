package com.destroyer.rubikcubetimer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*TODO:
    pass in TextViews to toggle/edit viewable text
    fix state linking/pointing errors (either here or in MainActivity)  (fixed?????)
    ssslllooowww performance, especially from HOLDING->READY
        -> maybe put calls from MainActivity on a seperate thread?
 */

public class UIStateMachine {

    protected static int STOPPING_TIMEOUT = 2000;

    protected enum STATES {START, WAITING, HOLDING, READY, RUNNING, STOPPING}

    private Context context;
    private float displayWidth, displayHeight;

    private CustomImageView bkgGlow;
    private CustomImageButton btn;
    private ImageView dotLine;
    private ImageView cube;
    private TextView stats;
    private TextView timer;

    private State currentState;
    private List<State> stateList;

    private long val;
    private boolean halt;

    public UIStateMachine(Context context, float displayWidth, float displayHeight, View... views) {
        this.context = context;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.bkgGlow = (CustomImageView)views[0];
        this.btn = (CustomImageButton)views[1];
        this.dotLine = (ImageView)views[2];
        this.cube = (ImageView)views[3];
        this.stats = (TextView)views[4];
        this.timer = (TextView)views[5];
        this.halt = false;

        ((ImageView)views[6]).setImageBitmap(getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.bkg_main), displayWidth));
        cube.setImageBitmap(getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.cube), displayWidth / 2));

        this.stateList = new ArrayList<>();
    }

    /*
        Stops the HOLDING state from proceeding and should return the state to WAITING
     */
    public void haltProcess() {
        this.halt = true;
    }

    /*
        create state and add it to the stateList
     */
    public void addState(STATES state, STATES nextState, int bkgGlow, int btnBkg) {
        this.stateList.add(state.ordinal(), new State(state, nextState.ordinal(), bkgGlow, btnBkg));
    }

    /*
        reset currentState to START (or whatever the first defined state is)
     */
    public void resetState() {
        this.setCurrentState(this.stateList.get(STATES.START.ordinal()));
    }

    /*
        get the current state of the stateMachine
     */
    public STATES getState() {
        return this.currentState.state;
    }

    /*
        public call to execute stateMachine to next state
     */
    public void nextState() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                setCurrentState(stateList.get(currentState.nextState));
            }
        });
    }

    /*
        public call to explicitly set the currentState
     */
    public void setState(STATES state) {
        this.setCurrentState(stateList.get(state.ordinal()));
    }

    /*
        public call to explicitly update views to currentState
     */
    public void updateViews() {
        btn.refreshDrawableState();
        bkgGlow.refreshDrawableState();
    }

    public void setVal(long val) {
        this.val = val;
    }

    /*
        stateMachine Core
        defines actions for each state
     */
    private void setCurrentState(final State state) {
        this.currentState = state;
        switch (currentState.state) {
            case START:
                this.timer.setVisibility(View.GONE);
                this.dotLine.setVisibility(View.GONE);
                this.cube.setVisibility(View.VISIBLE);
                this.stats.setVisibility(View.VISIBLE);
                btn.setClickable(true);

                btn.setStateStart();
                btn.refreshDrawableState();
                bkgGlow.setStateStart();
                bkgGlow.refreshDrawableState();
                break;

            case WAITING:
                float height = (float)(displayHeight / 1.2);
                dotLine.setY(height);
                this.stats.setVisibility(View.GONE);
                this.dotLine.setVisibility(View.VISIBLE);

                btn.setStateWaiting();
                btn.refreshDrawableState();
                bkgGlow.setStateWaiting();
                bkgGlow.refreshDrawableState();
                break;

            case HOLDING:
                this.halt = false;
                final boolean vibrate = context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getBoolean("vibrate", true);
                final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                val = 0;
                Runnable cubeHolder = new Runnable() {
                    @Override
                    public void run() {
                        if (!halt) {
                            if (val++ < 2) {
                                if (val == 2) {
                                    if (vibrate) vibrator.vibrate(250);
                                    new Handler().postDelayed(this, 500);
                                    return;
                                }
                                if (vibrate) vibrator.vibrate(250);
                                new Handler().postDelayed(this, 1000);
                            } else {
                                nextState();
                            }
                        }
                    }
                };
                btn.setStateHolding();
                btn.refreshDrawableState();
                bkgGlow.setStateHolding();
                bkgGlow.refreshDrawableState();
                if (vibrate) vibrator.vibrate(250);
                new Handler().postDelayed(cubeHolder, 1000);
                break;

            case READY:
                btn.setStateReady();
                btn.refreshDrawableState();
                bkgGlow.setStateReady();
                bkgGlow.refreshDrawableState();
                if (context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getBoolean("vibrate", true)) ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                break;

            case RUNNING:
                this.dotLine.setVisibility(View.GONE);
                this.cube.setVisibility(View.GONE);
                this.timer.setVisibility(View.VISIBLE);
                btn.setClickable(false);

                btn.setStateRunning();
                btn.refreshDrawableState();
                bkgGlow.setStateRunning();
                bkgGlow.refreshDrawableState();
                break;

            case STOPPING:
                btn.setStateStopping();
                btn.refreshDrawableState();
                bkgGlow.setStateStopping();
                bkgGlow.refreshDrawableState();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(context)
                                .setTitle("Save Score")
                                .setMessage("Save score to Database?\t\t" /*+ "\n\t\t\t\t\t\t"*/ + formatString(val))
                                .setCancelable(true)
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                        nextState();
                                        dialog.cancel();
                                    }
                                })
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                        nextState();
                                        DBAdapter db = new DBAdapter(context);
                                        db.open();
                                        db.addTime(System.currentTimeMillis(), val);
                                        db.close();
                                        dialog.dismiss();
                                    }
                                }).show();
                    }
                }, STOPPING_TIMEOUT);
                break;

            default:
                break;
        }
    }

    private static Bitmap getScaledBitmap(Bitmap b, float reqWidth) {
        return Bitmap.createScaledBitmap(b, (int)reqWidth, (int)((float)b.getHeight() * reqWidth / (float)b.getWidth() ), true);
    }

    private String formatString(long millis) {
        return (TimeUnit.MILLISECONDS.toMinutes(millis) > 0 ?
                String.format("%d:%02d:%03d",
                        TimeUnit.MILLISECONDS.toMinutes(millis),
                        TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
                        TimeUnit.MILLISECONDS.toMillis(millis) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis)))
                :
                String.format("%02d:%03d",
                        TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
                        TimeUnit.MILLISECONDS.toMillis(millis) - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis))));
    }

    private class State {

        public String TAG;
        public STATES state;
        public int nextState;
        public int bkgGlow;
        public int btnBkg;

        public State(STATES state, int nextState, int bkgGlow, int btnBkg) {
            this.TAG = state.name();
            this.state = state;
            this.nextState = nextState;
            this.bkgGlow = bkgGlow;
            this.btnBkg = btnBkg;
        }
    }
}
