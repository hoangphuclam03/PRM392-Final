package com.example.prm392.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import models.Users;

public class AndroidUtil {

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // ===== Users <-> Intent =====

    /** Đưa Users vào Intent.
     *  Giữ khoá "userId" = String(id) để tương thích các màn khác.
     */
    public static void passUsersAsIntent(Intent intent, Users u) {
        if (u == null) return;
        intent.putExtra("userId", String.valueOf(u.getUid()));    // quan trọng: id dạng chuỗi
        intent.putExtra("firstName", u.getFirstName());
        intent.putExtra("lastName",  u.getLastName());
        intent.putExtra("username",  u.getUsername());           // đã auto ghép nếu null
        intent.putExtra("email",     u.getEmail());
        intent.putExtra("fcmToken",  u.getFcmToken());
        // createdTimestamp nếu cần có thể convert sang millis để truyền
        // if (u.getCreatedTimestamp() != null) {
        //     intent.putExtra("createdTs", u.getCreatedTimestamp().toDate().getTime());
        // }
    }

    /** Lấy Users từ Intent.
     *  Nếu không có username sẽ tự ghép từ firstName + lastName (theo logic trong Users).
     */
    public static Users getUsersFromIntent(Intent intent) {
        if (intent == null) return null;
        Users u = new Users();
        // id lấy từ "userId" (chuỗi số)
        try {
            String idStr = intent.getStringExtra("userId");
            if (idStr != null) {
                String uid = idStr.trim();
                if (!uid.isEmpty()) u.setUid(uid);
            }
        } catch (Exception ignore) {}

        u.setFirstName(intent.getStringExtra("firstName"));
        u.setLastName(intent.getStringExtra("lastName"));
        u.setUsername(intent.getStringExtra("username")); // nếu null -> Users.getUsername() sẽ tự ghép
        u.setEmail(intent.getStringExtra("email"));
        u.setFcmToken(intent.getStringExtra("fcmToken"));

        // Nếu bạn có truyền createdTs (millis):
        // if (intent.hasExtra("createdTs")) {
        //     long ms = intent.getLongExtra("createdTs", 0L);
        //     if (ms > 0) u.setCreatedTimestamp(new com.google.firebase.Timestamp(new java.util.Date(ms)));
        // }

        return u;
    }

    // ===== Ảnh đại diện =====
    public static void setProfilePic(Context context, Uri imageUri, ImageView imageView) {
        Glide.with(context)
                .load(imageUri)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView);
    }

    // ===== Tiện ích tên đầy đủ (nếu cần dùng lại) =====
    public static String fullName(Users u) {
        if (u == null) return "User";
        String ln = u.getLastName()  != null ? u.getLastName().trim()  : "";
        String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String name = (ln + " " + fn).trim();
        return name.isEmpty() ? "User" : name;
    }
}
