package com.gh.sammie.quicktaxiriderapp.services;

import androidx.annotation.NonNull;

import com.gh.sammie.quicktaxiriderapp.Common;
import com.gh.sammie.quicktaxiriderapp.utils.UserUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            UserUtils.updateToken(this,s);

    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String,String>  dataRecieve = remoteMessage.getData();
        if (dataRecieve != null)
        {
            Common.showNotification(this,new Random().nextInt(),
                    dataRecieve.get(Common.NOTI_TITLE),
                    dataRecieve.get(Common.NOTI_CONTENT),
                    null);
        }

    }
}
