package com.destroyer.rubikcubetimer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

public class AppInit extends android.app.Application {

    private Activity mainActivity;

    @Override
    public void onCreate() {
        super.onCreate();

        final SharedPreferences sharedPrefs = getSharedPreferences("appPreferences", MODE_PRIVATE);
        Log.wtf("paidVersionReset_pref", String.valueOf(sharedPrefs.getBoolean("paidVersionReset", false)));
        if (sharedPrefs.getBoolean("paidVersionReset", false)) {
            sharedPrefs.edit().remove("paidVersion").commit();
        }

        if (!sharedPrefs.getBoolean("paidVersion", false)) {
            Parse.initialize(this, "UMpeJeqOQFoiNMxTX0SozYgs2hobmX2YY4Mh7tuv", "7bWn5dI3JiWnKvrdE4Xd5sCOlLYY5UO9eO1WZA7x");
            ParseQuery<ParseObject> query = ParseQuery.getQuery("paidVersion");
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> list, ParseException e) {
                    if (e == null) {
                        List<Integer> foundID = new ArrayList<>();
                        for (ParseObject obj : list) {
                            if (obj.getString("deviceId").equals(((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId())) {
                                foundID.add(list.indexOf(obj));
                            }
                        }
                        Log.d("PAID_VERSION", String.valueOf(!foundID.isEmpty()));

                        if (foundID.isEmpty()) {
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
                                            List<Integer> foundID = new ArrayList<>();
                                            for (ParseObject obj : list) {
                                                if (obj.getString("deviceId").equals(((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId())) {
                                                    foundID.add(list.indexOf(obj));
                                                }
                                            }

                                            if (foundID.isEmpty()) { // device ID not in DB
                                                // add device to DB
                                                ParseObject trialVersion = new ParseObject("trialVersion");
                                                trialVersion.put("deviceId", ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId());
                                                trialVersion.put("millis", System.currentTimeMillis());
                                                trialVersion.saveInBackground();
                                                Log.d("PARSE", "added device to cloud DB");
                                                return;
                                            }
                                            if (foundID.size() > 1) { // found multiple matching IDs (should not happen)
                                                Log.d("PARSE", "found " + foundID + " devices with mathcing ID");
                                                return;
                                            }
                                            Log.d("PARSE", "found device id: " + list.get(foundID.get(0)).getString("deviceId"));
                                            ((ActivityCallback) mainActivity).displayTrialTimeRemaining(list.get(foundID.get(0)).getLong("millis"));
                                        }
                                    } else {
                                        Log.d("PARSE_EXCEPTION", e.getMessage());
                                    }
                                }
                            });
                        } else sharedPrefs.edit().putBoolean("paidVersion", !foundID.isEmpty()).apply();
                    } else {
                        Log.d("PARSE_EXCEPTION", e.getMessage());
                    }
                }
            });
        }
    }

    public void setActivity(Activity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public interface ActivityCallback {
        void displayTrialTimeRemaining(long appCreatedDate);
    }
}
