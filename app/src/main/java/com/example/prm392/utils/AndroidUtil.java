package com.example.prm392.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.prm392.models.UserEntity;

/**
 * Utility class for Android UI helpers and UserEntity intent transfer.
 * Works fully offline with Room (no Firestore dependency).
 */
public class AndroidUtil {

    // ===== Toast Utility =====
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // ===== UserEntity <-> Intent =====

    /**
     * Puts UserEntity data into an Intent for transfer between Activities.
     */
    public static void passUserAsIntent(Intent intent, UserEntity user) {
        if (intent == null || user == null) return;

        intent.putExtra("userId", user.userId);
        intent.putExtra("fullName", user.fullName);
        intent.putExtra("email", user.email);
        intent.putExtra("password", user.password);  // (optional, if needed)
        intent.putExtra("avatarUrl", user.avatarUrl);
        intent.putExtra("lastLogin", user.lastLogin);
    }

    /**
     * Reconstructs a UserEntity from an Intent.
     */
    public static UserEntity getUserFromIntent(Intent intent) {
        if (intent == null) return null;

        UserEntity user = new UserEntity();
        user.userId = intent.getStringExtra("userId");
        user.fullName = intent.getStringExtra("fullName");
        user.email = intent.getStringExtra("email");
        user.password = intent.getStringExtra("password");
        user.avatarUrl = intent.getStringExtra("avatarUrl");
        user.lastLogin = intent.getLongExtra("lastLogin", 0L);

        return user;
    }

    // ===== Avatar Handling (Glide) =====
    public static void setProfilePic(Context context, Uri imageUri, ImageView imageView) {
        Glide.with(context)
                .load(imageUri)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView);
    }

    // ===== Helper: Get full name =====
    public static String fullName(UserEntity user) {
        if (user == null) return "Unknown User";
        if (user.fullName != null && !user.fullName.trim().isEmpty()) {
            return user.fullName.trim();
        }
        if (user.email != null && !user.email.trim().isEmpty()) {
            return user.email.trim();
        }
        return "User";
    }
}
