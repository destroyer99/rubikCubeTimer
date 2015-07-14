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

    private ImageView bkgGlow;
    private ImageButton btn;
    private ImageView dotLine;
    private ImageView cube;
    private TextView stats;
    private TextView timer;

    private State currentState;
    private List<State> stateList;

    private Bitmap bkgGlowBitmap;
    private Bitmap btnBitmap;

    private long val;
    private boolean halt;

    private Thread setViews = new Thread(new Runnable() {
        @Override
        public void run() {
            if (bkgGlowBitmap == null) {
                bkgGlowBitmap = getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), currentState.bkgGlow), displayWidth);
            }
            if (btnBitmap == null) {
                btnBitmap = getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), currentState.btnBkg), displayWidth / 2);
            }

            bkgGlow.setImageBitmap(bkgGlowBitmap);
            btn.setImageBitmap(btnBitmap);

            bkgGlowBitmap = getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), stateList.get(currentState.nextState).bkgGlow), displayWidth);
            btnBitmap = getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), stateList.get(currentState.nextState).btnBkg), displayWidth / 2);
        }
    });

    public UIStateMachine(Context context, float displayWidth, float displayHeight, View... views) {
        this.context = context;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.bkgGlow = (ImageView)views[0];
        this.btn = (ImageButton)views[1];
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
        this.bkgGlowBitmap = this.btnBitmap = null;
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
        this.bkgGlowBitmap = this.btnBitmap = null;
        this.setCurrentState(stateList.get(state.ordinal()));
    }

    /*
        public call to explicitly update views to currentState
     */
    public void updateViews() {
        setViews.run();
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
                break;

            case WAITING:
                float height = (float)(displayHeight / 1.2);
                dotLine.setY(height);
                this.stats.setVisibility(View.GONE);
                this.dotLine.setVisibility(View.VISIBLE);
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
                setViews.run();
                if (vibrate) vibrator.vibrate(250);
                new Handler().postDelayed(cubeHolder, 1000);
                return;

            case READY:
                setViews.run();
                ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                return;

            case RUNNING:
                this.dotLine.setVisibility(View.GONE);
                this.cube.setVisibility(View.GONE);
                this.timer.setVisibility(View.VISIBLE);
                btn.setClickable(false);
                break;

            case STOPPING:
                setViews.run();
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
                return;

            default:
                break;
        }
        setViews.run();
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
