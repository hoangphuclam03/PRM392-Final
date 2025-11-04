package com.example.prm392;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtFirstName, edtLastName, edtEmailRegister, edtPasswordRegister, edtConfirmPassword;
    private Button btnCreateAccount;
    private CheckBox cbAgree;
    private TextView tvGoToLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtFirstName = findViewById(R.id.edtFirstName);
        edtLastName = findViewById(R.id.edtLastName);
        edtEmailRegister = findViewById(R.id.edtEmailRegister);
        edtPasswordRegister = findViewById(R.id.edtPasswordRegister);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        cbAgree = findViewById(R.id.cbAgree);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnCreateAccount.setOnClickListener(v -> createAccount());
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void createAccount() {
        String firstName = edtFirstName.getText().toString().trim();
        String lastName = edtLastName.getText().toString().trim();
        String email = edtEmailRegister.getText().toString().trim();
        String password = edtPasswordRegister.getText().toString().trim();
        String confirm = edtConfirmPassword.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirm.isEmpty()) {
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

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification();

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("uid", user.getUid());
                            userData.put("firstName", firstName);
                            userData.put("lastName", lastName);
                            userData.put("email", email);
                            userData.put("createdAt", System.currentTimeMillis());

                            db.collection("Users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid ->
                                            Toast.makeText(this, "Đăng ký thành công! Vui lòng xác minh email.", Toast.LENGTH_LONG).show()
                                    )
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Lưu Firestore thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );

                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Lỗi đăng ký: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
