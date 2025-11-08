package com.example.prm392.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
    private TextView tvGoToLogin, tvPasswordStrength; // ✅ thêm dòng này

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private UserDAO userDAO;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_DARK_THEME = "dark_theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);

        // ---------------- Firebase + Room ----------------
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userDAO = AppDatabase.getInstance(this).userDAO();

        // ---------------- Theo dõi độ mạnh mật khẩu ----------------
        edtPasswordRegister.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // ---------------- Sự kiện ----------------
        btnCreateAccount.setOnClickListener(v -> createAccount());
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    // ✅ Hàm kiểm tra độ mạnh mật khẩu
    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            tvPasswordStrength.setText("");
            return;
        }

        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[!@#$%^&*()_+=\\-{};:'\",.<>?].*")) score++;

        if (score <= 2) {
            tvPasswordStrength.setText("Độ mạnh mật khẩu: Yếu");
            tvPasswordStrength.setTextColor(getColor(android.R.color.holo_red_dark));
        } else if (score == 3 || score == 4) {
            tvPasswordStrength.setText("Độ mạnh mật khẩu: Trung bình");
            tvPasswordStrength.setTextColor(getColor(android.R.color.holo_orange_dark));
        } else {
            tvPasswordStrength.setText("Độ mạnh mật khẩu: Mạnh");
            tvPasswordStrength.setTextColor(getColor(android.R.color.holo_green_dark));
        }
    }

    // ---------------- Kiểm tra & Tạo tài khoản ----------------
    private void createAccount() {
        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmailRegister.getText().toString().trim();
        String password = edtPasswordRegister.getText().toString().trim();
        String confirm = edtConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Thêm kiểm tra độ mạnh thực tế
        if (password.length() < 8 ||
                !password.matches(".*[A-Z].*") ||
                !password.matches(".*[a-z].*") ||
                !password.matches(".*[0-9].*") ||
                !password.matches(".*[!@#$%^&*()_+=\\-{};:'\",.<>?].*")) {
            Toast.makeText(this, "Mật khẩu phải gồm ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt", Toast.LENGTH_LONG).show();
            return;
        }

        if (!cbAgree.isChecked()) {
            Toast.makeText(this, "Vui lòng đồng ý với điều khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        // ---------------- Firebase Auth ----------------
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification();

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

    // ---------------- Lưu user vào Room ----------------
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
