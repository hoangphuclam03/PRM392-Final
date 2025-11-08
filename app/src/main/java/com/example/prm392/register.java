package com.example.prm392;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.prm392.activities.MainActivity;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.UserDAO;
import com.example.prm392.models.UserEntity;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class register extends AppCompatActivity {

    private EditText edtFirstName, edtLastName, edtEmailRegister, edtPasswordRegister, edtConfirmPassword;
    private Button btnCreateAccount;
    private CheckBox cbAgree;
    private TextView tvGoToLogin;

    private UserDAO userDAO;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // ---------------- UI References ----------------
        edtFirstName = findViewById(R.id.edtFirstName);
        edtLastName = findViewById(R.id.edtLastName);
        edtEmailRegister = findViewById(R.id.edtEmailRegister);
        edtPasswordRegister = findViewById(R.id.edtPasswordRegister);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        cbAgree = findViewById(R.id.cbAgree);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // ---------------- Room Setup ----------------
        userDAO = AppDatabase.getInstance(this).userDAO();

        // ---------------- Register Button ----------------
        btnCreateAccount.setOnClickListener(v -> {
            String firstName = edtFirstName.getText().toString().trim();
            String lastName = edtLastName.getText().toString().trim();
            String email = edtEmailRegister.getText().toString().trim();
            String password = edtPasswordRegister.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();

            // ---------------- Validation ----------------
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!cbAgree.isChecked()) {
                Toast.makeText(this, "Vui lòng đồng ý với điều khoản!", Toast.LENGTH_SHORT).show();
                return;
            }

            // ---------------- Save User via Room ----------------
            executor.execute(() -> {
                UserEntity existing = userDAO.findByEmail(email);
                if (existing != null) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Email đã tồn tại!", Toast.LENGTH_SHORT).show());
                    return;
                }

                UserEntity user = new UserEntity();
                user.userId = UUID.randomUUID().toString();
                user.fullName = firstName + " " + lastName;
                user.email = email;
                user.password = password;
                user.avatarUrl = null;
                user.lastLogin = System.currentTimeMillis();

                userDAO.insert(user);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
            });
        });

        // ---------------- Go to Login ----------------
        tvGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
