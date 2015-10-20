package com.destroyer.rubikcubetimer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DateFormat;

public class TimeListAdapter extends ArrayAdapter<TimeItem> {

  Context context;
  int layoutResourceId;
  TimeItem[] timeItems;

  public TimeListAdapter(Context context, int layoutResourceId, TimeItem[] timeItems) {
    super(context, layoutResourceId, timeItems);
    this.context = context;
    this.layoutResourceId = layoutResourceId;
    this.timeItems = timeItems;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    if (convertView == null) {
      LayoutInflater inflater = ((Activity) context).getLayoutInflater();
      convertView = inflater.inflate(layoutResourceId, parent, false);
    }

    convertView.setTag(timeItems[position].getId());
    ((TextView) convertView.findViewById(R.id.dateTxt)).setText(formatDate(timeItems[position].getDate()));
    ((TextView) convertView.findViewById(R.id.timeTxt)).setText(formatTime(timeItems[position].getTime()));

    return convertView;
  }

  private String formatDate(long date) {
    return DateFormat.getDateTimeInstance().format(date);
  }

  private String formatTime(long millis) {
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
}