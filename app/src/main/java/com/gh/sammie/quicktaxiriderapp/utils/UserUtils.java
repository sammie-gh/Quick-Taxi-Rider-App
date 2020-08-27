package com.gh.sammie.quicktaxiriderapp.utils;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.gh.sammie.quicktaxiriderapp.Common;
import com.gh.sammie.quicktaxiriderapp.model.TokenModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

public class UserUtils {
    public static void updateUser(View view, Map<String, Object> updateData) {
        FirebaseDatabase.getInstance().getReference(Common.RIDER_INFO_REF)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                 .updateChildren(updateData)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_SHORT).show();

                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Snackbar.make(view, "update information successfully", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    public static void updateToken(Context context, String token) {
        TokenModel tokenModel = new TokenModel(token);
        FirebaseDatabase.getInstance()
                .getReference(Common.TOKEN_REFERENCE).child(FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid()).setValue(tokenModel)
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        });

    }
}
