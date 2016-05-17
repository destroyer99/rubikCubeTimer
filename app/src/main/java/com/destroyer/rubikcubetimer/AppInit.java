package com.destroyer.rubikcubetimer;

import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

public class AppInit extends android.app.Application {

  @Override
  public void onCreate() {
    super.onCreate();

    final SharedPreferences sharedPrefs = getSharedPreferences("appPreferences", MODE_PRIVATE);

    if (sharedPrefs.getBoolean("firstRun", true)) {
      Parse.initialize(this, "UMpeJeqOQFoiNMxTX0SozYgs2hobmX2YY4Mh7tuv", "7bWn5dI3JiWnKvrdE4Xd5sCOlLYY5UO9eO1WZA7x");
      ParseQuery<ParseObject> query = ParseQuery.getQuery("paidVersion");
      query.findInBackground(new FindCallback<ParseObject>() {
        @Override
        public void done(List<ParseObject> list, ParseException e) {
          if (e == null) { // deviceId is not in the PAID db
            for (ParseObject obj : list) {
              if (obj.getString("deviceId").equals(((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId())) {
                sharedPrefs.edit().putBoolean("paidVersion", true).apply();
                return;
              }
            }

            ParseQuery<ParseObject> query = ParseQuery.getQuery("trialVersion");

            query.findInBackground(new FindCallback<ParseObject>() {
              @Override
              public void done(List<ParseObject> list, ParseException e) {
                if (e == null) {
                  if (list.isEmpty()) { // device ID not in DB (DB empty)
                    // add device to DB
                    ParseObject trialVersion = new ParseObject("trialVersion");
                    trialVersion.put("deviceId", ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId());
                    trialVersion.put("millis", System.currentTimeMillis());
                    trialVersion.saveInBackground();
                    Log.d("PARSE", "empty DB, added device to cloud DB");
                  } else { // found device ID
                    // find matching device ID
                    for (ParseObject obj : list) {
                      if (obj.getString("deviceId").equals(((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId())) {
                        sharedPrefs.edit().putLong("trialVersionX", obj.getLong("millis")).apply();
                        Log.d("PARSE", "found device {" + obj.getString("deviceId") + "} in cloud DB");
                        return;
                      }
                    }

                    // device ID not in DB
                    // add device to DB
                    long millis = System.currentTimeMillis();
                    ParseObject trialVersion = new ParseObject("trialVersion");
                    trialVersion.put("deviceId", ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId());
                    trialVersion.put("millis", millis);
                    trialVersion.saveInBackground();
                    sharedPrefs.edit().putLong("trialVersionX", millis).apply();
                    Log.d("PARSE", "added device to cloud DB");
                  }
                } else {
                  Log.d("PARSE_EXCEPTION", e.getMessage());
                }
              }
            });
          } else {
            Log.d("PARSE_EXCEPTION", e.getMessage());
          }
        }
      });
    }
  }
}
