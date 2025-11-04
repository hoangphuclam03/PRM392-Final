package com.example.prm392;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin, btnRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            finish();
        });

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ email và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                                // 🔥 Ghi thời gian đăng nhập vào Firestore (tự tạo nếu chưa có)
                                Map<String, Object> update = new HashMap<>();
                                update.put("lastLogin", System.currentTimeMillis());

                                DocumentReference ref = db.collection("Users").document(user.getUid());
                                ref.set(update, SetOptions.merge())
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Không thể cập nhật Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                        );

                                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this, "Vui lòng xác minh email trước khi đăng nhập", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.isEmailVerified()) {
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
        }
    }
}
