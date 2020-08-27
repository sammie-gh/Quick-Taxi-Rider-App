package com.gh.sammie.quicktaxiriderapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.gh.sammie.quicktaxiriderapp.model.DriverGeoModel;
import com.gh.sammie.quicktaxiriderapp.model.RiderModel;
import com.google.android.gms.maps.model.Marker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Common {
    public static final String DRIVER_INFO_REF = "DriverInfo";
    public static final String DRIVER_LOCATION_REFERENCES = "DriversLocation";
    public static final String TOKEN_REFERENCE = "Token";
    public static final String NOTI_TITLE = "title";
    public static final String NOTI_CONTENT = "body";
    public static final String RIDER_INFO_REF = "Riders";
    public static RiderModel currentRider;
    public static Set<DriverGeoModel> driversFound  = new HashSet<DriverGeoModel>();
    public static HashMap <String, Marker>markerList = new HashMap<>();


    //logo
    public static String buildWelcomeMessage() {
        if (Common.currentRider != null) {
            return new StringBuilder("Welcome ")
                    .append(Common.currentRider.getFirstName())
                    .append(" ")
                    .append(Common.currentRider.getLastName()).toString();
        } else
            return "";


    }

   // Notifications
    public static void showNotification(Context context, int id, String title, String body, Intent i) {
        PendingIntent pendingIntent = null;
        if (i != null)
            pendingIntent = PendingIntent.getActivity(context, id, i, PendingIntent.FLAG_UPDATE_CURRENT);
        String NOTIFICATION_CHANNEL_ID = "sammie_gh_quick_taxi";

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Quick Taxi", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("Uber remake");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);

            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.ic_baseline_directions_car_24)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_directions_car_24));

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);

        }
        Notification notification = builder.build();
        notificationManager.notify(id, notification);


    }

    public static String buildName(String firstName, String lastName) {
        return new StringBuilder(firstName).append(" ").append(lastName).toString();

    }
}
