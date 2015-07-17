package com.destroyer.rubikcubetimer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*TODO:
    fix state linking/pointing errors (either here or in MainActivity)  (fixed?????)
    in STATE.WAITING
        -> set "Place cube here" text to display while the CDT is running
        -> when the CDT expires, start text flashing animation
 */

public class UIStateMachine {

    protected static int STOPPING_TIMEOUT = 2000;

    protected enum STATES {START, WAITING, HOLDING, READY, RUNNING, STOPPING}

    private Context context;
    private float displayHeight;

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

    private CountDownTimer cdt;

    private Animation animationFadeOut, animationFadeIn, animationBlink, animationFadeOutComplete, animationFadeInComplete;

    public UIStateMachine(Context context, float displayWidth, float displayHeight, View... views) {
        this.context = context;
        this.displayHeight = displayHeight;
        this.bkgGlow = (CustomImageView)views[0];
        this.btn = (CustomImageButton)views[1];
        this.dotLine = (ImageView)views[2];
        this.cube = (ImageView)views[3];
        this.stats = (TextView)views[4];
        this.timer = (TextView)views[5];
        this.halt = false;

        animationFadeIn = AnimationUtils.loadAnimation(context, R.anim.fadein);
        animationFadeOut = AnimationUtils.loadAnimation(context, R.anim.fadeout);
        animationFadeInComplete = AnimationUtils.loadAnimation(context, R.anim.fadeincomplete);
        animationFadeOutComplete = AnimationUtils.loadAnimation(context, R.anim.fadeoutcomplete);
        animationFadeIn.setFillAfter(true);
        animationFadeOut.setFillAfter(true);
        animationFadeInComplete.setFillAfter(true);
        animationFadeOutComplete.setFillAfter(true);
        animationBlink = AnimationUtils.loadAnimation(context, R.anim.blink);

        cdt = new CountDownTimer((Integer.valueOf(context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getString("cdtTime", "10")) + 1) * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timer.setText(String.format("%d", TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)));
            }

