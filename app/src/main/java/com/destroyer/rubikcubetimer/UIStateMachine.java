package com.destroyer.rubikcubetimer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*TODO:

 */

public class UIStateMachine {

    protected static int STOPPING_TIMEOUT = 500;

    protected enum STATES {START, WAITING, HOLDING, READY, RUNNING, STOPPING}

    private Context context;
    private float displayHeight;

    private CustomImageView bkgGlow;
    private CustomImageButton btn;
    private ImageView dotLine;
    private ImageView cube;
    private TextView statsTxt;
    private TextView timerTxt;
    private TextView scrambleTxt;

    private State currentState;
    private List<State> stateList;

    private boolean runTimer;
    private long millis, val;
    private int timerPrecision;

    private CountDownTimer cdtWaiting, cdtHolding;
    private boolean vibrate;
    private Vibrator vibrator;

    private Animation animationFadeOut, animationFadeIn, animationBlink, animationFadeOutComplete, animationFadeInComplete;

    private Handler handler = new Handler();

    private final Runnable timer = new Runnable() {
        @Override
        public void run() {
            if(runTimer) {
                millis = System.currentTimeMillis()-val;
                timerTxt.setText(formatString(millis));
                if (runTimer) new Handler().postDelayed(this, timerPrecision);
            }
        }
    };

