package com.destroyer.rubikcubetimer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class UIStateMachine {

    protected static int STOPPING_TIMEOUT = 2000;

    protected enum STATES {START, WAITING, HOLDING, READY, RUNNING, STOPPING}

    private Context context;
    private float displayWidth, displayHeight;

    private View layoutView; // views[0] -> Layout Background
    private Button btn; // views[1] -> Button
    private ImageView imgView; // views[2] -> ImageView

    private State currentState;
    private List<State> stateList;

    private int val;
    private boolean halt;

    public UIStateMachine(Context context, float displayWidth, float displayHeight, View... views) {
        this.context = context;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.layoutView = views[0];
        this.btn = (Button)views[1];
        this.imgView = (ImageView)views[2];
        this.halt = false;

        this.stateList = new ArrayList<>();
    }

    public void haltProcess() {
        this.halt = true;
        Log.wtf("HALTING_STATEMACHINE", this.currentState.TAG);
    }

    public void addState(STATES state, STATES nextState, int btnBkg, int bdrBkg) {
        this.stateList.add(new State(state, nextState.ordinal(), btnBkg, bdrBkg));
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
    }

    private void setCurrentState(State nextState) {
        this.currentState = nextState;
        switch (currentState.state) {
            case START:
                this.imgView.setVisibility(View.GONE);
                btn.setClickable(true);
                break;

            case WAITING:
                float height = (float)(displayHeight / 1.5);
                imgView.setY(height);
                this.imgView.setVisibility(View.VISIBLE);
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
                                if (vibrate) vibrator.vibrate(250);
                                new Handler().postDelayed(this, 1000);
                            } else {
                                if (vibrate) vibrator.vibrate(500);
                                nextState();
                            }
                        }
                    }
                };
                if (vibrate) vibrator.vibrate(250);
                new Handler().postDelayed(cubeHolder, 1000);
                break;

            case READY:
                break;

            case RUNNING:
                this.imgView.setVisibility(View.GONE);
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
        layoutView.setBackgroundResource(currentState.bdrBkg);
        btn.setBackground(new BitmapDrawable(context.getResources(), getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), currentState.btnBkg), displayWidth / 2)));
        System.gc();
    }

    private static Bitmap getScaledBitmap(Bitmap b, float reqWidth) {
        return Bitmap.createScaledBitmap(b, (int)reqWidth, (int)((float)b.getHeight() * reqWidth / (float)b.getWidth() ), true);
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
