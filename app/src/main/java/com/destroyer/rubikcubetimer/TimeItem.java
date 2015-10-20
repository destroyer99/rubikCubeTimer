package com.destroyer.rubikcubetimer;

public class TimeItem {

  private int id;
  private long date, time;

  public TimeItem() {
  }

  public TimeItem(int id, long date, long time) {
    this.id = id;
    this.date = date;
    this.time = time;
  }

  public int getId() {
    return this.id;
  }

  public long getDate() {
    return this.date;
  }

  public long getTime() {
    return this.time;
  }
}
