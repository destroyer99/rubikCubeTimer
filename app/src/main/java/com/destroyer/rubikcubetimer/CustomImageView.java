package com.destroyer.rubikcubetimer;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CustomImageView extends ImageView {

  private static final int[] STATE_START = {R.attr.state_start};
  private static final int[] STATE_CDT = {R.attr.state_cdt};
  private static final int[] STATE_WAITING = {R.attr.state_waiting};
  private static final int[] STATE_HOLDING = {R.attr.state_holding};
  private static final int[] STATE_READY = {R.attr.state_ready};
  private static final int[] STATE_RUNNING = {R.attr.state_running};
  private static final int[] STATE_STOPPING = {R.attr.state_stopping};
  private static final int[] STATE_FINISHED = {R.attr.state_finished};

  private boolean mIsStart = false;
  private boolean mIsCDT = false;
  private boolean mIsWaiting = false;
  private boolean mIsHolding = false;
  private boolean mIsReady = false;
  private boolean mIsRunning = false;
  private boolean mIsStopping = false;
  private boolean mIsFinished = false;

  public CustomImageView(Context context) {
    super(context);
    this.mIsStart = true;
  }

  public CustomImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.mIsStart = true;
  }

  public CustomImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    this.mIsStart = true;
  }

  public void setStateStart() {
    this.mIsStart = true;
    this.mIsCDT = false;
    this.mIsWaiting = false;
    this.mIsHolding = false;
    this.mIsReady = false;
    this.mIsRunning = false;
    this.mIsStopping = false;
    this.mIsFinished = false;
  }

  public void setStateCDT() {
    this.mIsStart = false;
    this.mIsCDT = true;
    this.mIsWaiting = false;
    this.mIsHolding = false;
    this.mIsReady = false;
    this.mIsRunning = false;
    this.mIsStopping = false;
    this.mIsFinished = false;
  }

  public void setStateWaiting() {
    this.mIsStart = false;
    this.mIsCDT = false;
    this.mIsWaiting = true;
    this.mIsHolding = false;
    this.mIsReady = false;
    this.mIsRunning = false;
    this.mIsStopping = false;
    this.mIsFinished = false;
  }

  public void setStateHolding() {
    this.mIsStart = false;
    this.mIsCDT = false;
    this.mIsWaiting = false;
    this.mIsHolding = true;
    this.mIsReady = false;
    this.mIsRunning = false;
    this.mIsStopping = false;
    this.mIsFinished = false;
  }

  public void setStateReady() {
    this.mIsStart = false;
    this.mIsCDT = false;
    this.mIsWaiting = false;
    this.mIsHolding = false;
    this.mIsReady = true;
    this.mIsRunning = false;
    this.mIsStopping = false;
    this.mIsFinished = false;
  }

  public void setStateRunning() {
    this.mIsStart = false;
    this.mIsCDT = false;
    this.mIsWaiting = false;
    this.mIsHolding = false;
    this.mIsReady = false;
    this.mIsRunning = true;
    this.mIsStopping = false;
    this.mIsFinished = false;
  }

  public void setStateStopping() {
    this.mIsStart = false;
    this.mIsCDT = false;
    this.mIsWaiting = false;
    this.mIsHolding = false;
    this.mIsReady = false;
    this.mIsRunning = false;
    this.mIsStopping = true;
    this.mIsFinished = false;
  }

  public void setStateFinished() {
    this.mIsStart = false;
    this.mIsCDT = false;
    this.mIsWaiting = false;
    this.mIsHolding = false;
    this.mIsReady = false;
    this.mIsRunning = false;
    this.mIsStopping = false;
    this.mIsFinished = true;
  }

  @Override
  public int[] onCreateDrawableState(int extraSpace) {
    final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);
    if (mIsStart) {
      mergeDrawableStates(drawableState, STATE_START);
    }
    if (mIsCDT) {
      mergeDrawableStates(drawableState, STATE_CDT);
    }
    if (mIsWaiting) {
      mergeDrawableStates(drawableState, STATE_WAITING);
    }
    if (mIsHolding) {
      mergeDrawableStates(drawableState, STATE_HOLDING);
    }
    if (mIsReady) {
      mergeDrawableStates(drawableState, STATE_READY);
    }
    if (mIsRunning) {
      mergeDrawableStates(drawableState, STATE_RUNNING);
    }
    if (mIsStopping) {
      mergeDrawableStates(drawableState, STATE_STOPPING);
    }
    if (mIsFinished) {
      mergeDrawableStates(drawableState, STATE_FINISHED);
    }
    return drawableState;
  }
}
