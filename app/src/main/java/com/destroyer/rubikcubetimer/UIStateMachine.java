package com.destroyer.rubikcubetimer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class UIStateMachine {

    protected static int STOPPING_TIMEOUT = 2000;

    protected enum STATES {START, WAITING, HOLDING, READY, RUNNING, STOPPING}

    private Context context;
    private int displayWidth;

    private View layoutView; // views[0] -> Background
    private Button btn; // views[1] -> Button

    private State currentState;
    private List<State> stateList;

    private int val;
    private boolean halt;

    public UIStateMachine(Context context, int displayWidth, View... views) {
        this.context = context;
        this.displayWidth = displayWidth;
        this.layoutView = views[0];
        this.btn = (Button)views[1];
        this.halt = false;

        this.stateList = new ArrayList<>();
        this.stateList.add(new State(STATES.START, STATES.WAITING.ordinal(), R.drawable.startbtn, Color.WHITE)); //R.drawable.rubikmainbackground));
        this.stateList.add(new State(STATES.WAITING, STATES.HOLDING.ordinal(), R.drawable.proxwaitbtn, Color.YELLOW)); //R.drawable.rubiksetprox));
        this.stateList.add(new State(STATES.HOLDING, STATES.READY.ordinal(), R.drawable.proxholdbtn, Color.BLUE)); //R.drawable.rubikproxready));
        this.stateList.add(new State(STATES.READY, STATES.RUNNING.ordinal(), R.drawable.proxreadybtn, Color.CYAN)); //R.drawable.rubiktimerready));
        this.stateList.add(new State(STATES.RUNNING, STATES.STOPPING.ordinal(), R.drawable.startbtn/*TODO: change to STOP button*/, Color.GREEN)); //R.drawable.rubiktimerstart));
        this.stateList.add(new State(STATES.STOPPING, STATES.START.ordinal(), R.drawable.finishedbtn, Color.RED)); //R.drawable.rubiktimerstop));

        this.currentState = this.stateList.get(0);
    }

    public void haltProcess() {
        this.halt = true;
        Log.wtf("HALTING_STATEMACHINE", this.currentState.TAG);
    }

    public void createNewState() {
    }

    public void resetState() {
        this.setCurrentState(this.stateList.get(0));
    }

    public STATES getState() {
        return this.currentState.state;
    }

    public void nextState() {
        this.setCurrentState(this.stateList.get(this.currentState.nextState));
    }

    public void setState(STATES state) {
        this.setCurrentState(stateList.get(state.ordinal()));
//        Log.wtf("SET_STATE", this.currentState.TAG);
    }

    private void setCurrentState(State nextState) {
//        Log.wtf("CURRENT_STATE", this.currentState.TAG);
//        Log.wtf("NEXT", nextState.TAG);
        this.currentState = nextState;
        switch (currentState.state) {
            case START:
                btn.setClickable(true);
                break;

            case WAITING:
                break;

            case HOLDING:
                this.halt = false;
                final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                val = 0;
                Runnable cubeHolder = new Runnable() {
                    @Override
                    public void run() {
                        if (!halt) {
                            if (val++ < 2) {
                                vibrator.vibrate(250);
                                new Handler().postDelayed(this, 1000);
                            } else {
                                vibrator.vibrate(500);
                                nextState();
                            }
                        }
                    }
                };
                vibrator.vibrate(250);
                new Handler().postDelayed(cubeHolder, 1000);
                break;

            case READY:
                break;

            case RUNNING:
                btn.setClickable(false);
                break;

            case STOPPING:
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        nextState();
                    }
                }, STOPPING_TIMEOUT);
                break;

            default:
                break;
        }
        setViews();
    }

    public void updateViews() {
        setViews();
    }

    private void setViews() {
//        layoutView.setBackgroundResource(currentState.bdrBkg);
        layoutView.setBackgroundColor(currentState.bdrBkg);
        btn.setBackground(new BitmapDrawable(context.getResources(), getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), currentState.btnBkg), displayWidth / 2)));
        System.gc();
    }

    private static Bitmap getScaledBitmap(Bitmap b, int reqWidth) {
        return Bitmap.createScaledBitmap(b, reqWidth, (int)((float)b.getHeight() * (float)reqWidth / (float)b.getWidth() ), true);
    }

    private class State {

        public String TAG;
        public STATES state;
        public int nextState;
        public int btnBkg;
        public int bdrBkg;

        public State(STATES state, int nextState, int btnBkg, int bdrBkg) {
            this.TAG = state.name();
            this.state = state;
            this.nextState = nextState;
            this.btnBkg = btnBkg;
            this.bdrBkg = bdrBkg;
        }
    }
}