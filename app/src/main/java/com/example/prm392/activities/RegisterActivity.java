package com.example.prm392.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.prm392.R;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.UserDAO;
import com.example.prm392.models.UserEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtFullName, edtEmailRegister, edtPasswordRegister, edtConfirmPassword;
    private Button btnCreateAccount;
    private CheckBox cbAgree;
    private TextView tvGoToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private UserDAO userDAO;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_DARK_THEME = "dark_theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ---------------- Áp dụng theme lưu trước ----------------
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean darkTheme = prefs.getBoolean(KEY_DARK_THEME, false);
        AppCompatDelegate.setDefaultNightMode(
                darkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // ---------------- Ánh xạ UI ----------------
        edtFullName = findViewById(R.id.edtFullName);
        edtEmailRegister = findViewById(R.id.edtEmailRegister);
        edtPasswordRegister = findViewById(R.id.edtPasswordRegister);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        cbAgree = findViewById(R.id.cbAgree);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // ---------------- Khởi tạo Firebase + Room ----------------
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userDAO = AppDatabase.getInstance(this).userDAO();

        // ---------------- Sự kiện ----------------
        btnCreateAccount.setOnClickListener(v -> createAccount());
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void createAccount() {
        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmailRegister.getText().toString().trim();
        String password = edtPasswordRegister.getText().toString().trim();
        String confirm = edtConfirmPassword.getText().toString().trim();

        // ---------------- Kiểm tra ----------------
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!cbAgree.isChecked()) {
            Toast.makeText(this, "Vui lòng đồng ý với điều khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        // ---------------- Tạo tài khoản Firebase ----------------
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification();

                            // ---------------- Dữ liệu để lưu Firestore ----------------
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("userId", user.getUid());
                            userData.put("fullName", fullName);
                            userData.put("email", email);
                            userData.put("password", password);
                            userData.put("avatarUrl", null);
                            userData.put("lastLogin", 0L);
                            userData.put("createdAt", System.currentTimeMillis());

                            db.collection("Users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Đăng ký thành công! Vui lòng xác minh email.", Toast.LENGTH_LONG).show();
                                        saveUserToLocal(user.getUid(), fullName, email, password);
                                        startActivity(new Intent(this, MainActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Lưu Firestore thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                        }
                    } else {
                        Toast.makeText(this, "Lỗi đăng ký: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------------- Lưu user vào Room DB ----------------
    private void saveUserToLocal(String userId, String fullName, String email, String password) {
        executor.execute(() -> {
            UserEntity existing = userDAO.findByEmail(email);
            if (existing != null) return;

            UserEntity user = new UserEntity();
            user.userId = userId != null ? userId : UUID.randomUUID().toString();
            user.fullName = fullName;
            user.email = email;
            user.password = password;
            user.avatarUrl = null;
            user.lastLogin = System.currentTimeMillis();

            userDAO.insert(user);
        });
    }
}