            @Override
            public void onFinish() {
                stats.setTextSize(30);
                stats.setText("PLACE CUBE\nNOW!!!");
                stats.setVisibility(View.VISIBLE);
                timer.setVisibility(View.GONE);
            }
        };

        ((ImageView)views[6]).setImageBitmap(getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.bkg_main), displayWidth));
        cube.setImageBitmap(getScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.cube), displayWidth / 2));

        this.stateList = new ArrayList<>();
    }

    /*
        Stops the HOLDING state from proceeding and should return the state to WAITING
     */
    public void haltProcess() {
        this.halt = true;
        this.cdt.cancel();
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
        setCurrentState(stateList.get(currentState.nextState));
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

    public void updateTimer() {
        cdt = new CountDownTimer((Integer.valueOf(context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getString("cdtTime", "10")) + 1) * 1000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                timer.setText(String.format("%d", TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)));
            }

            @Override
            public void onFinish() {
                stats.startAnimation(animationBlink);
                stats.setTextSize(30);
                stats.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/baarpbi_.otf"));
                stats.setText("Place cube here\nto start your\nsolve!!!");
                stats.setVisibility(View.VISIBLE);
                timer.setVisibility(View.GONE);
            }
        };
    }

    /*
        stateMachine Core
        defines actions for each state
     */
    private void setCurrentState(final State state) {
        this.currentState = state;
        switch (currentState.state) {
            case START:
                dotLine.clearAnimation();
                cube.startAnimation(animationFadeIn);
                stats.startAnimation(animationFadeInComplete);
                stats.setTextSize(24);
                stats.setTypeface(null, Typeface.NORMAL);
                updateStats();

                timer.setVisibility(View.GONE);
                dotLine.setVisibility(View.GONE);
                cube.setVisibility(View.VISIBLE);
                stats.setVisibility(View.VISIBLE);
                stats.clearAnimation();
                btn.setClickable(true);

                btn.setStateStart();
                bkgGlow.setStateStart();
                btn.refreshDrawableState();
                bkgGlow.refreshDrawableState();
                break;

            case WAITING:
                cube.startAnimation(animationFadeOut);
                stats.startAnimation(animationFadeInComplete);
                dotLine.startAnimation(animationFadeInComplete);
                if (context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getBoolean("cdt", true)) {
                    timer.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/HemiHead426.otf"));
                    timer.setTextSize(100);
                    timer.setTextColor(Color.RED);
                    timer.setVisibility(View.VISIBLE);
                    cdt.start();
                } else {
                    stats.startAnimation(animationBlink);
                    timer.setVisibility(View.GONE);
                }
                stats.setVisibility(View.GONE);

                float height = (float)(displayHeight / .75);
                dotLine.setY(height);
                dotLine.setVisibility(View.VISIBLE);

                stats.setTextSize(30);
                stats.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/baarpbi_.otf"));
                stats.setText("Place cube here\nto start your\nsolve!!!");
                stats.setVisibility(View.VISIBLE);

                btn.setStateWaiting();
                bkgGlow.setStateWaiting();
                btn.refreshDrawableState();
                bkgGlow.refreshDrawableState();
                break;

            case HOLDING:
                halt = false;
                cdt.cancel();
                stats.clearAnimation();
                final boolean vibrate = context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getBoolean("vibrate", true);
                final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                val = 3;
                Runnable cubeHolder = new Runnable() {
                    @Override
                    public void run() {
                        if (!halt) {
                            if (val-- > 0) {
                                if (vibrate) vibrator.vibrate(250);
                                timer.setText(String.valueOf(val));
                                new Handler().postDelayed(this, 1000);
                            } else {
                                nextState();
                            }
                        }
                    }
                };
                timer.setText("");
                timer.setVisibility(View.VISIBLE);
                timer.setTextColor(Color.YELLOW);
                stats.setVisibility(View.GONE);

                btn.setStateHolding();
                btn.refreshDrawableState();
                bkgGlow.setStateHolding();
                bkgGlow.refreshDrawableState();
                if (vibrate) vibrator.vibrate(250);
                timer.setText(String.valueOf(val));
                new Handler().postDelayed(cubeHolder, 1000);
                break;

            case READY:
                timer.setTextColor(Color.WHITE);
                timer.setTextSize(42);
                timer.setText("Lift cube to\nstart timer!");

                btn.setStateReady();
                btn.refreshDrawableState();
                bkgGlow.setStateReady();
                bkgGlow.refreshDrawableState();
                if (context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getBoolean("vibrate", true)) ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                break;

            case RUNNING:
                dotLine.clearAnimation();
                dotLine.setVisibility(View.GONE);
                cube.setVisibility(View.GONE);
                timer.setTextColor(Color.WHITE);
                getFontFromPreference(timer);
//                timer.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/ATOMICCLOCKRADIO.otf"));
//                timer.setTextSize(100);
                timer.setVisibility(View.VISIBLE);
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
                                        DBAdapter db = new DBAdapter(context);
                                        db.open();
                                        db.addTime(System.currentTimeMillis(), val);
                                        db.close();
                                        nextState();
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

    private void getFontFromPreference(TextView textView){
        switch (context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getString("timerFont", "atomic")){
            case "atomic":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/ATOMICCLOCKRADIO.otf"));
                textView.setTextSize(100);
                break;

            case "baar":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/baarpbi_.otf"));
                textView.setTextSize(100);
                break;

            case "delusion":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/DELUSION.otf"));
                textView.setTextSize(100);
                break;

            default:
                break;
        }
    }

    private void updateStats() {
        int avgAll = 0, avg5 = 0, avg25 = 0, low = Integer.MAX_VALUE, high = Integer.MIN_VALUE, val;
        DBAdapter db = new DBAdapter(context);
        db.open();
        Cursor cursor;
        if ((cursor = db.getAllTimes()) != null && cursor.moveToFirst()) {
            do {
                val = cursor.getInt(1);
                if (cursor.getPosition() < 5) avg5 += val;
                if (cursor.getPosition() < 25) avg25 += val;
                avgAll += val;
                if (val > high) high = val;
                if (val < low) low = val;
            } while (cursor.moveToNext());

            avgAll = avgAll / cursor.getCount();
            avg5 = avg5 / 5;
            avg25 = avg25 / 25;

            stats.setText("Average Score:  " + formatString(avgAll) +
                    (cursor.getCount() > 25 ? "\nLast 25 Average:  " + formatString(avg25) : "") +
                    (cursor.getCount() > 5 ? "\nLast 5 Average:  " + formatString(avg5) : "") +
                    "\nFastest Score:  " + formatString(low) +
                    "\nLast Score:  " + (cursor.moveToFirst() ? formatString(cursor.getInt(1)) : formatString(0)));
        } else stats.setText("");
        db.close();
    }

    private String formatString(long millis) {
        return (TimeUnit.MILLISECONDS.toMinutes(millis) > 0 ?
                String.format("%d:%02d:%02d",
                        (millis / (1000 * 60)),
                        ((millis / 1000) % 60),
                        ((millis / 10)%100))
                :
                String.format("%02d:%02d",
                        ((millis / 1000) % 60),
                        ((millis / 10)%100)));
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
