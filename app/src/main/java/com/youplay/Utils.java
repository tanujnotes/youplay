package com.youplay;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static android.content.Context.POWER_SERVICE;

/**
 * Created by tan on 23/01/17.
 **/

public class Utils {
    protected static final String TAG = "Utils";

    public static void showToast(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static void showToastLong(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static void showNoInternetMessage(Activity activity, View.OnClickListener clickListener) {
        Snackbar.make(activity.findViewById(android.R.id.content), "No Internet Connection!", Snackbar.LENGTH_INDEFINITE)
                .setAction("REFRESH", clickListener)
                .show();
    }

    public static void changeStatusBarColor(Activity activity, int color) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(activity, color));
        }
    }

    public static void copyToClipboard(Context context, String message) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("YouPlay", message);
            clipboard.setPrimaryClip(clip);
            showToast(context, "Text copied to clipboard");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getChatDate(long milliSeconds) {
        SimpleDateFormat formatter = new SimpleDateFormat(AppConstants.YOUTUBE_API_DATE_FORMAT, java.util.Locale.getDefault());
        String dateString = formatter.format(milliSeconds);

        String result = "";
        Date date;
        try {
            formatter.setTimeZone(TimeZone.getDefault());
            date = formatter.parse(dateString);
            if (DateUtils.isToday(date.getTime())) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                result = "Today, " + simpleDateFormat.format(date);
            } else if (DateUtils.isToday(date.getTime() + (24 * 60 * 60 * 1000))) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                result = "Yesterday, " + simpleDateFormat.format(date);
            } else {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(AppConstants.CHAT_DATE_FORMAT, java.util.Locale.getDefault());
                result = simpleDateFormat.format(date);
            }
        } catch (ParseException pe) {
            pe.printStackTrace();
        }
        return result;
    }

    public static String getRelativeDay(String dateString) {
        long ONE_DAY = 24 * 60 * 60 * 1000;
        SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.YOUTUBE_API_DATE_FORMAT, java.util.Locale.getDefault());
        String result = "";
        Date date;
        try {
            dateFormat.setTimeZone(TimeZone.getDefault());
            date = dateFormat.parse(dateString);
            if (DateUtils.isToday(date.getTime())) {
                result = "Today";
            } else if (DateUtils.isToday(date.getTime() + ONE_DAY)) {
                result = "Yesterday";
            } else if (date.getTime() > System.currentTimeMillis() - 7 * ONE_DAY) {
                result = "This week";
            } else if (date.getTime() > System.currentTimeMillis() - 14 * ONE_DAY) {
                result = "1 week ago";
            } else if (date.getTime() > System.currentTimeMillis() - 21 * ONE_DAY) {
                result = "2 weeks ago";
            } else if (date.getTime() > System.currentTimeMillis() - 28 * ONE_DAY) {
                result = "3 weeks ago";
            } else {
                SimpleDateFormat formatter = new SimpleDateFormat(AppConstants.APP_DATE_FORMAT, java.util.Locale.getDefault());
                result = formatter.format(date);
            }
        } catch (ParseException pe) {
            pe.printStackTrace();
        }
        return result;
    }

    public static boolean isInternetAvailable(Context context, boolean showToast) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean internetComing = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (showToast && !internetComing) showToast(context, "Uh-oh! No Internet");

        return internetComing;
    }

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static int pxToDp(int px, Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static boolean isScreenOn(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        boolean isScreenOn;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            isScreenOn = powerManager.isInteractive();
        } else {
            isScreenOn = powerManager.isScreenOn();
        }

        return isScreenOn;
    }

    public static void sendShareAppIntent(Context context, FirebaseAnalytics firebaseAnalytics) {
        String message = "YouPlay app - play YouTube videos in background. No ads! Download apk here: "
                + AppConstants.YOUPLAY_DOWNLOAD_SHORT_LINK
                + " (Size: 2MB)";
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.setType("text/plain");
        context.startActivity(Intent.createChooser(sendIntent, "Share app via"));
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("YouPlay video", message);
            clipboard.setPrimaryClip(clip);
            showToast(context, "YouPlay app link copied");
        } catch (Exception e) {
            e.printStackTrace();
        }

        firebaseAnalytics.logEvent(AppConstants.Event.APP_SHARE, new Bundle());
    }

    private static String getYoutubeLink(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    public static void sendShareVideoIntent(Context context, String videoId, String videoTitle) {
        if (videoId == null || videoId.isEmpty()) {
            showToast(context, "Please try again!");
            return;
        }
        String youtubeLink = getYoutubeLink(videoId);
        String message;

        if (videoTitle.isEmpty())
            message = "Watch this video on YouTube - " + youtubeLink;
        else if (videoTitle.length() <= 80)
            message = videoTitle + " - " + youtubeLink;
        else
            message = videoTitle.substring(0, 70) + "... " + youtubeLink;

        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("YouPlay video", message);
            clipboard.setPrimaryClip(clip);
            showToast(context, "Video link copied");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent sentIntent = new Intent(Intent.ACTION_SEND);
        sentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sentIntent.putExtra(Intent.EXTRA_TEXT, message);
        sentIntent.setType("text/plain");
        Intent chooserIntent = Intent.createChooser(sentIntent, "Share video via");
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooserIntent);
    }

    public static int getStatusBarHeight(android.content.res.Resources res) {
        return (int) (24 * res.getDisplayMetrics().density);
    }

    public static String getUserCountry(Context context) {
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final String simCountry = tm.getSimCountryIso();
            if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
                return simCountry.toUpperCase();
            } else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
                String networkCountry = tm.getNetworkCountryIso();
                if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
                    return networkCountry.toUpperCase();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getChannelUploadsId(String channelId) {
        return "UU".concat(channelId.substring(2));
    }

    public static void showKeyboard(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 1);
    }

    public static void hideKeyboard(Activity activity) {
        View view = activity.findViewById(android.R.id.content);
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (view != null && imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static List<String> getSavedSearchList(String savedSearchQueries) {
        List<String> list = new ArrayList<>();
        if (savedSearchQueries.isEmpty()) {
            return list;
        } else {
            String[] items = savedSearchQueries.split(",");
            list.addAll(Arrays.asList(items));
            Collections.reverse(list);
            return list;
        }
    }

    public static void saveSearchQuery(String query, PreferenceManager pm) {
        String savedSearchQueries = pm.getSavedSearchQueries();
        List<String> list = new ArrayList<>();

        if (savedSearchQueries.isEmpty()) {
            list.add(query);
        } else {
            String[] items = savedSearchQueries.split(",");
            list.addAll(Arrays.asList(items));
            if (list.contains(query)) {
                list.remove(query);
                list.add(query);
            } else if (list.size() < 5) {
                list.add(query);
            } else {
                list.remove(0);
                list.add(query);
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String s : list) {
            stringBuilder.append(s);
            stringBuilder.append(",");
        }
        pm.setSavedSearchQueries(stringBuilder.toString());
    }

    public static String formatVideoDuration(String duration) {
        if (duration.equals("PT0S")) return "LIVE";

        String videoDuration = "";
        String h, m, s;
        duration = duration.substring(2);
        if (duration.contains("H")) {
            h = duration.split("H")[0];
            duration = duration.substring(h.length() + 1);
            if (h.length() == 1) h = "0" + h;
            videoDuration = h + ":";
        }
        if (duration.contains("M")) {
            m = duration.split("M")[0];
            duration = duration.substring(m.length() + 1);
            if (m.length() == 1) m = "0" + m;
            videoDuration = videoDuration + m + ":";
        } else {
            videoDuration = videoDuration + "00:";
        }
        if (duration.contains("S")) {
            s = duration.split("S")[0];
            if (s.length() == 1) s = "0" + s;
            videoDuration = videoDuration + s;
        } else {
            videoDuration = videoDuration + "00";
        }

        return videoDuration;
    }

    public static String formatViewCount(String viewCount) {
        int length = viewCount.length();

        if (length >= 10) {
            if (viewCount.substring(1, length - 8).equals("0"))
                return viewCount.substring(0, length - 9) + "B views";
            else
                return viewCount.substring(0, length - 9) + "." + viewCount.substring(1, length - 8) + "B views";
        }

        if (length >= 7) {
            if (length == 7 && !viewCount.substring(1, length - 5).equals("0"))
                return viewCount.substring(0, length - 6) + "." + viewCount.substring(1, length - 5) + "M views";
            else
                return viewCount.substring(0, length - 6) + "M views";
        }

        if (length >= 4) {
            if (length == 4 && !viewCount.substring(1, length - 2).equals("0"))
                return viewCount.substring(0, length - 3) + "." + viewCount.substring(1, length - 2) + "K views";
            else
                return viewCount.substring(0, length - 3) + "K views";
        }
        return viewCount + " views";
    }
}