    private Runnable saveDialog = new Runnable() {
        @Override
        public void run() {
            new AlertDialog.Builder(context)
                    .setTitle("Save Score")
                    .setMessage("Save score to Database?\t\t" + formatString(millis))
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
                            db.addTime(System.currentTimeMillis(), millis);
                            db.close();
                            nextState();
                            dialog.dismiss();
                        }
                    }).show();
        }
    };

    public UIStateMachine(Context ctx, float displayWidth, float displayHeight, View... views) {
        this.context = ctx;
        this.displayHeight = displayHeight;
        this.bkgGlow = (CustomImageView)views[0];
        this.btn = (CustomImageButton)views[1];
        this.cube = (ImageView)views[2];
        this.dotLine = (ImageView)views[3];
        this.statsTxt = (TextView)views[4];
        this.timerTxt = (TextView)views[5];
        this.scrambleTxt = (TextView)views[6];

        animationFadeIn = AnimationUtils.loadAnimation(context, R.anim.fadein);
        animationFadeOut = AnimationUtils.loadAnimation(context, R.anim.fadeout);
        animationFadeInComplete = AnimationUtils.loadAnimation(context, R.anim.fadeincomplete);
        animationFadeOutComplete = AnimationUtils.loadAnimation(context, R.anim.fadeoutcomplete);
        animationFadeIn.setFillAfter(true);
        animationFadeOut.setFillAfter(true);
        animationFadeInComplete.setFillAfter(true);
        animationFadeOutComplete.setFillAfter(true);
        animationBlink = AnimationUtils.loadAnimation(context, R.anim.blink);

        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        cdtWaiting = new CountDownTimer((Integer.valueOf(context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getString("cdtTime", "10")) + 1) * 1000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerTxt.setText(String.format("%d", TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)));
            }

            @Override
            public void onFinish() {
                statsTxt.startAnimation(animationBlink);
                statsTxt.setTextSize(30);
                statsTxt.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/HemiHead426.otf"));
                statsTxt.setText("Place cube here\nto start your\nsolve!!!");
                timerTxt.startAnimation(animationFadeOutComplete);
            }
        };

        cdtHolding = new CountDownTimer(3000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (millisUntilFinished < val*1000) {
                    if (vibrate) vibrator.vibrate(250);
                    timerTxt.setText(String.valueOf(val--));
                }
            }

            @Override
            public void onFinish() {
                nextState();
            }
        };

        stateList = new ArrayList<>();

        stateList.add(STATES.START.ordinal(), new State(STATES.START, STATES.WAITING, R.drawable.glow_start_rainbow, R.drawable.btn_start));
        stateList.add(STATES.WAITING.ordinal(), new State(STATES.WAITING, STATES.HOLDING, R.drawable.btn_waiting, R.drawable.btn_waiting));
        stateList.add(STATES.HOLDING.ordinal(), new State(STATES.HOLDING, STATES.READY, R.drawable.glow_holding, R.drawable.btn_holding));
        stateList.add(STATES.READY.ordinal(), new State(STATES.READY, STATES.RUNNING, R.drawable.glow_ready, R.drawable.btn_ready));
        stateList.add(STATES.RUNNING.ordinal(), new State(STATES.RUNNING, STATES.STOPPING, R.drawable.glow_running, R.drawable.btn_running));
        stateList.add(STATES.STOPPING.ordinal(), new State(STATES.STOPPING, STATES.START, R.drawable.glow_finished, R.drawable.btn_finished));
    }

    /*
        Stops the HOLDING state from proceeding and should return the state to WAITING
     */
    public void haltProcess() {
        cdtWaiting.cancel();
        cdtHolding.cancel();
        runTimer = false;
        if(handler!= null){
            handler.removeCallbacks(saveDialog);
        }
    }

    /*
        create state and add it to the stateList
     */
    public void addState(STATES state, STATES nextState, int bkgGlow, int btnBkg) {
        stateList.add(state.ordinal(), new State(state, nextState.ordinal(), bkgGlow, btnBkg));
    }

    /*
        reset currentState to START (or whatever the first defined state is)
     */
    public void resetState() {
        haltProcess();
        timerTxt.clearAnimation();
        timerPrecision = Integer.valueOf(context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getString("timerPrecision", "21"));
        vibrate = context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getBoolean("vibrate", true);
        setCurrentState(stateList.get(STATES.START.ordinal()), null);
    }

    /*
        get the current state of the stateMachine
     */
    public STATES getState() {
        return currentState.state;
    }

    /*
        public call to execute stateMachine to next state
     */
    public void nextState() {
        setCurrentState(stateList.get(currentState.nextState), currentState);
    }

    /*
        public call to explicitly set the currentState
     */
    public void setState(STATES state) {
        setCurrentState(stateList.get(state.ordinal()), currentState);
    }

    /*
        stateMachine Core
        defines actions for each state
     */
    private void setCurrentState(final State state, final State previousState) {
        currentState = state;
        switch (currentState.state) {
            case START:
                dotLine.clearAnimation();
                cube.startAnimation(animationFadeIn);
                timerTxt.setTextColor(Color.WHITE);
                timerTxt.setBackgroundColor(Color.TRANSPARENT);

                ViewGroup.LayoutParams params = timerTxt.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                timerTxt.setLayoutParams(params);

                statsTxt.startAnimation(animationFadeInComplete);
                statsTxt.setTextSize(24);
                statsTxt.setTypeface(null, Typeface.NORMAL);
                updateStats();

                timerTxt.setText("");
                dotLine.setVisibility(View.GONE);
                cube.setVisibility(View.VISIBLE);
                statsTxt.clearAnimation();
                btn.setClickable(true);

                btn.setStateStart();
                bkgGlow.setStateStart();
                btn.refreshDrawableState();
                bkgGlow.refreshDrawableState();

                scrambleTxt.setText(Scrambler.generateScramble());
                scrambleTxt.setVisibility(View.VISIBLE);


                break;

            case WAITING:
                scrambleTxt.setVisibility(View.INVISIBLE);
                cdtHolding.cancel();
                cdtWaiting.cancel();
                timerTxt.setText("");
                if (previousState.state == STATES.START) {
                    cube.startAnimation(animationFadeOut);
                    statsTxt.startAnimation(animationFadeInComplete);
                    dotLine.startAnimation(animationFadeInComplete);

                    timerTxt.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/HemiHead426.otf"));
                    timerTxt.setTextSize(100);

                    if (context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getBoolean("cdt", true)) {
                        timerTxt.setTextColor(Color.RED);
                        cdtWaiting.start();
                    } else {
                        timerTxt.setText("");
                        statsTxt.startAnimation(animationBlink);
                    }
                }
                else {
                    statsTxt.startAnimation(animationBlink);
                    timerTxt.setText("");
                }

                float height = (float)(displayHeight / 0.75);
                dotLine.setY(height);
                dotLine.setVisibility(View.VISIBLE);

                statsTxt.setTextSize(30);
                statsTxt.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/HemiHead426.otf"));
                statsTxt.setText("Place cube here\nto start your\nsolve!!!");

                btn.setStateWaiting();
                bkgGlow.setStateWaiting();
                btn.refreshDrawableState();
                bkgGlow.refreshDrawableState();
                break;

            case HOLDING:
                cdtWaiting.cancel();
                statsTxt.clearAnimation();

                timerTxt.setText("");
                timerTxt.clearAnimation();
                timerTxt.setTextColor(Color.YELLOW);
                statsTxt.setText("");

                btn.setStateHolding();
                btn.refreshDrawableState();
                bkgGlow.setStateHolding();
                bkgGlow.refreshDrawableState();

                val = 3;
                if (vibrate) vibrator.vibrate(250);
                timerTxt.setText(String.valueOf(val--));
                cdtHolding.start();
                break;

            case READY:
                if (vibrate) vibrator.vibrate(750);
                dotLine.clearAnimation();
                dotLine.setVisibility(View.GONE);
                timerTxt.setTextColor(Color.WHITE);
                timerTxt.setTextSize(42);
                timerTxt.setText("Lift cube to\nstart timer!");

                btn.setStateReady();
                btn.refreshDrawableState();
                bkgGlow.setStateReady();
                bkgGlow.refreshDrawableState();
                if (context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE).getBoolean("vibrate", true)) ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                break;

            case RUNNING:
                dotLine.clearAnimation();
                cube.setVisibility(View.GONE);
                timerTxt.setTextColor(Color.WHITE);
                getFontFromPreference(timerTxt);
                timerTxt.setText("");

                btn.setClickable(false);
                btn.setStateRunning();
                btn.refreshDrawableState();
                bkgGlow.setStateRunning();
                bkgGlow.refreshDrawableState();

                runTimer = true;
                val = System.currentTimeMillis();
                new Handler().post(timer);

                break;

            case STOPPING:
                runTimer = false;
                btn.setStateStopping();
                btn.refreshDrawableState();
                bkgGlow.setStateStopping();
                bkgGlow.refreshDrawableState();
                handler.postDelayed(saveDialog, STOPPING_TIMEOUT);
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
                textView.setTextSize(48);
                break;

            case "chickenscratch":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/ChickenScratch.otf"));
                textView.setTextSize(80);
                break;

            case "comicbook":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/ComicBook.otf"));
                textView.setTextSize(80);
                break;

            case "delusion":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Delusion.otf"));
                textView.setTextSize(85);
                break;

            case "eroded":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/RetroErosion.otf"));
                textView.setTextSize(75);
                break;

            case "earthquake":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Erthqake.otf"));
                textView.setTextSize(55);
                break;

            case "ghetto":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/ghettomarquee.otf"));
                textView.setTextSize(65);
                textView.setTextColor(Color.BLACK);
//                textView.setBackgroundColor(Color.WHITE);
                textView.setBackgroundResource(R.drawable.white_gradient);
//                textView.setBackgroundDrawable("@drawable/white_gradient");
                ViewGroup.LayoutParams params = timerTxt.getLayoutParams();
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                timerTxt.setLayoutParams(params);
                break;

            case "hemihead426":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/HemiHead426.otf"));
                textView.setTextSize(80);
                break;

            case "jerrybuilt":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Jerrybuilt.otf"));
                textView.setTextSize(75);
                break;

            case "letsgodigital":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/LetsgoDigital.otf"));
                textView.setTextSize(80);
                break;

            case "rugged":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/RuggedRide.otf"));
                textView.setTextSize(80);
                break;

            case "see":
                textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/See.otf"));
                textView.setTextSize(65);
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
                val = cursor.getInt(2);
                if (cursor.getPosition() < 5) avg5 += val;
                if (cursor.getPosition() < 25) avg25 += val;
                avgAll += val;
                if (val > high) high = val;
                if (val < low) low = val;
            } while (cursor.moveToNext());

            avgAll = avgAll / cursor.getCount();
            avg5 = avg5 / 5;
            avg25 = avg25 / 25;

            statsTxt.setText("Average Score:  " + formatString(avgAll) +
                    (cursor.getCount() > 25 ? "\nLast 25 Average:  " + formatString(avg25) : "") +
                    (cursor.getCount() > 5 ? "\nLast 5 Average:  " + formatString(avg5) : "") +
                    "\nFastest Score:  " + formatString(low) +
                    "\nLast Score:  " + (cursor.moveToFirst() ? formatString(cursor.getInt(2)) : formatString(0)));
        } else statsTxt.setText("");
        db.close();
    }

    private String formatString(long millis) {
        return ((millis / (1000 * 60)) > 0 ?
                String.format("%d:%02d:%02d",
                        (millis / (1000 * 60)),
                        ((millis / 1000) % 60),
                        ((millis / 10) % 100))
                :
                String.format("%02d:%02d",
                        ((millis / 1000) % 60),
                        ((millis / 10) % 100)));
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

        public State(STATES state, STATES nextState, int bkgGlow, int btnBkg) {
            this.TAG = state.name();
            this.state = state;
            this.nextState = nextState.ordinal();
            this.bkgGlow = bkgGlow;
            this.btnBkg = btnBkg;
        }
    }
}
